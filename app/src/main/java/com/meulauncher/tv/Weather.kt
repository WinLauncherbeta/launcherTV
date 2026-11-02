package com.meulauncher.tv

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** Clima via Open-Meteo (gratuito, sem chave). */
object Weather {
    class Result(val temp: Int, val code: Int)

    fun fetch(city: String): Result? {
        try {
            val geoUrl = "https://geocoding-api.open-meteo.com/v1/search?count=1&language=pt&name=" +
                URLEncoder.encode(city, "UTF-8")
            val results = JSONObject(get(geoUrl)).optJSONArray("results") ?: return null
            if (results.length() == 0) return null
            val g = results.getJSONObject(0)
            val lat = g.getDouble("latitude")
            val lon = g.getDouble("longitude")
            val url = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon" +
                "&current=temperature_2m,weather_code&timezone=auto"
            val cur = JSONObject(get(url)).getJSONObject("current")
            return Result(Math.round(cur.getDouble("temperature_2m")).toInt(), cur.getInt("weather_code"))
        } catch (e: Exception) {
            return null
        }
    }

    private fun get(u: String): String {
        val c = URL(u).openConnection() as HttpURLConnection
        c.connectTimeout = 8000
        c.readTimeout = 8000
        try {
            return c.inputStream.bufferedReader().use { it.readText() }
        } finally {
            c.disconnect()
        }
    }

    fun emoji(code: Int): String = when (code) {
        0 -> "☀️"
        1, 2 -> "⛅"
        3 -> "☁️"
        45, 48 -> "🌫️"
        in 51..57 -> "🌦️"
        in 61..67 -> "🌧️"
        in 71..77 -> "❄️"
        in 80..82 -> "🌧️"
        in 85..86 -> "❄️"
        in 95..99 -> "⛈️"
        else -> "🌡️"
    }
}
