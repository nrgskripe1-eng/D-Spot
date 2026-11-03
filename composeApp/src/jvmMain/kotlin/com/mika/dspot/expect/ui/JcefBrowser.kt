package com.mika.dspot.expect.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import com.sun.jna.Native
import com.sun.jna.platform.win32.User32
import com.sun.jna.platform.win32.WinDef.DWORD
import com.sun.jna.platform.win32.WinDef.HWND
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.friwi.jcefmaven.CefAppBuilder
import me.friwi.jcefmaven.MavenCefAppHandlerAdapter
import org.cef.CefApp
import org.cef.CefClient
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.handler.CefLoadHandlerAdapter
import org.cef.misc.BoolRef
import org.cef.network.CefCookie
import org.cef.network.CefCookieManager
import java.awt.BorderLayout
import java.io.File
import java.util.concurrent.CountDownLatch
import javax.swing.JPanel
import javax.swing.SwingUtilities

// ─── Singleton CEF app ────────────────────────────────────────────────────────

private var _cefApp: CefApp? = null
internal var _cefInitialised = false
private val _cefLock = Any()

/**
 * Returns the process-wide [CefApp] singleton.
 * Must be called from a background thread (e.g. [Dispatchers.IO]).
 */
internal fun acquireCefApp(): CefApp {
    if (_cefInitialised) return _cefApp!!
    synchronized(_cefLock) {
        if (_cefInitialised) return _cefApp!!

        val installDir = File(System.getProperty("user.home"), ".dspot/jcef")
        val cacheDir   = File(System.getProperty("user.home"), ".dspot/jcef-cache")
        cacheDir.mkdirs()

        val builder = CefAppBuilder().apply {
            setInstallDir(installDir)
            val helperExe = File(installDir, "jcef_helper.exe")
            if (helperExe.exists()) {
                cefSettings.browser_subprocess_path = helperExe.absolutePath
            }
            addJcefArgs(
                "--no-sandbox",
                "--disable-gpu-process-crash-limit",
                "--disable-dev-shm-usage",
                "--enable-gpu-rasterization",
                "--enable-zero-copy",
                "--ignore-gpu-blocklist",
            )
            cefSettings.cache_path = cacheDir.absolutePath
            cefSettings.persist_session_cookies = true
            cefSettings.windowless_rendering_enabled = false
        }

        var buildError: Throwable? = null
        SwingUtilities.invokeAndWait {
            try {
                _cefApp = builder.build()
                _cefInitialised = true
            } catch (t: Throwable) {
                buildError = t
            }
        }
        buildError?.let { throw it }

        return _cefApp!!
    }
}

// ─── Cookie bridge ────────────────────────────────────────────────────────────

/**
 * A [WebViewCookieManager] backed by JCEF's global [CefCookieManager].
 *
 * This reads from Chromium's actual cookie store — completely separate from
 * Java's java.net.CookieHandler. Using this class is the only way to get
 * cookies that JCEF stores during a web login.
 */
internal class JcefCookieManager : WebViewCookieManager {

    private val mgr: CefCookieManager?
        get() = CefCookieManager.getGlobalManager()

    override fun getCookie(url: String): String {
        val manager = mgr ?: return ""
        val cookies = mutableMapOf<String, String>()

        // 1. Visit URL-specific cookies
        val latch = CountDownLatch(1)
        val urlVisitResult = manager.visitUrlCookies(url, true, object : org.cef.callback.CefCookieVisitor {
            override fun visit(
                cookie: CefCookie,
                count: Int,
                total: Int,
                delete: BoolRef,
            ): Boolean {
                if (cookie.name != null && cookie.value != null) {
                    cookies[cookie.name] = cookie.value
                }
                // Always countDown on the last cookie OR if this is the only/last visit
                if (count >= total - 1) latch.countDown()
                return true
            }
        })
        // If visitUrlCookies returns false, no cookies exist → unblock immediately
        if (!urlVisitResult) latch.countDown()
        try { latch.await(3, java.util.concurrent.TimeUnit.SECONDS) } catch (_: InterruptedException) {}

        // 2. Also visit all cookies to pick up relevant domain cookies (e.g. .youtube.com, .google.com, .spotify.com)
        val domain = try {
            java.net.URI(url).host?.removePrefix("www.")?.removePrefix("music.")?.removePrefix("accounts.") ?: ""
        } catch (_: Exception) {
            ""
        }

        if (domain.isNotEmpty()) {
            val domainLatch = CountDownLatch(1)
            val allVisitResult = manager.visitAllCookies(object : org.cef.callback.CefCookieVisitor {
                override fun visit(
                    cookie: CefCookie,
                    count: Int,
                    total: Int,
                    delete: BoolRef,
                ): Boolean {
                    if (cookie.domain != null && cookie.domain.contains(domain, ignoreCase = true)) {
                        if (cookie.name != null && cookie.value != null) {
                            cookies[cookie.name] = cookie.value
                        }
                    }
                    if (count >= total - 1) domainLatch.countDown()
                    return true
                }
            })
            if (!allVisitResult) domainLatch.countDown()
            try { domainLatch.await(3, java.util.concurrent.TimeUnit.SECONDS) } catch (_: InterruptedException) {}
        }

        return cookies.entries.joinToString("; ") { "${it.key}=${it.value}" }
    }

    override fun removeAllCookies() {
        mgr?.deleteCookies("", "")
    }

    /**
     * Extracts all cookies matching [domainFilter] (e.g. "youtube.com").
     */
    fun getCookiesForDomain(domainFilter: String): String {
        val manager = mgr ?: return ""
        val cookies = mutableMapOf<String, String>()
        val latch   = CountDownLatch(1)

        val visitResult = manager.visitAllCookies(object : org.cef.callback.CefCookieVisitor {
            override fun visit(
                cookie: CefCookie,
                count: Int,
                total: Int,
                delete: BoolRef,
            ): Boolean {
                if (cookie.domain != null && cookie.domain.contains(domainFilter, ignoreCase = true)) {
                    if (cookie.name != null && cookie.value != null) {
                        cookies[cookie.name] = cookie.value
                    }
                }
                if (count >= total - 1) latch.countDown()
                return true
            }
        })
        if (!visitResult) latch.countDown()

        try { latch.await(3, java.util.concurrent.TimeUnit.SECONDS) } catch (_: InterruptedException) {}
        return cookies.entries.joinToString("; ") { "${it.key}=${it.value}" }
    }
}

/**
 * Forces Windows DWM to re-evaluate the window composition hierarchy
 * so the embedded Chromium HWND becomes visible inside the Compose window.
 */
private var lastWin32RepaintTime = 0L

internal fun forceWin32RepaintGlobal(panel: JPanel?, browser: CefBrowser?) {
    if (panel == null) return
    // Skip forced repaints when the panel is intentionally hidden so the HWND
    // stops painting YouTube Music content to the screen.
    if (!panel.isVisible) return
    val now = System.currentTimeMillis()
    if (now - lastWin32RepaintTime < 50) return
    lastWin32RepaintTime = now

    try {
        browser?.createImmediately()

        val isWindows = System.getProperty("os.name", "")
            .contains("Windows", ignoreCase = true)
        if (!isWindows) {
            panel.revalidate()
            panel.repaint()
            return
        }

        val user32 = User32.INSTANCE

        val awtWindow = SwingUtilities.getWindowAncestor(panel)
        if (awtWindow != null && awtWindow.isShowing) {
            val windowPtr = Native.getComponentPointer(awtWindow)
            if (windowPtr != null) {
                val windowHwnd = HWND(windowPtr)
                val SWP_NOSIZE       = 0x0001
                val SWP_NOMOVE       = 0x0002
                val SWP_NOZORDER     = 0x0004
                val SWP_FRAMECHANGED = 0x0020
                user32.SetWindowPos(
                    windowHwnd, null,
                    0, 0, 0, 0,
                    SWP_NOSIZE or SWP_NOMOVE or SWP_NOZORDER or SWP_FRAMECHANGED
                )
            }
        }

        val panelPtr = Native.getComponentPointer(panel)
        if (panelPtr != null) {
            val panelHwnd = HWND(panelPtr)
            val rdwFlags = DWORD(
                (0x0001 /* RDW_INVALIDATE */ or
                 0x0080 /* RDW_ALLCHILDREN */ or
                 0x0100 /* RDW_UPDATENOW  */).toLong()
            )
            user32.RedrawWindow(panelHwnd, null, null, rdwFlags)
        }

        panel.revalidate()
        panel.repaint()
    } catch (_: Throwable) {
        panel.revalidate()
        panel.repaint()
    }
}

// ─── Browser composable ───────────────────────────────────────────────────────

/**
 * Renders a real Chromium page (JCEF) inside a Compose Desktop window.
 */
@Composable
internal fun JcefBrowser(
    url: String,
    onPageFinished: (String) -> Unit = {},
    onLoadingStateChange: ((browser: CefBrowser, isLoading: Boolean, canGoBack: Boolean, canGoForward: Boolean) -> Unit)? = null,
    onJsAlert: ((browser: CefBrowser, message: String) -> Boolean)? = null,
    visible: Boolean = true,
    modifier: Modifier = Modifier.fillMaxSize(),
) {
    val panelRef   = remember { mutableStateOf<JPanel?>(null) }
    val clientRef  = remember { mutableStateOf<CefClient?>(null) }
    val browserRef = remember { mutableStateOf<CefBrowser?>(null) }
    var ready by remember { mutableStateOf(false) }

    LaunchedEffect(url) {
        withContext(Dispatchers.IO) {
            acquireCefApp()
        }

        withContext(Dispatchers.IO) {
            SwingUtilities.invokeAndWait {
                val app    = _cefApp ?: return@invokeAndWait
                val client = app.createClient()
                val repaintCallback = {
                    SwingUtilities.invokeLater {
                        forceWin32RepaintGlobal(panelRef.value, browserRef.value)
                    }
                }

                client.addLoadHandler(object : CefLoadHandlerAdapter() {
                    override fun onLoadingStateChange(
                        browser: CefBrowser?,
                        isLoading: Boolean,
                        canGoBack: Boolean,
                        canGoForward: Boolean,
                    ) {
                        if (browser != null && onLoadingStateChange != null) {
                            onLoadingStateChange(browser, isLoading, canGoBack, canGoForward)
                        }
                    }

                    override fun onLoadEnd(
                        browser: CefBrowser?,
                        frame: CefFrame?,
                        httpStatusCode: Int,
                    ) {
                        repaintCallback()
                        if (frame?.isMain == true) {
                            onPageFinished(browser?.url ?: "")
                        }
                    }
                })

                if (onJsAlert != null) {
                    client.addJSDialogHandler(object : org.cef.handler.CefJSDialogHandlerAdapter() {
                        override fun onJSDialog(
                            browser: CefBrowser?,
                            originUrl: String?,
                            dialogType: org.cef.handler.CefJSDialogHandler.JSDialogType?,
                            message_text: String?,
                            default_prompt_text: String?,
                            callback: org.cef.callback.CefJSDialogCallback?,
                            suppress_message: org.cef.misc.BoolRef?,
                        ): Boolean {
                            if (browser != null && message_text != null) {
                                val handled = onJsAlert(browser, message_text)
                                if (handled) {
                                    callback?.Continue(true, "")
                                    suppress_message?.set(true)
                                    return true
                                }
                            }
                            return false
                        }
                    })
                }

                val browser = client.createBrowser(url, false, false)
                val panel   = object : JPanel(BorderLayout()) {
                    override fun addNotify() {
                        super.addNotify()
                        repaintCallback()
                    }
                }.also { p ->
                    p.add(browser.uiComponent, BorderLayout.CENTER)
                    p.addComponentListener(object : java.awt.event.ComponentAdapter() {
                        override fun componentResized(e: java.awt.event.ComponentEvent?) {
                            browser.createImmediately()
                            repaintCallback()
                        }
                        override fun componentShown(e: java.awt.event.ComponentEvent?) {
                            browser.createImmediately()
                            repaintCallback()
                        }
                    })
                }
                clientRef.value  = client
                browserRef.value = browser
                panelRef.value   = panel
            }
        }

        ready = true
    }

    DisposableEffect(Unit) {
        onDispose {
            val browser = browserRef.value
            val client  = clientRef.value
            SwingUtilities.invokeLater {
                browser?.close(true)
                client?.dispose()
            }
        }
    }

    if (ready && panelRef.value != null) {
        SwingPanel(
            factory = {
                val panel   = panelRef.value!!
                val browser = browserRef.value

                fun forceWin32Repaint() {
                    forceWin32RepaintGlobal(panel, browser)
                }

                val repaintExecutor = {
                    SwingUtilities.invokeLater { forceWin32Repaint() }
                }

                repaintExecutor()

                panel.addHierarchyListener { e ->
                    val showingChanged =
                        (e.changeFlags and java.awt.event.HierarchyEvent.SHOWING_CHANGED.toLong()) != 0L
                    if (showingChanged && panel.isShowing) {
                        repaintExecutor()
                    }
                }

                val intervals = intArrayOf(20, 50, 100, 200, 350, 600, 1000)
                for (delay in intervals) {
                    javax.swing.Timer(delay) { forceWin32Repaint() }.apply {
                        isRepeats = false
                        start()
                    }
                }

                panel
            },
            update = { panel ->
                // Propagate Compose `visible` state to the AWT heavyweight component.
                // Setting isVisible=false hides the native HWND from Win32/DWM so
                // Chromium stops drawing its content on screen, while the CEF process
                // (cookie manager, network stack) continues running in the background.
                if (panel.isVisible != visible) {
                    panel.isVisible = visible
                    val uiComp = browserRef.value?.uiComponent
                    if (uiComp != null && uiComp.isVisible != visible) {
                        uiComp.isVisible = visible
                    }
                    panel.revalidate()
                    panel.repaint()
                } else {
                    panel.revalidate()
                    panel.repaint()
                }
            },
            modifier = modifier,
        )
    }
}
