package com.webview.videoplayer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.io.ByteArrayInputStream

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private var currentSiteUrl: String = ""

    // Player preference: "ask", "vlc", "mx", "system"
    private val prefsName = "player_prefs"
    private val keyPlayer = "player"

    // Ad domains to block
    private val adDomains = listOf(
        "doubleclick.net", "googlesyndication.com", "googleadservices.com",
        "adservice.google.com", "pagead2.googlesyndication.com",
        "adnxs.com", "adsystem.com", "amazon-adsystem.com",
        "outbrain.com", "taboola.com", "criteo.com", "criteo.net",
        "popads.net", "propellerads.com", "adsterra.com",
        "yandex.ru/ads", "an.yandex.ru", "mc.yandex.ru/metrika",
        "google-analytics.com", "googletagmanager.com",
        "facebook.com/tr", "connect.facebook.net",
        "ads.twitter.com", "static.ads-twitter.com"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Ask for site URL on first launch
        val input = EditText(this).apply {
            hint = "https://example.com"
            setText("https://")
        }
        AlertDialog.Builder(this)
            .setTitle("Введите адрес сайта")
            .setMessage("Укажи URL сайта с видео")
            .setView(input)
            .setCancelable(false)
            .setPositiveButton("Открыть") { _, _ ->
                val url = input.text.toString().trim()
                if (url.isNotEmpty()) {
                    currentSiteUrl = url
                    showWebView(url)
                    // Ask for player preference on first launch
                    if (getPlayerPref() == null) askPlayerPreference()
                } else {
                    finish()
                }
            }
            .setNegativeButton("Выход") { _, _ -> finish() }
            .show()
    }

    private fun getPlayerPref(): String? =
        getSharedPreferences(prefsName, MODE_PRIVATE).getString(keyPlayer, null)

    private fun setPlayerPref(value: String) {
        getSharedPreferences(prefsName, MODE_PRIVATE).edit().putString(keyPlayer, value).apply()
    }

    private fun askPlayerPreference() {
        val options = arrayOf("Спрашивать каждый раз", "VLC", "MX Player", "Системный плеер")
        AlertDialog.Builder(this)
            .setTitle("Выберите плеер для видео")
            .setItems(options) { _, which ->
                val value = when (which) {
                    0 -> "ask"
                    1 -> "vlc"
                    2 -> "mx"
                    else -> "system"
                }
                setPlayerPref(value)
                Toast.makeText(this, "Сохранено: ${options[which]}", Toast.LENGTH_SHORT).show()
            }
            .setCancelable(false)
            .show()
    }

    private fun showWebView(url: String) {
        setContentView(R.layout.activity_main)
        val webView = findViewById<WebView>(R.id.webView)

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
                if (isVideoUrl(u)) {
                    playVideo(u)
                    return true
                }
                return false
            }

            override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                val url = request?.url?.toString() ?: return null
                // Ad blocker: block known ad domains
                val host = request.url.host?.lowercase() ?: ""
                if (adDomains.any { host.contains(it) } || url.lowercase().contains("/ads/") ||
                    url.lowercase().contains("doubleclick") || url.lowercase().contains("googlesyndication")) {
                    return WebResourceResponse("text/plain", "UTF-8", ByteArrayInputStream("".toByteArray()))
                }
                return null
            }
        }

        webView.loadUrl(url)
        this.webView = webView
    }

    private fun isVideoUrl(url: String): Boolean {
        val lower = url.lowercase()
        return lower.endsWith(".m3u8") || lower.endsWith(".mp4") || lower.endsWith(".mkv") ||
            lower.endsWith(".avi") || lower.endsWith(".webm") || lower.endsWith(".ts") ||
            lower.contains("/video/") || lower.contains("playlist.m3u8")
    }

    private fun playVideo(url: String) {
        val pref = getPlayerPref() ?: "ask"
        val uri = Uri.parse(url)

        when (pref) {
            "vlc" -> launchPlayer("org.videolan.vlc", uri) { showPlayerChooser(url) }
            "mx" -> launchPlayer("com.mxtech.videoplayer.ad", uri) {
                launchPlayer("com.mxtech.videoplayer.pro", uri) { showPlayerChooser(url) }
            }
            "system" -> {
                val intent = Intent(Intent.ACTION_VIEW, uri)
                startActivity(Intent.createChooser(intent, "Открыть видео в..."))
            }
            else -> showPlayerChooser(url)
        }
    }

    private fun launchPlayer(packageName: String, uri: Uri, onNotFound: () -> Unit) {
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage(packageName)
        }
        try {
            startActivity(intent)
        } catch (e: Exception) {
            onNotFound()
        }
    }

    private fun showPlayerChooser(url: String) {
        val uri = Uri.parse(url)
        val intent = Intent(Intent.ACTION_VIEW, uri)
        try {
            startActivity(Intent.createChooser(intent, "Открыть видео в..."))
        } catch (e: Exception) {
            Toast.makeText(this, "Нет приложения для воспроизведения видео", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateOptionsMenu(menu: android.view.Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_choose_player -> {
                askPlayerPreference()
                true
            }
            R.id.action_change_site -> {
                val input = EditText(this).apply { setText(currentSiteUrl) }
                AlertDialog.Builder(this)
                    .setTitle("Сменить сайт")
                    .setView(input)
                    .setPositiveButton("Открыть") { _, _ ->
                        val url = input.text.toString().trim()
                        if (url.isNotEmpty()) {
                            currentSiteUrl = url
                            webView.loadUrl(url)
                        }
                    }
                    .setNegativeButton("Отмена", null)
                    .show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
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
        CookieManager.getInstance().flush()
        if (::webView.isInitialized) webView.destroy()
        super.onDestroy()
    }
}