package com.verdant.faereader

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.io.InputStream

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var searchInput: EditText
    private lateinit var matchCountText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webView)
        searchInput = findViewById(R.id.searchInput)
        matchCountText = findViewById(R.id.matchCountText)

        val btnPrev: ImageButton = findViewById(R.id.btnPrev)
        val btnNext: ImageButton = findViewById(R.id.btnNext)
        val btnCloseSearch: ImageButton = findViewById(R.id.btnCloseSearch)

        setupWebView()
        setupFindInPage(btnPrev, btnNext, btnCloseSearch)

        // Read incoming HTML Uri from "Open With" long-press
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun setupWebView() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = true
            allowContentAccess = true
            loadWithOverviewMode = true
            useWideViewPort = true
            builtInZoomControls = true
            displayZoomControls = false
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        }

        // Listens to native WebView search results
        webView.setFindListener { activeMatchOrdinal, numberOfMatches, isDone ->
            if (isDone) {
                val current = if (numberOfMatches > 0) activeMatchOrdinal + 1 else 0
                matchCountText.text = "$current/$numberOfMatches wisps"
            }
        }
    }

    private fun handleIntent(intent: Intent?) {
        val uri: Uri? = intent?.data
        if (uri != null) {
            loadHtmlFromUri(uri)
        } else {
            // Whimsical default home screen when launched directly
            val landingPage = """
                <!DOCTYPE html>
                <html>
                <body style="background:#0d1a12; color:#a7f3d0; font-family:serif; text-align:center; padding:2em;">
                    <h1 style="color:#34d399; font-style:italic;">🍃 Verdant Reader</h1>
                    <p style="color:#6ee7b7;">Long-press any local HTML file in your Pixel's file manager to open it here.</p>
                </body>
                </html>
            """.trimIndent()
            webView.loadDataWithBaseURL(null, landingPage, "text/html", "UTF-8", null)
        }
    }

    private fun loadHtmlFromUri(uri: Uri) {
        try {
            val contentStream: InputStream? = contentResolver.openInputStream(uri)
            val htmlContent = contentStream?.bufferedReader()?.use { it.readText() } ?: ""
            
            // Base URL set to URI allows relative resources (images/CSS) to resolve if accessible
            webView.loadDataWithBaseURL(uri.toString(), htmlContent, "text/html", "UTF-8", null)
        } catch (e: Exception) {
            val errorHtml = "<html><body style='background:#0d1a12; color:#f87171; padding:1em;'><h3>Failed to open file</h3><p>${e.localizedMessage}</p></body></html>"
            webView.loadDataWithBaseURL(null, errorHtml, "text/html", "UTF-8", null)
        }
    }

    private fun setupFindInPage(btnPrev: ImageButton, btnNext: ImageButton, btnClose: ImageButton) {
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s?.toString() ?: ""
                if (query.isNotEmpty()) {
                    webView.findAllAsync(query)
                } else {
                    webView.clearMatches()
                    matchCountText.text = "0 wisps"
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        btnNext.setOnClickListener { webView.findNext(true) }
        btnPrev.setOnClickListener { webView.findNext(false) }
        btnClose.setOnClickListener {
            searchInput.text.clear()
            webView.clearMatches()
        }
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}

