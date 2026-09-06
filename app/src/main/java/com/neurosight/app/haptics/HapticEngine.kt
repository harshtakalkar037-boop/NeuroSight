package com.neurosight.app.haptics

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/** Drives distinct continuously repeating vibration patterns for each class. */
class HapticEngine(context: Context) {

    private val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        manager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    private var currentClass: String? = null

    fun startForClass(className: String) {
        if (className == currentClass) return
        currentClass = className
        if (!vibrator.hasVibrator()) return

        val (timings, amplitudes) = patternFor(className)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, 0))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(timings, 0)
        }
    }

    fun stop() {
        currentClass = null
        vibrator.cancel()
    }

    /** Distinct tactile signatures for the five model outputs. */
    private fun patternFor(className: String): Pair<LongArray, IntArray> = when (className) {
        "door" -> waveform(onMs = 400L, offMs = 400L, amplitude = 200)
        "window" -> waveform(onMs = 180L, offMs = 180L, amplitude = 180)
        "chair" -> waveform(onMs = 220L, offMs = 420L, amplitude = 160)
        "table" -> waveform(onMs = 120L, offMs = 480L, amplitude = 220)
        "cabinet" -> waveform(onMs = 500L, offMs = 250L, amplitude = 200)
        else -> longArrayOf(0, 150) to intArrayOf(0, 120)
    }

    private fun waveform(onMs: Long, offMs: Long, amplitude: Int): Pair<LongArray, IntArray> {
        val timings = LongArray(20) { if (it % 2 == 0) offMs else onMs }
        val amplitudes = IntArray(20) { if (it % 2 == 0) 0 else amplitude }
        return timings to amplitudes
    }
}
