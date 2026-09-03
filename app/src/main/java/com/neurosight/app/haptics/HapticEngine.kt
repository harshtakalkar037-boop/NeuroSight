package com.neurosight.app.haptics

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Drives distinct, continuously-repeating vibration patterns for each of the
 * three classes. Uses [VibrationEffect.createWaveform] with `repeat` set to
 * loop indefinitely until [stop] (or a class change) cancels it.
 *
 * ---- HOW TO ADJUST PATTERNS ----
 * Each pattern is a `timings` long[] alternating [off, on, off, on, ...] in
 * milliseconds, paired with an `amplitudes` int[] (0-255, or
 * DEFAULT_AMPLITUDE) of the same length. To change the "feel" of a class,
 * edit its entry in [patternFor] below -- e.g. shorten the on/off durations
 * for a faster pulse, or change amplitude for a stronger/weaker buzz.
 */
class HapticEngine(context: Context) {

    private val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        manager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    private var currentClass: String? = null

    /** Starts (or continues) the vibration pattern for [className]. No-op if already active. */
    fun startForClass(className: String) {
        if (className == currentClass) return // Already vibrating this pattern; avoid re-triggering.
        currentClass = className

        if (!vibrator.hasVibrator()) return

        val (timings, amplitudes) = patternFor(className)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // repeat = index into `timings` to loop back to (0 = loop the whole pattern).
            val effect = VibrationEffect.createWaveform(timings, amplitudes, 0)
            vibrator.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(timings, 0)
        }
    }

    /** Immediately stops any active vibration. */
    fun stop() {
        currentClass = null
        vibrator.cancel()
    }

    /**
     * Returns the (timings, amplitudes) waveform for a given class.
     *   - "wall":   low-frequency, slow steady pulse -- 600ms on / 600ms off.
     *   - "door":   medium-frequency, regular pulse  -- 400ms on / 400ms off.
     *   - "person": high-frequency, fast pulse       -- 200ms on / 200ms off.
     * Unknown classes fall back to a gentle single blip so the app never
     * vibrates unpredictably on an unexpected label.
     */
    private fun patternFor(className: String): Pair<LongArray, IntArray> {
        return when (className) {
            "wall" -> LongArray(20) { if (it % 2 == 0) 600L else 600L } to
                IntArray(20) { if (it % 2 == 0) 0 else 140 }
            "door" -> LongArray(30) { if (it % 2 == 0) 400L else 400L } to
                IntArray(30) { if (it % 2 == 0) 0 else 200 }
            "person" -> LongArray(50) { if (it % 2 == 0) 200L else 200L } to
                IntArray(50) { if (it % 2 == 0) 0 else 255 }
            else -> longArrayOf(0, 150) to intArrayOf(0, 120)
        }
    }
}
