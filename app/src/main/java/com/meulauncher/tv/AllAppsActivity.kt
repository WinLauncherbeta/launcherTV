package com.meulauncher.tv

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class AllAppsActivity : Activity() {

    private lateinit var prefs: Prefs
    private lateinit var rv: RecyclerView
    private var favs = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Prefs(this)
        favs = prefs.favorites()

        val root = FrameLayout(this)
        val scrim = View(this)
        scrim.setBackgroundColor(Color.parseColor("#CC000000"))
        root.addView(scrim, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))

        val col = LinearLayout(this)
        col.orientation = LinearLayout.VERTICAL
        col.setPadding(dp(48), dp(32), dp(48), dp(16))

        val title = TextView(this)
        title.text = "Todos os apps"
        title.textSize = 26f
        title.setTextColor(Color.WHITE)
        col.addView(title)

        val hint = TextView(this)
        hint.text = "Segure OK em um app para fixar ou remover do dock  ·  ★ = fixado"
        hint.textSize = 14f
        hint.setTextColor(Color.parseColor("#AAFFFFFF"))
        val lpHint = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        lpHint.topMargin = dp(4)
        lpHint.bottomMargin = dp(12)
        col.addView(hint, lpHint)

        rv = RecyclerView(this)
        rv.layoutManager = GridLayoutManager(this, 7)
        rv.clipToPadding = false
        col.addView(rv, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))

        root.addView(col, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        setContentView(root)

        Thread {
            val apps = Apps.listAll(this)
            runOnUiThread {
                rv.adapter = AppAdapter(apps)
                rv.post { rv.getChildAt(0)?.requestFocus() }
            }
        }.start()
    }

    private inner class Holder(val view: LinearLayout, val icon: ImageView, val label: TextView) :
        RecyclerView.ViewHolder(view)

    private inner class AppAdapter(private val apps: List<AppItem>) : RecyclerView.Adapter<Holder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            val item = LinearLayout(this@AllAppsActivity)
            item.orientation = LinearLayout.VERTICAL
            item.gravity = Gravity.CENTER_HORIZONTAL
            item.setPadding(dp(8), dp(10), dp(8), dp(10))
            item.background = focusBg(14)
            item.isFocusable = true
            item.isClickable = true
            item.scaleOnFocus(1.08f)

            val icon = ImageView(this@AllAppsActivity)
            icon.scaleType = ImageView.ScaleType.FIT_CENTER
            item.addView(icon, LinearLayout.LayoutParams(dp(64), dp(64)))

            val label = TextView(this@AllAppsActivity)
            label.textSize = 13f
            label.setTextColor(Color.WHITE)
            label.maxLines = 1
            label.ellipsize = android.text.TextUtils.TruncateAt.END
            label.gravity = Gravity.CENTER
            val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            lp.topMargin = dp(6)
            item.addView(label, lp)

            item.layoutParams = RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            )
            return Holder(item, icon, label)
        }

        override fun getItemCount(): Int = apps.size

        override fun onBindViewHolder(h: Holder, position: Int) {
            val app = apps[position]
            h.icon.setImageDrawable(app.icon)
            h.label.text = (if (favs.contains(app.pkg)) "★ " else "") + app.label
            h.view.setOnClickListener { Apps.launch(this@AllAppsActivity, app) }
            h.view.setOnLongClickListener {
                if (favs.contains(app.pkg)) {
                    favs.remove(app.pkg)
                    Toast.makeText(this@AllAppsActivity, "${app.label} removido do dock", Toast.LENGTH_SHORT).show()
                } else {
                    favs.add(app.pkg)
                    Toast.makeText(this@AllAppsActivity, "${app.label} fixado no dock", Toast.LENGTH_SHORT).show()
                }
                prefs.setFavorites(favs)
                notifyItemChanged(h.adapterPosition)
                true
            }
        }
    }
}
