package com.neurosight.app.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.sin

/**
 * Generates and plays distinct audio cues per class.
 *
 * ---- WHY AudioTrack INSTEAD OF SoundPool ----
 * The spec calls for SoundPool or AudioTrack, "whichever is more appropriate."
 * SoundPool plays pre-recorded short clips loaded from res/raw or assets --
 * that would require bundling actual audio files for three continuous tones.
 * AudioTrack lets us synthesize the sine-wave tones directly in code with no
 * bundled audio assets at all, which keeps the app fully self-contained for
 * the hackathon. If you'd rather ship WAV/MP3 assets and use SoundPool
 * instead, swap this class's internals but keep the same
 * startForClass()/stop() interface so the rest of the app doesn't change.
 *
 * ---- HOW TO ADJUST TONES ----
 * Edit [ToneSpec] values in [specFor]:
 *   - frequencyHz: pitch of the tone.
 *   - pulseOnMs / pulseOffMs: for pulsed tones, on/off duration in ms.
 *     Set pulseOffMs = 0 for a continuous (non-pulsed) tone.
 */
class AudioEngine {

    private data class ToneSpec(
        val frequencyHz: Double,
        val pulseOnMs: Long,
        val pulseOffMs: Long
    )

    private val sampleRate = 44100
    private var currentClass: String? = null
    private var playJob: Job? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Starts (or continues) the tone for [className]. No-op if already playing that class's tone. */
    fun startForClass(className: String) {
        if (className == currentClass) return
        stopInternal()
        currentClass = className

        val spec = specFor(className) ?: return
        playJob = scope.launch { playLoop(spec) }
    }

    /** Stops any currently playing tone. */
    fun stop() {
        currentClass = null
        stopInternal()
    }

    private fun stopInternal() {
        playJob?.cancel()
        playJob = null
    }

    /**
     * Tone definitions per class:
     *   - "wall":   ~200 Hz, continuous (no pulsing) -- a steady low drone.
     *   - "door":   ~400 Hz, pulsed on/off every 300ms -- a mid-pitch rhythm.
     *   - "person": ~600 Hz, pulsed on/off every 120ms -- a fast, urgent chirp.
     */
    private fun specFor(className: String): ToneSpec? = when (className) {
        "wall" -> ToneSpec(frequencyHz = 200.0, pulseOnMs = 1000, pulseOffMs = 0)
        "door" -> ToneSpec(frequencyHz = 400.0, pulseOnMs = 300, pulseOffMs = 300)
        "person" -> ToneSpec(frequencyHz = 600.0, pulseOnMs = 120, pulseOffMs = 120)
        else -> null
    }

    /** Continuously (re-)plays short generated tone buffers until the coroutine is cancelled. */
    private suspend fun playLoop(spec: ToneSpec) {
        val toneBuffer = generateSineWave(spec.frequencyHz, spec.pulseOnMs)
        val track = buildAudioTrack(toneBuffer.size)
        try {
            track.play()
            while (kotlinx.coroutines.currentCoroutineContext().isActive) {
                track.write(toneBuffer, 0, toneBuffer.size)
                if (spec.pulseOffMs > 0) {
                    track.stop()
                    kotlinx.coroutines.delay(spec.pulseOffMs)
                    track.play() // STREAM mode: play() alone resumes playback, no reload needed.
                }
            }
        } finally {
            try {
                track.stop()
            } catch (_: IllegalStateException) {
                // Track may already be stopped/released; safe to ignore during teardown.
            }
            track.release()
        }
    }

    private fun buildAudioTrack(bufferSizeInFrames: Int): AudioTrack {
        val minBufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val bufferBytes = maxOf(minBufferSize, bufferSizeInFrames * 2)

        return AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(bufferBytes)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
    }

    /** Generates a 16-bit PCM sine wave buffer of [durationMs] at [frequencyHz]. */
    private fun generateSineWave(frequencyHz: Double, durationMs: Long): ShortArray {
        val sampleCount = (sampleRate * (durationMs / 1000.0)).toInt().coerceAtLeast(1)
        val buffer = ShortArray(sampleCount)
        val angularStep = 2.0 * Math.PI * frequencyHz / sampleRate
        for (i in 0 until sampleCount) {
            // Simple amplitude envelope fade in/out to avoid audible clicks at buffer edges.
            val fadeSamples = (sampleRate * 0.01).toInt().coerceAtLeast(1) // 10ms fade
            val envelope = when {
                i < fadeSamples -> i.toDouble() / fadeSamples
                i > sampleCount - fadeSamples -> (sampleCount - i).toDouble() / fadeSamples
                else -> 1.0
            }
            val sampleValue = (sin(angularStep * i) * envelope * Short.MAX_VALUE * 0.6).toInt()
            buffer[i] = sampleValue.toShort()
        }
        return buffer
    }
}
