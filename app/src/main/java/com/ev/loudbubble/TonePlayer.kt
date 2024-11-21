package com.ev.loudbubble

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runBlocking
import kotlin.math.pow


object TonePlayer {
    private val TAG = "TonePlayer"

    sealed interface Playable {
        fun frequency(): Float
        fun durationMs(): Int
    }

    class Silence(private val durationMs: Int) : Playable {
        override fun frequency(): Float = 0.0f
        override fun durationMs(): Int = durationMs
    }

    class Tone(private val frequency: Float, private val durationMs: Int) : Playable {
        override fun frequency(): Float = frequency
        override fun durationMs(): Int = durationMs
    }

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
        private val channel = Channel<Playable>(Channel.UNLIMITED)
        private var keepRunning = true

        suspend fun queue(playable: Playable) {
            Log.d(TAG, "Queued playable")
            channel.send(playable)
        }

        fun isEmpty(): Boolean {
            return channel.isEmpty
        }

        fun start() {
            keepRunning = true

            Thread {
                runBlocking {
                    launch {
                        while (keepRunning) {
                            Log.d(TAG, "Background spinning")
                            val playable = channel.receive()
                            Log.d(TAG, "Received playable from channel")
                            when (playable) {
                                is Silence -> {
                                    Log.d(TAG, "Silence for ${playable.durationMs()}ms")
                                    delay(playable.durationMs().toLong())
                                }
                                else -> {
                                    val state = play(playable)
                                    while (state.isPlaying()) {
                                        delay(1)
                                    }
                                    state.cleanup()
                                }
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

    fun play(playable: Playable): PlaybackState {
        Log.d(TAG, "Playing playable")

        val sampleRate = 44100 // Standard sample rate for audio
        val numSamples =
            (playable.durationMs() * sampleRate / 1000) // Number of samples for the duration
        val buffer = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            buffer[i] =
                (Short.MAX_VALUE * Math.sin(2 * Math.PI * i / (sampleRate / playable.frequency()))).toInt()
                    .toShort()
        }

        val audioTrack = AudioTrack(
            AudioManager.STREAM_MUSIC,
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            buffer.size * Short.SIZE_BYTES,
            AudioTrack.MODE_STATIC
        )
        audioTrack.setVolume(AudioTrack.getMaxVolume())

        audioTrack.write(buffer, 0, buffer.size)
        audioTrack.play()
        return PlaybackState(audioTrack)

    }
}