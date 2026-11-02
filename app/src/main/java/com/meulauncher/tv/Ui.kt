package com.meulauncher.tv

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.view.View

fun Context.dp(v: Int): Int = (v * resources.displayMetrics.density + 0.5f).toInt()

fun Context.dpf(v: Int): Float = v * resources.displayMetrics.density

/** Fundo que ganha borda branca quando o item está em foco (controle remoto). */
fun Context.focusBg(radius: Int): StateListDrawable {
    val focused = GradientDrawable()
    focused.cornerRadius = dpf(radius)
    focused.setColor(Color.parseColor("#33FFFFFF"))
    focused.setStroke(dp(3), Color.WHITE)
    val rest = GradientDrawable()
    rest.cornerRadius = dpf(radius)
    rest.setColor(Color.TRANSPARENT)
    val sl = StateListDrawable()
    sl.addState(intArrayOf(android.R.attr.state_focused), focused)
    sl.addState(intArrayOf(), rest)
    return sl
}

fun View.scaleOnFocus(scale: Float) {
    onFocusChangeListener = View.OnFocusChangeListener { v, has ->
        val s = if (has) scale else 1f
        v.animate().scaleX(s).scaleY(s).setDuration(120).start()
    }
}
