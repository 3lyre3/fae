package com.foxfire.viewer

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.inputmethod.InputMethodManager
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.IntentCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.io.BufferedReader

/**
 * Foxfire — a small green light that renders local HTML and lets you
 * find your way around inside a page. It does one thing, gently.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var findBar: View
    private lateinit var findField: EditText
    private lateinit var findCount: TextView
    private lateinit var emptyState: View
    private lateinit var firefly: View
    private lateinit var searchFab: FloatingActionButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webView)
        findBar = findViewById(R.id.findBar)
        findField = findViewById(R.id.findField)
        findCount = findViewById(R.id.findCount)
        emptyState = findViewById(R.id.emptyState)
        firefly = findViewById(R.id.firefly)
        searchFab = findViewById(R.id.searchFab)

        configureWebView()
        wireFindInPage()
        handleIntent(intent)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun configureWebView() {
        webView.settings.apply {
            javaScriptEnabled = true          // real documents can be interactive
            domStorageEnabled = true
            allowFileAccess = true            // relative assets beside a file:// page
            allowContentAccess = true
            loadWithOverviewMode = true
            useWideViewPort = true
            builtInZoomControls = true        // pinch-zoom, no clunky buttons
            displayZoomControls = false
            setSupportZoom(true)
            cacheMode = WebSettings.LOAD_NO_CACHE
        }
        webView.setBackgroundColor(0x00000000)
        webView.setFindListener { activeMatchOrdinal, numberOfMatches, isDoneCounting ->
            if (!isDoneCounting) return@setFindListener
            findCount.text = if (numberOfMatches == 0) "0 / 0"
                else "${activeMatchOrdinal + 1} / $numberOfMatches"
        }
    }

    private fun wireFindInPage() {
        searchFab.setOnClickListener { toggleFindBar(true) }
        findViewById<ImageButton>(R.id.findClose).setOnClickListener { toggleFindBar(false) }
        findViewById<ImageButton>(R.id.findNext).setOnClickListener {
            hideKeyboard(); webView.findNext(true)
        }
        findViewById<ImageButton>(R.id.findPrev).setOnClickListener {
            hideKeyboard(); webView.findNext(false)
        }
        findField.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString().orEmpty()
                if (query.isEmpty()) {
                    webView.clearMatches()
                    findCount.text = "0 / 0"
                } else {
                    webView.findAllAsync(query)
                }
            }
        })
        findField.setOnEditorActionListener { _, _, _ ->
            hideKeyboard(); webView.findNext(true); true
        }
    }

    private fun toggleFindBar(show: Boolean) {
        findBar.visibility = if (show) View.VISIBLE else View.GONE
        searchFab.visibility =
            if (show || webView.visibility != View.VISIBLE) View.GONE else View.VISIBLE
        if (show) {
            findField.requestFocus()
            imm().showSoftInput(findField, InputMethodManager.SHOW_IMPLICIT)
        } else {
            findField.text?.clear()
            webView.clearMatches()
            hideKeyboard()
        }
    }

    private fun imm() =
        getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

    private fun hideKeyboard() =
        imm().hideSoftInputFromWindow(findField.windowToken, 0)

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val uri: Uri? = when (intent?.action) {
            Intent.ACTION_VIEW -> intent.data
            Intent.ACTION_SEND ->
                IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
            else -> null
        }
        if (uri != null) render(uri) else showEmpty()
    }

    private fun showEmpty() {
        emptyState.visibility = View.VISIBLE
        webView.visibility = View.GONE
        findBar.visibility = View.GONE
        searchFab.visibility = View.GONE
        startPulse()
    }

    private var pulsing = false
    private fun startPulse() {
        if (pulsing) return
        pulsing = true
        ObjectAnimator.ofPropertyValuesHolder(
            firefly,
            PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 1.09f),
            PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 1.09f),
            PropertyValuesHolder.ofFloat(View.ALPHA, 0.8f, 1f)
        ).apply {
            duration = 2200
            repeatCount = ObjectAnimator.INFINITE
            repeatMode = ObjectAnimator.REVERSE
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
    }

    private fun render(uri: Uri) {
        emptyState.visibility = View.GONE
        webView.visibility = View.VISIBLE
        searchFab.visibility = View.VISIBLE
        try {
            if (uri.scheme == "file") {
                // load in place so sibling images / CSS resolve naturally
                webView.loadUrl(uri.toString())
            } else {
                val html = contentResolver.openInputStream(uri)?.use { input ->
                    input.bufferedReader().use(BufferedReader::readText)
                } ?: error("Couldn't open that file.")
                webView.loadDataWithBaseURL(uri.toString(), html, "text/html", "UTF-8", null)
            }
        } catch (e: Exception) {
            val safe = (e.localizedMessage ?: "Unknown snag").replace("<", "&lt;")
            val page = "<html><head><meta name='viewport' " +
                "content='width=device-width,initial-scale=1'></head>" +
                "<body style=\"margin:0;padding:2rem;background:#0a0f0c;color:#e8f5ed;" +
                "font-family:sans-serif;line-height:1.5\">" +
                "<h2 style=\"color:#74e39b;font-family:serif\">Couldn&#39;t light this one</h2>" +
                "<p style=\"color:#7fa793\">$safe</p></body></html>"
            webView.loadDataWithBaseURL(null, page, "text/html", "UTF-8", null)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        when {
            findBar.visibility == View.VISIBLE -> toggleFindBar(false)
            webView.visibility == View.VISIBLE && webView.canGoBack() -> webView.goBack()
            else -> @Suppress("DEPRECATION") super.onBackPressed()
        }
    }
}
