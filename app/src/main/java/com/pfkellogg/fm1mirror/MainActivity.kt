package com.pfkellogg.fm1mirror

import android.app.Activity
import android.os.Bundle
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.view.Gravity
import java.io.ByteArrayInputStream
import java.io.FileNotFoundException

class MainActivity : Activity() {
    private lateinit var midi: MirrorMidi
    private lateinit var screen: View
    private lateinit var root: LinearLayout
    private lateinit var content: FrameLayout
    private lateinit var toolbar: LinearLayout
    private lateinit var fullscreen: Button
    private lateinit var status: TextView
    private lateinit var detail: TextView
    private lateinit var mode: Button
    private lateinit var filter: Button
    private val frame = ScreenFrame()
    private val bitmap = Bitmap.createBitmap(240, 240, Bitmap.Config.ARGB_8888)
    private val paint = Paint()
    private val destination = RectF()
    private var firmware: String? = null
    private var emulating = false
    private var expanded = false
    private var detailVisible = false
    private var emulator: WebView? = null

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(18, 20, 26))
            setPadding(dp(8), dp(8), dp(8), dp(8))
        }
        setContentView(root)
        toolbar = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        root.addView(toolbar)
        mode = Button(this).apply {
            text = "Modo espejo real"
            contentDescription = "Cambiar al modo emulador"
            setOnClickListener { showEmulator(!emulating) }
        }
        toolbar.addView(mode)
        status = TextView(this).apply {
            textSize = 15f
            text = "Esperando FM-1 por USB-C"
            accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
            setPadding(dp(10), 0, dp(10), 0)
        }
        toolbar.addView(status, LinearLayout.LayoutParams(0, -2, 1f))
        filter = Button(this).apply {
            text = "Nítido"
            setOnClickListener {
                paint.isFilterBitmap = !paint.isFilterBitmap
                text = if (paint.isFilterBitmap) "Suavizado" else "Nítido"
                screen.invalidate()
            }
        }
        toolbar.addView(filter)
        toolbar.addView(Button(this).apply {
            text = "MIDI"
            setOnClickListener {
                detailVisible = !detailVisible
                detail.visibility = if (detailVisible) View.VISIBLE else View.GONE
                if (detailVisible) updateDetail()
            }
        })
        fullscreen = Button(this).apply {
            text = "Solo pantalla"
            setOnClickListener { showOnlyScreen(true) }
        }
        toolbar.addView(fullscreen)
        detail = TextView(this).apply {
            textSize = 14f
            gravity = Gravity.CENTER
            visibility = View.GONE
        }
        root.addView(detail)
        updateDetail()
        content = FrameLayout(this).apply { setBackgroundColor(Color.BLACK) }
        root.addView(content, LinearLayout.LayoutParams(-1, 0, 1f))
        screen = object : View(this) {
            override fun onDraw(canvas: Canvas) {
                frame.consume { pixels, top, bottom ->
                    bitmap.setPixels(pixels, top * 240, 240, 0, top, 240, bottom - top)
                }
                if (detailVisible && !expanded) updateDetail()
                val side = minOf(width, height).toFloat()
                val x = (width - side) / 2
                val y = (height - side) / 2
                destination.set(x, y, x + side, y + side)
                canvas.drawBitmap(bitmap, null, destination, paint)
            }
        }.apply {
            contentDescription = "Pantalla del sintetizador, 240 por 240 píxeles"
            setOnClickListener { if (expanded) showOnlyScreen(false) }
        }
        content.addView(screen, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        bitmap.eraseColor(Color.BLACK)
        midi = MirrorMidi(this, { value -> runOnUiThread {
            if (!emulating) status.text = value
        } }, frame, { runOnUiThread {
            if (!emulating) screen.postInvalidateOnAnimation()
        } }, { identity -> runOnUiThread {
            firmware = identity
            updateDetail()
        } })
        if (state?.getBoolean("emulator") == true) showEmulator(true)
    }

    private fun updateDetail() {
        val label = if (emulating) "Emulación local WebAssembly" else
            "Firmware: ${firmware ?: "sin identificar"}    ·    Paquetes de pantalla: ${frame.packetCount()}"
        if (detail.text != label) detail.text = label
    }

    private fun showOnlyScreen(enabled: Boolean) {
        expanded = enabled
        toolbar.visibility = if (enabled) View.GONE else View.VISIBLE
        detail.visibility = if (!enabled && detailVisible) View.VISIBLE else View.GONE
        val inset = if (enabled) 0 else dp(8)
        root.setPadding(inset, inset, inset, inset)
        screen.contentDescription = if (enabled) "Mostrar controles" else "Pantalla del sintetizador, 240 por 240 píxeles"
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onBackPressed() {
        when {
            expanded -> showOnlyScreen(false)
            emulating -> showEmulator(false)
            else -> super.onBackPressed()
        }
    }

    private fun showEmulator(enabled: Boolean) {
        emulating = enabled
        midi.setActive(!enabled)
        mode.text = if (enabled) "Modo emulador" else "Modo espejo real"
        mode.contentDescription = if (enabled) "Cambiar al modo espejo real" else "Cambiar al modo emulador"
        status.text = if (enabled) "DARK-1 local" else "Esperando FM-1 por USB-C"
        filter.visibility = if (enabled) View.GONE else View.VISIBLE
        fullscreen.visibility = if (enabled) View.GONE else View.VISIBLE
        screen.visibility = if (enabled) View.GONE else View.VISIBLE
        updateDetail()
        if (enabled) {
            emulator = WebView(this).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.allowFileAccess = false
                settings.allowContentAccess = false
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest) =
                        request.url.host != "dark1.local"
                    override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse {
                        val name = request.url.lastPathSegment
                        val mime = when (name) {
                            "index.html" -> "text/html"
                            "worklet.js" -> "text/javascript"
                            "amb.wasm" -> "application/wasm"
                            else -> "application/octet-stream"
                        }
                        if (request.url.scheme == "https" && request.url.host == "dark1.local" &&
                            name in setOf("index.html", "worklet.js", "amb.wasm")) {
                            try {
                                return WebResourceResponse(mime, if (name == "amb.wasm") null else "UTF-8",
                                    assets.open("emu/$name"))
                            } catch (_: FileNotFoundException) { }
                        }
                        return WebResourceResponse("text/plain", "UTF-8", 404, "Not Found", emptyMap(),
                            ByteArrayInputStream(byteArrayOf()))
                    }
                }
                loadUrl("https://dark1.local/index.html")
            }
            content.addView(emulator, FrameLayout.LayoutParams(-1, -1))
        } else destroyEmulator()
    }

    private fun destroyEmulator() {
        emulator?.let {
            content.removeView(it)
            it.destroy()
        }
        emulator = null
    }

    override fun onStart() {
        super.onStart()
        midi.setActive(!emulating)
        emulator?.onResume()
        window.insetsController?.apply {
            systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsets.Type.systemBars())
        }
    }

    override fun onStop() {
        midi.setActive(false)
        emulator?.onPause()
        super.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean("emulator", emulating)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        midi.close()
        destroyEmulator()
        super.onDestroy()
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density + 0.5f).toInt()
}
