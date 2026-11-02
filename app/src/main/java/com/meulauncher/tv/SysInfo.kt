package com.meulauncher.tv

import android.app.ActivityManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.SystemClock
import java.io.File

object SysInfo {
    private const val MB = 1024L * 1024L

    class Mem(val totalMb: Long, val usedMb: Long, val availMb: Long, val percent: Int)

    fun memory(ctx: Context): Mem {
        val am = ctx.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val mi = ActivityManager.MemoryInfo()
        am.getMemoryInfo(mi)
        val total = mi.totalMem / MB
        val avail = mi.availMem / MB
        val used = (total - avail).coerceAtLeast(0L)
        val pct = if (total > 0) ((used * 100) / total).toInt() else 0
        return Mem(total, used, avail, pct)
    }

    val cores: Int
        get() = Runtime.getRuntime().availableProcessors().coerceAtLeast(1)

    /** Maior frequência atual entre os núcleos, em MHz. Null se o aparelho não deixar ler. */
    fun cpuFreqMhz(): Int? {
        var max = 0L
        for (i in 0 until cores) {
            try {
                val t = File("/sys/devices/system/cpu/cpu$i/cpufreq/scaling_cur_freq").readText().trim().toLong()
                if (t > max) max = t
            } catch (e: Exception) {
                // sem acesso a este núcleo
            }
        }
        return if (max > 0) (max / 1000).toInt() else null
    }

    /** Encerra apps de terceiros em segundo plano. Retorna (apps, MB liberados). */
    fun optimize(ctx: Context): Pair<Int, Long> {
        val am = ctx.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val before = memory(ctx).availMb
        var count = 0
        for (ai in ctx.packageManager.getInstalledApplications(0)) {
            if (ai.packageName == ctx.packageName) continue
            if ((ai.flags and ApplicationInfo.FLAG_SYSTEM) != 0) continue
            try {
                am.killBackgroundProcesses(ai.packageName)
                count++
            } catch (e: Exception) {
                // ignora
            }
        }
        Thread.sleep(800)
        val after = memory(ctx).availMb
        return Pair(count, (after - before).coerceAtLeast(0L))
    }
}

/** Estima o uso de CPU do próprio launcher (em % do total de núcleos). */
class CpuTracker {
    private var lastCpu = android.os.Process.getElapsedCpuTime()
    private var lastTime = SystemClock.elapsedRealtime()

    fun appPercent(): Int {
        val cpu = android.os.Process.getElapsedCpuTime()
        val now = SystemClock.elapsedRealtime()
        val dc = cpu - lastCpu
        val dt = now - lastTime
        lastCpu = cpu
        lastTime = now
        if (dt <= 0) return 0
        return ((dc * 100) / (dt * SysInfo.cores)).toInt().coerceIn(0, 100)
    }
}
