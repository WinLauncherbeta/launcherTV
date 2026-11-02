package com.meulauncher.tv

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Outline
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.VideoView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {

    private lateinit var prefs: Prefs
    private lateinit var video: VideoView
    private lateinit var tvDay: TextView
    private lateinit var tvDate: TextView
    private lateinit var tvWeather: TextView
    private lateinit var tvTime: TextView
    private lateinit var monitor: TextView
    private lateinit var dock: LinearLayout

    private val handler = Handler(Looper.getMainLooper())
    private val cpu = CpuTracker()
    private val loc = Locale("pt", "BR")

    private val clockTick = object : Runnable {
        override fun run() {
            updateClock()
            handler.postDelayed(this, 1000L)
        }
    }

    private val monitorTick = object : Runnable {
        override fun run() {
            updateMonitor()
            handler.postDelayed(this, if (prefs.economy) 4000L else 2000L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Prefs(this)

        val root = FrameLayout(this)
        root.clipChildren = false

        // Fundo animado (o fundo estático fica no próprio window background)
        video = VideoView(this)
        video.isFocusable = false
        video.isFocusableInTouchMode = false
        root.addView(
            video,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        )

        // Relógio (canto superior esquerdo)
        val anurati = Typeface.createFromAsset(assets, "fonts/anurati.otf")
        val shadow = Color.parseColor("#99000000")

        tvDay = TextView(this)
        tvDay.typeface = anurati
        tvDay.textSize = 40f
        tvDay.letterSpacing = 0.2f
        tvDay.setTextColor(Color.WHITE)
        tvDay.setShadowLayer(10f, 0f, 2f, shadow)

        tvDate = TextView(this)
        tvDate.typeface = anurati
        tvDate.textSize = 16f
        tvDate.letterSpacing = 0.15f
        tvDate.setTextColor(Color.parseColor("#FFEDE5")) 
        tvDate.setShadowLayer(8f, 0f, 2f, shadow)

        tvWeather = TextView(this)
        tvWeather.textSize = 16f
        tvWeather.setTextColor(Color.WHITE)
        tvWeather.setShadowLayer(8f, 0f, 2f, shadow)

        tvTime = TextView(this)
        tvTime.textSize = 16f
        tvTime.letterSpacing = 0.1f
        tvTime.setTextColor(Color.WHITE)
        tvTime.setShadowLayer(8f, 0f, 2f, shadow)

        val timeRow = LinearLayout(this)
        timeRow.orientation = LinearLayout.HORIZONTAL
        timeRow.gravity = Gravity.CENTER_VERTICAL
        timeRow.addView(tvWeather)
        val gap = View(this)
        timeRow.addView(gap, LinearLayout.LayoutParams(dp(14), 1))
        timeRow.addView(tvTime)

        val clockBox = LinearLayout(this)
        clockBox.orientation = LinearLayout.VERTICAL
        clockBox.gravity = Gravity.CENTER_HORIZONTAL
        clockBox.addView(tvDay)
        val lpDate = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        lpDate.topMargin = dp(6)
        clockBox.addView(tvDate, lpDate)
        val lpRow = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        lpRow.topMargin = dp(6)
        clockBox.addView(timeRow, lpRow)

        val lpClock = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
            Gravity.TOP or Gravity.START
        )
        lpClock.setMargins(dp(32), dp(28), 0, 0)
        root.addView(clockBox, lpClock)

        // Monitor de RAM / CPU
        monitor = TextView(this)
        monitor.textSize = 13f
        monitor.setTextColor(Color.WHITE)
        monitor.setPadding(dp(14), dp(8), dp(14), dp(8))
        val mbg = GradientDrawable()
        mbg.cornerRadius = dpf(12)
        mbg.setColor(Color.parseColor("#88000000"))
        monitor.background = mbg
        root.addView(
            monitor,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.TOP or Gravity.END
            )
        )

        // Dock
        dock = LinearLayout(this)
        dock.orientation = LinearLayout.HORIZONTAL
        dock.gravity = Gravity.CENTER_VERTICAL
        dock.clipChildren = false
        dock.setPadding(dp(8), dp(6), dp(8), dp(6))
        val dbg = GradientDrawable()
        dbg.cornerRadius = dpf(22)
        dbg.setColor(Color.parseColor("#66000000"))
        dock.background = dbg

        val scroller = HorizontalScrollView(this)
        scroller.isHorizontalScrollBarEnabled = false
        scroller.clipToPadding = false
        scroller.clipChildren = false
        scroller.isFocusable = false
        scroller.setPadding(dp(14), dp(16), dp(14), dp(16))
        scroller.addView(dock)
        root.addView(
            scroller,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            )
        )

        setContentView(root)
    }

    override fun onStart() {
        super.onStart()
        applyBackground()
    }

    override fun onResume() {
        super.onResume()
        positionMonitor()
        buildDock()
        updateClock()
        showWeather()
        refreshWeather()
        handler.removeCallbacks(clockTick)
        handler.removeCallbacks(monitorTick)
        handler.postDelayed(clockTick, 1000L)
        handler.post(monitorTick)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(clockTick)
        handler.removeCallbacks(monitorTick)
    }

    override fun onStop() {
        super.onStop()
        // libera o decodificador de vídeo quando outro app está aberto
        video.stopPlayback()
    }

    private fun applyBackground() {
        val mode = if (prefs.economy) 2 else prefs.wallpaper
        if (mode == 2) {
            video.stopPlayback()
            video.visibility = View.GONE
            return
        }
        video.visibility = View.VISIBLE
        val res = if (mode == 0) R.raw.wallpaper_1 else R.raw.wallpaper_2
        video.setOnPreparedListener { mp ->
            mp.isLooping = true
            mp.setVolume(0f, 0f)
        }
        video.setOnErrorListener { _, _, _ ->
            video.visibility = View.GONE
            true
        }
        video.setVideoURI(Uri.parse("android.resource://$packageName/$res"))
        video.start()
    }

    private fun positionMonitor() {
        val lp = monitor.layoutParams as FrameLayout.LayoutParams
        lp.gravity = if (prefs.monitorBottom) (Gravity.BOTTOM or Gravity.END) else (Gravity.TOP or Gravity.END)
        lp.setMargins(0, dp(28), dp(32), dp(28))
        monitor.layoutParams = lp
        monitor.visibility = if (prefs.monitorOn) View.VISIBLE else View.GONE
    }

    private fun updateClock() {
        val now = Date()
        tvDay.text = SimpleDateFormat("EEEE", loc).format(now).substringBefore('-').trim().uppercase(loc)
        tvDate.text = SimpleDateFormat("dd MMMM, yyyy.", loc).format(now).uppercase(loc)
        val pattern = if (prefs.clock24) "HH:mm" else "hh:mm a"
        tvTime.text = "- " + SimpleDateFormat(pattern, loc).format(now) + " -"
    }

    private fun showWeather() {
        tvWeather.text = if (prefs.weatherCity == prefs.city) prefs.weatherText else ""
    }

    private fun refreshWeather() {
        val now = System.currentTimeMillis()
        val fresh = now - prefs.weatherTime < 30L * 60L * 1000L && prefs.weatherCity == prefs.city
        if (fresh) return
        val city = prefs.city
        Thread {
            val r = Weather.fetch(city)
            if (r != null) {
                prefs.weatherText = Weather.emoji(r.code) + " " + r.temp + "°"
                prefs.weatherCity = city
                prefs.weatherTime = System.currentTimeMillis()
                runOnUiThread { showWeather() }
            }
        }.start()
    }

    private fun updateMonitor() {
        if (!prefs.monitorOn) {
            monitor.visibility = View.GONE
            return
        }
        monitor.visibility = View.VISIBLE
        val m = SysInfo.memory(this)
        val freq = SysInfo.cpuFreqMhz()
        val cpuLine = "CPU ${SysInfo.cores} núcleos" +
            (if (freq != null) " · $freq MHz" else "") +
            " · launcher ${cpu.appPercent()}%"
        monitor.text = "RAM ${m.usedMb}/${m.totalMb} MB (${m.percent}%)\n$cpuLine"
        monitor.setTextColor(
            when {
                m.percent >= 85 -> Color.parseColor("#FF6B6B")
                m.percent >= 70 -> Color.parseColor("#FFD166")
                else -> Color.parseColor("#7CF29A")
            }
        )
    }

    // ---------- Dock ----------

    private fun buildDock() {
        dock.removeAllViews()
        val sizeDp = intArrayOf(48, 60, 72)[prefs.iconSize.coerceIn(0, 2)]

        val iconApps = resources.getDrawable(R.drawable.ic_apps, theme)
        dock.addView(
            tile(iconApps, sizeDp, Color.parseColor("#1E6BFF"), 10, {
                startActivity(Intent(this, AllAppsActivity::class.java))
            }, null)
        )

        val favs = prefs.favorites()
        for (pkg in favs) {
            val app = Apps.load(this, pkg) ?: continue
            dock.addView(
                tile(app.icon, sizeDp, null, 0, { Apps.launch(this, app) }, {
                    val list = prefs.favorites()
                    list.remove(pkg)
                    prefs.setFavorites(list)
                    buildDock()
                    dock.getChildAt(0)?.requestFocus()
                })
            )
        }

        val iconGear = resources.getDrawable(R.drawable.ic_gear, theme)
        dock.addView(
            tile(iconGear, sizeDp, Color.parseColor("#555B66"), 10, {
                startActivity(Intent(this, SettingsActivity::class.java))
            }, null)
        )

        dock.getChildAt(0)?.requestFocus()
    }

    private fun tile(
        icon: Drawable,
        sizeDp: Int,
        bgColor: Int?,
        padDp: Int,
        onClick: () -> Unit,
        onLong: (() -> Unit)?
    ): View {
        val wrap = FrameLayout(this)
        wrap.setPadding(dp(4), dp(4), dp(4), dp(4))
        wrap.background = focusBg(18)
        wrap.isFocusable = true
        wrap.isClickable = true
        wrap.clipChildren = false

        val iv = ImageView(this)
        iv.setImageDrawable(icon)
        iv.scaleType = ImageView.ScaleType.FIT_CENTER
        if (bgColor != null) {
            val bg = GradientDrawable()
            bg.cornerRadius = dpf(14)
            bg.setColor(bgColor)
            iv.background = bg
            iv.setPadding(dp(padDp), dp(padDp), dp(padDp), dp(padDp))
        }
        val radius = dpf(14)
        iv.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, radius)
            }
        }
        iv.clipToOutline = true
        wrap.addView(iv, FrameLayout.LayoutParams(dp(sizeDp), dp(sizeDp)))

        wrap.setOnClickListener { onClick() }
        if (onLong != null) {
            wrap.setOnLongClickListener {
                onLong()
                true
            }
        }
        wrap.scaleOnFocus(1.15f)

        val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        lp.setMargins(dp(4), 0, dp(4), 0)
        wrap.layoutParams = lp
        return wrap
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            startActivity(Intent(this, SettingsActivity::class.java))
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onBackPressed() {
        // launcher não fecha com o botão voltar
    }
}
