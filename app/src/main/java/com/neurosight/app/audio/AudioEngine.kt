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

/** Generates distinct accessibility audio cues for each detected class. */
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

    fun startForClass(className: String) {
        if (className == currentClass) return
        stopInternal()
        currentClass = className
        val spec = specFor(className) ?: return
        playJob = scope.launch { playLoop(spec) }
    }

    fun stop() {
        currentClass = null
        stopInternal()
    }

    private fun stopInternal() {
        playJob?.cancel()
        playJob = null
    }

    /** Five classes intentionally use separated pitch/rhythm signatures. */
    private fun specFor(className: String): ToneSpec? = when (className) {
        "door" -> ToneSpec(400.0, 300, 300)
        "window" -> ToneSpec(520.0, 180, 180)
        "chair" -> ToneSpec(300.0, 220, 420)
        "table" -> ToneSpec(650.0, 120, 480)
        "cabinet" -> ToneSpec(250.0, 500, 250)
        else -> null
    }

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
                    track.play()
                }
            }
        } finally {
            try { track.stop() } catch (_: IllegalStateException) { }
            track.release()
        }
    }

    private fun buildAudioTrack(bufferSizeInFrames: Int): AudioTrack {
        val minBufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
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
            .setBufferSizeInBytes(maxOf(minBufferSize, bufferSizeInFrames * 2))
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
    }

    private fun generateSineWave(frequencyHz: Double, durationMs: Long): ShortArray {
        val sampleCount = (sampleRate * (durationMs / 1000.0)).toInt().coerceAtLeast(1)
        val buffer = ShortArray(sampleCount)
        val angularStep = 2.0 * Math.PI * frequencyHz / sampleRate
        val fadeSamples = (sampleRate * 0.01).toInt().coerceAtLeast(1)
        for (i in 0 until sampleCount) {
            val envelope = when {
                i < fadeSamples -> i.toDouble() / fadeSamples
                i > sampleCount - fadeSamples -> (sampleCount - i).toDouble() / fadeSamples
                else -> 1.0
            }
            buffer[i] = (sin(angularStep * i) * envelope * Short.MAX_VALUE * 0.6).toInt().toShort()
        }
        return buffer
    }
}
