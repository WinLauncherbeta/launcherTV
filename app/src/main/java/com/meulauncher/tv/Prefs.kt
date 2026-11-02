package com.meulauncher.tv

import android.content.Context
import android.content.SharedPreferences

class Prefs(ctx: Context) {
    private val app = ctx.applicationContext
    private val sp: SharedPreferences = app.getSharedPreferences("launcher", Context.MODE_PRIVATE)

    /** 0 = vídeo 1, 1 = vídeo 2, 2 = imagem estática */
    var wallpaper: Int
        get() = sp.getInt("wallpaper", 0)
        set(v) { sp.edit().putInt("wallpaper", v).apply() }

    /** 0 = pequeno, 1 = médio, 2 = grande */
    var iconSize: Int
        get() = sp.getInt("iconSize", 1)
        set(v) { sp.edit().putInt("iconSize", v).apply() }

    var clock24: Boolean
        get() = sp.getBoolean("clock24", true)
        set(v) { sp.edit().putBoolean("clock24", v).apply() }

    var city: String
        get() = sp.getString("city", "São Paulo") ?: "São Paulo"
        set(v) { sp.edit().putString("city", v).apply() }

    var monitorOn: Boolean
        get() = sp.getBoolean("monitorOn", true)
        set(v) { sp.edit().putBoolean("monitorOn", v).apply() }

    var monitorBottom: Boolean
        get() = sp.getBoolean("monitorBottom", false)
        set(v) { sp.edit().putBoolean("monitorBottom", v).apply() }

    var economy: Boolean
        get() = sp.getBoolean("economy", false)
        set(v) { sp.edit().putBoolean("economy", v).apply() }

    var weatherText: String
        get() = sp.getString("weatherText", "") ?: ""
        set(v) { sp.edit().putString("weatherText", v).apply() }

    var weatherTime: Long
        get() = sp.getLong("weatherTime", 0L)
        set(v) { sp.edit().putLong("weatherTime", v).apply() }

    var weatherCity: String
        get() = sp.getString("weatherCity", "") ?: ""
        set(v) { sp.edit().putString("weatherCity", v).apply() }

    /** Apps fixados no dock. Se o usuário nunca mexeu, usa a lista padrão (só os instalados). */
    fun favorites(): MutableList<String> {
        val raw = sp.getString("fav", null)
        if (raw == null) {
            val pm = app.packageManager
            return Apps.defaultFavorites.filter { Apps.launchIntent(pm, it) != null }.toMutableList()
        }
        return if (raw.isEmpty()) mutableListOf() else raw.split("|").toMutableList()
    }

    fun setFavorites(list: List<String>) {
        sp.edit().putString("fav", list.joinToString("|")).apply()
    }

    fun resetFavorites() {
        sp.edit().remove("fav").apply()
    }
}
