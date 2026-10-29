package com.webview.videoplayer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private var currentSiteUrl: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Ask for site URL on first launch
        val input = EditText(this).apply {
            hint = "https://example.com"
            setText("https://")
        }
        AlertDialog.Builder(this)
            .setTitle("Введите адрес сайта")
            .setMessage("Укажи URL сайта с видео (с http:// или https://)")
            .setView(input)
            .setCancelable(false)
            .setPositiveButton("Открыть") { _, _ ->
                val url = input.text.toString().trim()
                if (url.isNotEmpty()) {
                    currentSiteUrl = url
                    showWebView(url)
                } else {
                    finish()
                }
            }
            .setNegativeButton("Выход") { _, _ -> finish() }
            .show()
    }

    private fun showWebView(url: String) {
        setContentView(R.layout.activity_main)
        webView = findViewById(R.id.webView)

        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(webView, true)

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            allowFileAccess = true
            allowContentAccess = true
            mediaPlaybackRequiresUserGesture = false
            userAgentString = "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
        }

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val u = request?.url?.toString() ?: return false
                // If it's a video file, offer to open in external player
                if (isVideoUrl(u)) {
                    openVideoInPlayer(u)
                    return true
                }
                return false
            }
        }

        webView.loadUrl(url)
    }

    private fun isVideoUrl(url: String): Boolean {
        val lower = url.lowercase()
        return lower.endsWith(".m3u8") || lower.endsWith(".mp4") || lower.endsWith(".mkv") ||
            lower.endsWith(".avi") || lower.endsWith(".webm") || lower.endsWith(".ts") ||
            lower.contains("/video/") || lower.contains("playlist.m3u8")
    }

    private fun openVideoInPlayer(url: String) {
        val uri = Uri.parse(url)
        val intent = Intent(Intent.ACTION_VIEW, uri)
        // Let the system show all apps that can play video
        val chooser = Intent.createChooser(intent, "Открыть видео в...")
        try {
            startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(this, "Нет приложения для воспроизведения видео", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onBackPressed() {
        if (::webView.isInitialized && webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    override fun onPause() {
        super.onPause()
        if (::webView.isInitialized) {
            CookieManager.getInstance().flush()
            webView.onPause()
        }
    }

    override fun onResume() {
        super.onResume()
        if (::webView.isInitialized) webView.onResume()
    }

    override fun onDestroy() {
        if (::webView.isInitialized) {
            CookieManager.getInstance().flush()
            webView.destroy()
        }
        super.onDestroy()
    }
}