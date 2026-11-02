package com.meulauncher.tv

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.widget.Toast

class AppItem(val pkg: String, val label: String, val icon: Drawable, val intent: Intent)

object Apps {
    /** Apps que aparecem no dock por padrão (se estiverem instalados). */
    val defaultFavorites = listOf(
        "com.google.android.youtube.tv",
        "com.netflix.ninja",
        "com.amazon.amazonvideo.livingroom",
        "com.disney.disneyplus",
        "com.apple.atve.androidtv.appletv",
        "com.spotify.tv.android",
        "tv.twitch.android.app",
        "com.plexapp.android",
        "com.microsoft.xboxone.smartglass"
    )

    fun launchIntent(pm: PackageManager, pkg: String): Intent? =
        pm.getLeanbackLaunchIntentForPackage(pkg) ?: pm.getLaunchIntentForPackage(pkg)

    fun load(ctx: Context, pkg: String): AppItem? {
        val pm = ctx.packageManager
        val intent = launchIntent(pm, pkg) ?: return null
        val info = try {
            pm.getApplicationInfo(pkg, 0)
        } catch (e: PackageManager.NameNotFoundException) {
            return null
        }
        return AppItem(pkg, info.loadLabel(pm).toString(), info.loadIcon(pm), intent)
    }

    fun listAll(ctx: Context): List<AppItem> {
        val pm = ctx.packageManager
        val pkgs = LinkedHashSet<String>()
        for (cat in listOf(Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER)) {
            val i = Intent(Intent.ACTION_MAIN)
            i.addCategory(cat)
            for (ri in pm.queryIntentActivities(i, 0)) {
                pkgs.add(ri.activityInfo.packageName)
            }
        }
        pkgs.remove(ctx.packageName)
        return pkgs.mapNotNull { load(ctx, it) }.sortedBy { it.label.lowercase() }
    }

    fun launch(ctx: Context, app: AppItem) {
        try {
            val i = Intent(app.intent)
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ctx.startActivity(i)
        } catch (e: Exception) {
            Toast.makeText(ctx, "Não foi possível abrir ${app.label}", Toast.LENGTH_SHORT).show()
        }
    }
}
