package com.mika.dspot.expect.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.cValue
import platform.CoreGraphics.CGRectZero
import platform.CoreGraphics.CGRect
import platform.Foundation.NSURL
import platform.Foundation.NSURLRequest
import platform.WebKit.WKHTTPCookieStore
import platform.WebKit.WKWebView
import platform.WebKit.WKWebViewConfiguration
import platform.WebKit.WKNavigationDelegateProtocol
import platform.WebKit.WKNavigation
import platform.darwin.NSObject

// ---------------------------------------------------------------------------
// WKHTTPCookieStore-based cookie manager
// ---------------------------------------------------------------------------

actual fun createWebViewCookieManager(): WebViewCookieManager =
    object : WebViewCookieManager {
        @OptIn(ExperimentalForeignApi::class)
        override fun getCookie(url: String): String {
            // WKHTTPCookieStore is async-only on iOS; return empty string synchronously.
            // Full async cookie retrieval would require a callback-based API exposed
            // to the caller — not needed for current D-Spot usage.
            return ""
        }

        @OptIn(ExperimentalForeignApi::class)
        override fun removeAllCookies() {
            val store: WKHTTPCookieStore =
                WKWebViewConfiguration().websiteDataStore.httpCookieStore
            store.getAllCookies { cookies ->
                @Suppress("UNCHECKED_CAST")
                (cookies as? List<Any>)?.forEach { cookie ->
                    store.deleteCookie(cookie as platform.Foundation.NSHTTPCookie) {}
                }
            }
        }
    }

// ---------------------------------------------------------------------------
// Navigation delegate helper
// ---------------------------------------------------------------------------

@OptIn(ExperimentalForeignApi::class)
private class WKNavigationDelegate(
    private val onFinished: (String) -> Unit,
) : NSObject(), WKNavigationDelegateProtocol {

    override fun webView(webView: WKWebView, didFinishNavigation: WKNavigation?) {
        val url = webView.URL?.absoluteString ?: return
        onFinished(url)
    }
}

// ---------------------------------------------------------------------------
// PlatformWebView — generic URL loader
// ---------------------------------------------------------------------------

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
@Composable
actual fun PlatformWebView(
    state: MutableState<WebViewState>,
    initUrl: String,
    aboveContent: @Composable (BoxScope.() -> Unit),
    onPageFinished: (String) -> Unit,
) {
    Box {
        UIKitView(
            factory = {
                val config = WKWebViewConfiguration()
                val webView = WKWebView(frame = cValue<CGRect> { }, configuration = config)
                webView.navigationDelegate = WKNavigationDelegate(onFinished = { url ->
                    state.value = WebViewState.Finished
                    onPageFinished(url)
                })
                NSURL.URLWithString(initUrl)?.let { nsUrl ->
                    webView.loadRequest(NSURLRequest.requestWithURL(nsUrl))
                }
                webView
            },
            modifier = Modifier.fillMaxSize(),
        )
        aboveContent()
    }
}

// ---------------------------------------------------------------------------
// DiscordWebView — loads discord.com/login and extracts the token via JS
// ---------------------------------------------------------------------------

private const val DISCORD_URL = "https://discord.com/login"

// JS snippet that triggers an alert with the Discord token (same as Android).
private const val DISCORD_JS_SNIPPET =
    "javascript:(function(){var i=document.createElement('iframe');document.body.appendChild(i);alert(i.contentWindow.localStorage.token.slice(1,-1))})()"

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
@Composable
actual fun DiscordWebView(
    state: MutableState<WebViewState>,
    aboveContent: @Composable (BoxScope.() -> Unit),
    onLoginDone: (token: String) -> Unit,
) {
    Box {
        UIKitView(
            factory = {
                val config = WKWebViewConfiguration()
                val webView = WKWebView(frame = cValue<CGRect> { }, configuration = config)

                // Navigation delegate: when we land on /app, inject JS to extract token.
                webView.navigationDelegate = object : NSObject(), WKNavigationDelegateProtocol {
                    override fun webView(webView: WKWebView, didFinishNavigation: WKNavigation?) {
                        val url = webView.URL?.absoluteString ?: return
                        if (url.endsWith("/app")) {
                            // Evaluate the token-extraction snippet; result arrives in the
                            // completion handler as an NSString alert substitute.
                            webView.evaluateJavaScript(
                                """
                                (function(){
                                    try {
                                        var t = window.localStorage.token;
                                        if (t) { return t.replace(/"/g,''); }
                                    } catch(e) {}
                                    return '';
                                })()
                                """.trimIndent()
                            ) { result, _ ->
                                val token = result as? String ?: ""
                                if (token.isNotBlank()) {
                                    onLoginDone(token)
                                }
                            }
                        }
                    }
                }

                NSURL.URLWithString(DISCORD_URL)?.let { nsUrl ->
                    webView.loadRequest(NSURLRequest.requestWithURL(nsUrl))
                }
                webView
            },
            modifier = Modifier.fillMaxSize(),
        )
        aboveContent()
    }
}

