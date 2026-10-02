package com.example.snapweb

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature

/**
 * Snapchat-Web wrapper.
 *
 * web.snapchat.com gates on screen WIDTH, not just the user-agent — a bare UA
 * swap gets bounced to "open the app". "Desktop site" mode works because it
 * also forces a wide desktop-width viewport. Android has no one-switch for
 * that, so we (a) send a desktop UA and (b) inject a script BEFORE the page's
 * own JS that forces a 1280px viewport and hides touch. Viability probe — see
 * README.
 */
class MainActivity : ComponentActivity() {

    private val startUrl = "https://web.snapchat.com/"

    private val desktopUserAgent =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
        "(KHTML, like Gecko) Chrome/140.0.0.0 Safari/537.36"

    private val desktopSpoofScript = """
        (function () {
          function forceViewport() {
            var vp = document.querySelector('meta[name="viewport"]');
            if (!vp) {
              vp = document.createElement('meta');
              vp.setAttribute('name', 'viewport');
              (document.head || document.documentElement).appendChild(vp);
            }
            vp.setAttribute('content', 'width=1280');
          }
          forceViewport();
          document.addEventListener('DOMContentLoaded', forceViewport);
          try { Object.defineProperty(navigator, 'maxTouchPoints', { get: function () { return 0; } }); } catch (e) {}
          try {
            Object.defineProperty(screen, 'width',  { get: function () { return 1280; } });
            Object.defineProperty(screen, 'height', { get: function () { return 800;  } });
          } catch (e) {}
        })();
    """.trimIndent()

    private lateinit var webView: WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestAvPermissions()

        webView = WebView(this).apply {
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                mediaPlaybackRequiresUserGesture = false
                userAgentString = desktopUserAgent
                useWideViewPort = true
                loadWithOverviewMode = true
                setSupportZoom(true)
                builtInZoomControls = true
                displayZoomControls = false
            }

            CookieManager.getInstance().apply {
                setAcceptCookie(true)
                setAcceptThirdPartyCookies(this@apply, true)
            }

            if (WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
                WebViewCompat.addDocumentStartJavaScript(this, desktopSpoofScript, setOf("*"))
            }

            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView,
                    request: WebResourceRequest
                ): Boolean = false
            }

            webChromeClient = object : WebChromeClient() {
                override fun onPermissionRequest(request: PermissionRequest) {
                    runOnUiThread { request.grant(request.resources) }
                }
            }

            loadUrl(startUrl)
        }

        setContentView(webView)
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        if (webView.canGoBack()) webView.goBack() else super.onBackPressed()
    }

    private fun requestAvPermissions() {
        val needed = arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
            .filter {
                ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
            }
        if (needed.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, needed.toTypedArray(), 1001)
        }
    }
}
