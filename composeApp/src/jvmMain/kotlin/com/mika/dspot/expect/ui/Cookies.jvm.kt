package com.mika.dspot.expect.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mika.dspot.expect.openUrl
import com.mika.dspot.ui.theme.typo
import dspot.composeapp.generated.resources.Res
import dspot.composeapp.generated.resources.desktop_webview_description
import dspot.composeapp.generated.resources.open_blog_post
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource
import javax.swing.SwingUtilities

// --- Cookie manager -----------------------------------------------------------

/**
 * Returns the JCEF-backed cookie manager.
 *
 * IMPORTANT: On Desktop JVM, cookies set by JCEF (Chromium) are stored in
 * Chromium's own internal cookie database — completely separate from Java's
 * java.net.CookieHandler. Using JcefCookieManager here is the only way to
 * correctly read cookies after a JCEF web login.
 */
actual fun createWebViewCookieManager(): WebViewCookieManager = JcefCookieManager()

// --- CEF init state -----------------------------------------------------------

private sealed interface JcefState {
    object Loading : JcefState
    object Ready   : JcefState
    data class Error(val message: String) : JcefState
}

/**
 * Remembers the current CEF initialisation state and supports retry.
 */
@Composable
private fun rememberJcefState(): Pair<JcefState, () -> Unit> {
    var retryKey by remember { mutableIntStateOf(0) }
    var state: JcefState by remember { mutableStateOf(JcefState.Loading) }

    LaunchedEffect(retryKey) {
        state = JcefState.Loading
        state = withContext(Dispatchers.IO) {
            try {
                acquireCefApp()
                JcefState.Ready
            } catch (t: Throwable) {
                JcefState.Error(t.message ?: "Unknown error")
            }
        }
    }

    val retry: () -> Unit = { retryKey++ }
    return Pair(state, retry)
}

// --- PlatformWebView ----------------------------------------------------------

private const val YOUTUBE_MUSIC_HOST      = "music.youtube.com"
// Always pass this exact canonical URL to onPageFinished so that LogInScreen's
// strict equality check (url == Config.YOUTUBE_MUSIC_MAIN_URL) always matches.
private const val YOUTUBE_MUSIC_CANONICAL = "https://music.youtube.com/"

/**
 * Desktop actual: embeds a real Chromium browser (JCEF).
 *
 * Login detection:
 * - YouTube: URL is on the actual music.youtube.com domain (NOT accounts.google.com?continue=...)
 *   → flush cookie store → extract cookies from JCEF's CefCookieManager
 *   → call onPageFinished with the canonical URL so LogInScreen recognises it.
 * - Spotify: redirect away from accounts.spotify.com/authorize
 * - LastFM: callback scheme/token in URL
 */
@Composable
actual fun PlatformWebView(
    state: MutableState<WebViewState>,
    initUrl: String,
    aboveContent: @Composable (BoxScope.() -> Unit),
    onPageFinished: (String) -> Unit,
) {
    val (jcef, retry) = rememberJcefState()
    var isLoginCompleted by remember { mutableStateOf(false) }
    // Tracks whether we have flushed stale cookies after CEF became ready.
    // The browser is only shown once cookies have been cleared so that a
    // pre-existing Google session cannot silently auto-complete the login.
    var cookiesCleared by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // When CEF transitions to Ready, clear all cookies before showing the browser.
    LaunchedEffect(jcef) {
        if (jcef is JcefState.Ready) {
            withContext(Dispatchers.IO) {
                val mgr = org.cef.network.CefCookieManager.getGlobalManager()
                mgr?.deleteCookies("", "")
                mgr?.flushStore(null)
                kotlinx.coroutines.delay(300) // allow Chromium to process the deletion
            }
            cookiesCleared = true
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            jcef is JcefState.Loading || (jcef is JcefState.Ready && !cookiesCleared) -> {
                // Show spinner while CEF is initialising OR while we are clearing cookies
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center).size(48.dp),
                )
            }
            jcef is JcefState.Ready && cookiesCleared -> {
                val triggerExtraction: (org.cef.browser.CefBrowser) -> Unit = { browser ->
                    if (!isLoginCompleted) {
                        isLoginCompleted = true
                        state.value = WebViewState.Finished

                        coroutineScope.launch(Dispatchers.IO) {
                            var validCookies = ""
                            val mgr = org.cef.network.CefCookieManager.getGlobalManager()

                            // Retry loop to allow all authentication cookies and redirects to settle
                            for (attempt in 1..15) {
                                mgr?.flushStore(null)
                                kotlinx.coroutines.delay(250)
                                val cookies = JcefCookieManager().getCookie(YOUTUBE_MUSIC_CANONICAL)
                                if (cookies.contains("SAPISID") || cookies.contains("LOGIN_INFO") ||
                                    cookies.contains("__Secure") || cookies.contains("SSID")
                                ) {
                                    validCookies = cookies
                                    break
                                }
                            }

                            withContext(Dispatchers.Main) {
                                try {
                                    browser.stopLoad()
                                    SwingUtilities.invokeLater {
                                        try { browser.close(true) } catch (_: Throwable) {}
                                    }
                                } catch (_: Throwable) {}

                                if (validCookies.isNotEmpty()) {
                                    onPageFinished(YOUTUBE_MUSIC_CANONICAL)
                                } else {
                                    // If authentication cookies were not found, allow user to retry
                                    isLoginCompleted = false
                                }
                            }
                        }
                    }
                }

                // Pass visible=!isLoginCompleted to JcefBrowser so that when login
                // completes the AWT panel (and its Win32 HWND child) is set to
                // isVisible=false — hiding Chromium's painting from the screen —
                // while the CEF cookie/network engine continues running in the
                // background so session cookies can be extracted before closing.
                JcefBrowser(
                    url = initUrl,
                    visible = !isLoginCompleted,
                    onLoadingStateChange = { browser, isLoading, _, _ ->
                        if (isLoginCompleted) return@JcefBrowser
                        val currentUrl = browser.url ?: ""

                        // 1. YouTube / Google login detection
                        //
                        // We trigger as soon as the browser begins navigating to
                        // music.youtube.com (isLoading == true), not only after the
                        // page finishes loading.  isLoginCompleted is set to true
                        // immediately inside triggerExtraction, which also flips
                        // visible=false on JcefBrowser, so Chromium's rendering is
                        // hidden before the YouTube Music interface can appear.
                        val host = try { java.net.URI(currentUrl).host?.lowercase() ?: "" } catch (_: Exception) { "" }
                        val isActualYouTubeMusic = host == "music.youtube.com" || host.endsWith(".music.youtube.com")

                        if (isActualYouTubeMusic) {
                            triggerExtraction(browser)
                            return@JcefBrowser
                        }

                        // 2. Spotify login detection:
                        val isStatusUrl = currentUrl.contains("accounts.spotify.com") && currentUrl.contains("/status")
                        val isOpenSpotify = currentUrl.contains("open.spotify.com")
                        val hasAuthToken = currentUrl.contains("access_token=") || currentUrl.contains("code=")
                        if ((isStatusUrl || isOpenSpotify || hasAuthToken) && !isLoading) {
                            isLoginCompleted = true
                            browser.stopLoad()
                            SwingUtilities.invokeLater { browser.close(true) }

                            state.value = WebViewState.Finished
                            onPageFinished(currentUrl)
                            return@JcefBrowser
                        }

                        // 3. LastFM login detection
                        if ((currentUrl.contains("lastfm-auth") || currentUrl.contains("token=")) && !isLoading) {
                            isLoginCompleted = true
                            browser.stopLoad()
                            SwingUtilities.invokeLater { browser.close(true) }

                            state.value = WebViewState.Finished
                            onPageFinished(currentUrl)
                            return@JcefBrowser
                        }
                    },
                    onPageFinished = { url ->
                        if (!isLoginCompleted && url.contains(YOUTUBE_MUSIC_HOST, ignoreCase = true)) {
                            // Fallback handled by onLoadingStateChange; if reached here, trigger as well
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                )

                if (isLoginCompleted) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center).size(48.dp),
                    )
                }
            }
            jcef is JcefState.Error -> {
                BrowserErrorFallback(onRetry = retry)
            }
        }
        aboveContent()
    }
}

// --- DiscordWebView -----------------------------------------------------------

private const val DISCORD_LOGIN_URL = "https://discord.com/login"
private const val DISCORD_JS_SNIPPET =
    "javascript:(function(){try{var i=document.createElement('iframe');document.body.appendChild(i);alert(i.contentWindow.localStorage.token.slice(1,-1));}catch(e){}})()"

/**
 * Desktop actual: Chromium pointed at Discord login.
 */
@Composable
actual fun DiscordWebView(
    state: MutableState<WebViewState>,
    aboveContent: @Composable (BoxScope.() -> Unit),
    onLoginDone: (token: String) -> Unit,
) {
    val (jcef, retry) = rememberJcefState()
    var isLoginCompleted by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            isLoginCompleted -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center).size(48.dp),
                )
            }
            jcef is JcefState.Loading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center).size(48.dp),
                )
            }
            jcef is JcefState.Ready -> {
                JcefBrowser(
                    url = DISCORD_LOGIN_URL,
                    onLoadingStateChange = { browser, isLoading, _, _ ->
                        if (isLoginCompleted) return@JcefBrowser
                        val currentUrl = browser.url ?: ""
                        if (!isLoading && (currentUrl.contains("discord.com/app", ignoreCase = true) || currentUrl.contains("discord.com/channels", ignoreCase = true))) {
                            browser.executeJavaScript(DISCORD_JS_SNIPPET, currentUrl, 0)
                        }
                    },
                    onJsAlert = { browser, message ->
                        if (!isLoginCompleted && message.isNotBlank() && message != "undefined" && message != "null") {
                            isLoginCompleted = true
                            browser.stopLoad()
                            SwingUtilities.invokeLater { browser.close(true) }

                            state.value = WebViewState.Finished
                            onLoginDone(message)
                            true
                        } else {
                            false
                        }
                    },
                    onPageFinished = {},
                    modifier = Modifier.fillMaxSize(),
                )
            }
            jcef is JcefState.Error -> {
                BrowserErrorFallback(discord = true, onRetry = retry)
            }
        }
        aboveContent()
    }
}

// --- Error / Fallback UI ------------------------------------------------------

@Composable
private fun BrowserErrorFallback(
    discord: Boolean = false,
    onRetry: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Browser failed to load. Please try again.",
            style = typo().titleSmall,
            color = Color.White,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Text(
                text = "Retry",
                style = typo().labelMedium,
                color = Color.DarkGray,
            )
        }
        Spacer(Modifier.height(24.dp))
        Text(
            stringResource(Res.string.desktop_webview_description),
            style = typo().bodySmall,
            color = Color.White.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = {
                val url = if (discord)
                    "https://www.dspot.app/blogs/en/how-to-log-in-to-Discord-on-desktop-app"
                else
                    "https://www.dspot.app/blogs/en/how-to-log-in-on-desktop-app"
                openUrl(url)
            },
        ) {
            Text(
                stringResource(Res.string.open_blog_post),
                style = typo().labelMedium,
                color = Color.DarkGray,
                textAlign = TextAlign.Center,
            )
        }
    }
}