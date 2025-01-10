package com.ev.loudbubble

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.util.Log
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin


object TonePlayer {
    private val TAG = "TonePlayer"

    sealed interface Queueable

    sealed interface Playable : Queueable {
        fun frequency(): Float
        fun durationMs(): Int
    }

    class Silence(private val durationMs: Int) : Queueable {
        fun durationMs(): Int = durationMs
    }

    class Tone(private val frequency: Float, private val durationMs: Int) : Playable {
        override fun frequency(): Float = frequency
        override fun durationMs(): Int = durationMs
    }

    class MultiPlayable(vararg val playables: Playable) : Queueable

    object Note {

        fun C(durationMs: Int) = NoteTone(Tone(261.63f, durationMs))
        fun Csharp(durationMs: Int) = NoteTone(Tone(277.18f, durationMs))
        fun Db(durationMs: Int) = Csharp(durationMs)
        fun D(durationMs: Int) = NoteTone(Tone(293.66f, durationMs))
        fun Dsharp(durationMs: Int) = NoteTone(Tone(311.13f, durationMs))
        fun Eb(durationMs: Int) = Dsharp(durationMs)
        fun E(durationMs: Int) = NoteTone(Tone(329.63f, durationMs))
        fun F(durationMs: Int) = NoteTone(Tone(349.23f, durationMs))
        fun Fsharp(durationMs: Int) = NoteTone(Tone(369.99f, durationMs))
        fun Gb(durationMs: Int) = Fsharp(durationMs)
        fun G(durationMs: Int) = NoteTone(Tone(392.00f, durationMs))
        fun Gsharp(durationMs: Int) = NoteTone(Tone(415.30f, durationMs))
        fun Ab(durationMs: Int) = Gsharp(durationMs)
        fun A(durationMs: Int) = NoteTone(Tone(440.00f, durationMs))
        fun Asharp(durationMs: Int) = NoteTone(Tone(466.16f, durationMs))
        fun Bb(durationMs: Int) = Asharp(durationMs)
        fun B(durationMs: Int) = NoteTone(Tone(493.88f, durationMs))

        class NoteTone(tone: Tone) : Playable by tone {
            fun lowerOctaves(octaveCount: Int): NoteTone {
                return NoteTone(
                    Tone(
                        frequency() - 2.0.pow(octaveCount).toFloat(),
                        durationMs()
                    )
                )
            }

            fun higherOctaves(octaveCount: Int): NoteTone {
                return NoteTone(
                    Tone(
                        frequency() + 2.0.pow(octaveCount).toFloat(),
                        durationMs()
                    )
                )
            }
        }
    }

    class BackgroundPlayer {
        private val channel = Channel<Queueable>(Channel.UNLIMITED)
        private var keepRunning = true
        private var executing = false

        suspend fun queue(playable: Queueable) {
            Log.d(TAG, "Queued playable")
            channel.send(playable)
        }

        @OptIn(ExperimentalCoroutinesApi::class)
        fun isIdle(): Boolean {
            return channel.isEmpty && !executing
        }

        fun start() {
            keepRunning = true

            suspend fun playCompletely(state: PlaybackState) {
                while (state.isPlaying()) {
                    delay(1)
                }
                state.cleanup()
                executing = false
            }

            Thread {
                runBlocking {
                    launch {
                        while (keepRunning) {
                            Log.d(TAG, "Background spinning")
                            val queueable = channel.receive()
                            executing = true
                            Log.d(TAG, "Received playable from channel")
                            when (queueable) {
                                is MultiPlayable ->
                                    playCompletely(play(*queueable.playables))

                                is Silence -> {
                                    Log.d(TAG, "Silence for ${queueable.durationMs()}ms")
                                    delay(queueable.durationMs().toLong())
                                    executing = false
                                }

                                is Playable -> playCompletely(play(queueable))

                            }
                        }
                    }
                }
            }.start()
        }

        fun stop() {
            keepRunning = false
        }
    }

    fun createBackgroundPlayer(): BackgroundPlayer {
        return BackgroundPlayer()
    }

    class PlaybackState(private val audioTrack: AudioTrack) {
        fun isPlaying(): Boolean {
            return audioTrack.playbackHeadPosition < audioTrack.bufferSizeInFrames
        }

        fun cleanup() {
            audioTrack.stop()
            audioTrack.release()
        }

    }

    fun play(vararg playables: Playable): PlaybackState {
        Log.d(TAG, "Playing playable")

        val sampleRate = 44100 // Standard sample rate for audio
        val fadeSamples = sampleRate / 10
        val totalDuration = playables.sumOf { it.durationMs() }
        val numSamples =
            (totalDuration * sampleRate / 1000) // Number of samples for the duration
        val buffer = ShortArray(numSamples)
        val playableBoundarySamples = playables
            .map { it.durationMs() * sampleRate / 1000 }
            .fold(mutableListOf<Int>(), { acc, i -> acc.add((acc.lastOrNull() ?: 0) + i); acc })

        fun freqBySample(sampleNum: Int): Float {
            var samplesPassed = 0
            for (playable in playables) {
                if (samplesPassed + playable.durationMs() * sampleRate / 1000 > sampleNum)
                    return playable.frequency()
                samplesPassed += playable.durationMs() * sampleRate / 1000
            }
            return playables.last().frequency()
        }

        for (i in 0 until numSamples) {
            val frequency = freqBySample(i)

            val fadeCoef = min(
                fadeSamples,
                playableBoundarySamples.minOfOrNull { abs(it - i) } ?: 0)
                .toFloat() / fadeSamples

            buffer[i] =
                (fadeCoef * Short.MAX_VALUE * sin(2 * Math.PI * i / (sampleRate / frequency))).toInt()
                    .toShort()
        }

        val audioTrack = AudioTrack(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                .build(),
            AudioFormat.Builder()
                .setSampleRate(sampleRate)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build(),
            buffer.size * Short.SIZE_BYTES,
            AudioTrack.MODE_STATIC,
            AudioManager.AUDIO_SESSION_ID_GENERATE
        )

        audioTrack.setVolume(AudioTrack.getMaxVolume())

        audioTrack.write(buffer, 0, buffer.size)
        audioTrack.play()
        return PlaybackState(audioTrack)

    }
}