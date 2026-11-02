package com.meulauncher.tv

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class SettingsActivity : Activity() {

    private lateinit var prefs: Prefs
    private lateinit var list: LinearLayout
    private lateinit var tvRam: TextView
    private val refreshers = mutableListOf<() -> Unit>()
    private val handler = Handler(Looper.getMainLooper())

    private val ramTick = object : Runnable {
        override fun run() {
            updateRam()
            handler.postDelayed(this, 2000L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Prefs(this)

        val root = FrameLayout(this)
        val scrim = View(this)
        scrim.setBackgroundColor(Color.parseColor("#D9000000"))
        root.addView(scrim, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))

        list = LinearLayout(this)
        list.orientation = LinearLayout.VERTICAL
        list.setPadding(dp(120), dp(32), dp(120), dp(32))

        val title = TextView(this)
        title.text = "Configurações"
        title.textSize = 28f
        title.setTextColor(Color.WHITE)
        val lpTitle = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        lpTitle.bottomMargin = dp(16)
        list.addView(title, lpTitle)

        section("Visual")
        val wallpapers = arrayOf("Vídeo 1", "Vídeo 2", "Imagem estática")
        addRow("Papel de parede", { wallpapers[prefs.wallpaper.coerceIn(0, 2)] }) {
            prefs.wallpaper = (prefs.wallpaper + 1) % 3
        }
        val sizes = arrayOf("Pequeno", "Médio", "Grande")
        addRow("Tamanho dos ícones", { sizes[prefs.iconSize.coerceIn(0, 2)] }) {
            prefs.iconSize = (prefs.iconSize + 1) % 3
        }
        addRow("Formato do relógio", { if (prefs.clock24) "24 horas" else "12 horas" }) {
            prefs.clock24 = !prefs.clock24
        }
        addRow("Cidade do clima", { prefs.city }) { askCity() }

        section("Monitor")
        addRow("Monitor de RAM / CPU", { onOff(prefs.monitorOn) }) { prefs.monitorOn = !prefs.monitorOn }
        addRow("Posição do monitor", { if (prefs.monitorBottom) "Inferior direito" else "Superior direito" }) {
            prefs.monitorBottom = !prefs.monitorBottom
        }
        addRow("Modo economia (sem vídeo, monitor mais lento)", { onOff(prefs.economy) }) {
            prefs.economy = !prefs.economy
        }

        section("Memória")
        tvRam = TextView(this)
        tvRam.textSize = 16f
        tvRam.setTextColor(Color.parseColor("#7CF29A"))
        tvRam.setPadding(dp(20), dp(4), dp(20), dp(8))
        list.addView(tvRam)
        addRow("Otimizar memória (encerra apps em segundo plano)", { "" }) {
            Toast.makeText(this, "Otimizando...", Toast.LENGTH_SHORT).show()
            Thread {
                val r = SysInfo.optimize(this)
                runOnUiThread {
                    Toast.makeText(
                        this,
                        "Apps encerrados: ${r.first} · memória liberada: ${r.second} MB",
                        Toast.LENGTH_LONG
                    ).show()
                    updateRam()
                }
            }.start()
        }

        section("Sistema")
        addRow("Abrir configurações do Android", { "" }) {
            try {
                startActivity(Intent(Settings.ACTION_SETTINGS))
            } catch (e: Exception) {
                Toast.makeText(this, "Não foi possível abrir", Toast.LENGTH_SHORT).show()
            }
        }
        addRow("Definir como launcher padrão", { "" }) {
            try {
                startActivity(Intent(Settings.ACTION_HOME_SETTINGS))
            } catch (e: Exception) {
                Toast.makeText(this, "Aperte o botão Home e escolha Meu Launcher", Toast.LENGTH_LONG).show()
            }
        }
        addRow("Restaurar dock padrão", { "" }) {
            prefs.resetFavorites()
            Toast.makeText(this, "Dock restaurado", Toast.LENGTH_SHORT).show()
        }
        addRow("Versão", { "0.1" }) { }

        val scroll = ScrollView(this)
        scroll.addView(list)
        root.addView(scroll, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        setContentView(root)
        updateRam()
    }

    override fun onResume() {
        super.onResume()
        handler.post(ramTick)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(ramTick)
    }

    private fun onOff(v: Boolean) = if (v) "Ligado" else "Desligado"

    private fun updateRam() {
        val m = SysInfo.memory(this)
        tvRam.text = "RAM em uso: ${m.usedMb} / ${m.totalMb} MB (${m.percent}%) · livre: ${m.availMb} MB"
    }

    private fun section(name: String) {
        val t = TextView(this)
        t.text = name.uppercase()
        t.textSize = 13f
        t.letterSpacing = 0.15f
        t.setTextColor(Color.parseColor("#99FFFFFF"))
        val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        lp.topMargin = dp(14)
        lp.bottomMargin = dp(6)
        list.addView(t, lp)
    }

    private fun addRow(title: String, value: () -> String, onClick: () -> Unit) {
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER_VERTICAL
        row.setPadding(dp(20), dp(14), dp(20), dp(14))
        row.background = focusBg(12)
        row.isFocusable = true
        row.isClickable = true

        val t = TextView(this)
        t.text = title
        t.textSize = 18f
        t.setTextColor(Color.WHITE)
        val v = TextView(this)
        v.textSize = 18f
        v.setTextColor(Color.parseColor("#FFC857"))

        row.addView(t, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        row.addView(v, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        val refresh: () -> Unit = { v.text = value() }
        refresh()
        refreshers.add(refresh)

        row.setOnClickListener {
            onClick()
            for (r in refreshers) r()
        }
        val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        lp.bottomMargin = dp(4)
        list.addView(row, lp)
    }

    private fun askCity() {
        val et = EditText(this)
        et.setText(prefs.city)
        et.setSingleLine()
        AlertDialog.Builder(this)
            .setTitle("Cidade do clima")
            .setView(et)
            .setPositiveButton("Salvar") { _, _ ->
                val c = et.text.toString().trim()
                if (c.isNotEmpty()) {
                    prefs.city = c
                    prefs.weatherTime = 0L
                }
                for (r in refreshers) r()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}
