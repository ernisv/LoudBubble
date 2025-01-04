package com.ev.loudbubble

import android.content.Intent
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min

class MainActivity : AppCompatActivity() {

    private lateinit var horizontalProgressBar: ProgressBar
    private lateinit var verticalProgressBar: ProgressBar
    private lateinit var debugText: TextView
    private lateinit var bubbleManager: BubbleManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        horizontalProgressBar = findViewById(R.id.horizontal_progress_bar)

        verticalProgressBar = findViewById(R.id.vertical_progress_bar)
        debugText = findViewById(R.id.debugText)

        bubbleManager = BubbleManager(
            getSystemService(SENSOR_SERVICE) as SensorManager,
            getSharedPreferences("bubblePrefs", MODE_PRIVATE)
        )
        bubbleManager.bubbleStateListener = CompositeBubbleLevelListener(
            listOf(
                UIProgressBarsLevelFeedback(),
                musicalFeedback
            )
        )::onBubbleLevelChanged

        bubbleManager.init()
    }

    override fun onPause() {
        super.onPause()
        bubbleManager.pause()
        musicalFeedback.stop()
    }

    override fun onResume() {
        super.onResume()
        bubbleManager.resume()
        musicalFeedback.start()

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)

        val distortionHzPerDegree = sharedPreferences.getInt(getString(R.string.distortion_hz), 15)
        musicalFeedback.distortionHzPerDegree = distortionHzPerDegree
    }

    inner class UIProgressBarsLevelFeedback : BubbleLevelListener {
        private val progressBarDegreesLimit = 15.0

        private fun scaleProgressBar(degrees: Double): Int {
            return ((max(-progressBarDegreesLimit, min(progressBarDegreesLimit, -degrees))
                    + progressBarDegreesLimit) / (progressBarDegreesLimit*2) * 100).toInt()
        }

        override fun onBubbleLevelChanged(pitch: Float, roll: Float) {
            runOnUiThread {
                debugText.text = String.format("pitch: %.2f, roll: %.2f", pitch, roll)
                verticalProgressBar.setProgress(scaleProgressBar(pitch.toDouble()), true)
                horizontalProgressBar.setProgress(scaleProgressBar(roll.toDouble()), true)
            }
        }

    }

    inner class MusicalPitchLevelFeedback : BubbleLevelListener {

        private val player = TonePlayer.createBackgroundPlayer()

        val MaxDistortionHz = 40

        var distortionHzPerDegree = 15

        fun start() {
            player.start()
        }

        fun stop() {
            player.stop()
        }

        override fun onBubbleLevelChanged(pitch: Float, roll: Float) {
            suspend fun playDistortedTone(perfectFreq: Float, distortionDegrees: Float) {

                // play distorted tone
                val distortedTone =
                    min(
                        perfectFreq + MaxDistortionHz,
                        max(
                            perfectFreq - MaxDistortionHz,
                            (perfectFreq + distortionHzPerDegree * distortionDegrees).toFloat()
                        )
                    )


                player.queue(
                    TonePlayer.MultiPlayable(
                        *arrayOf(
                            // play perfect note
                            TonePlayer.Tone(perfectFreq, 500),
                            // play distorted tone
                            TonePlayer.Tone(distortedTone, 500)
                        )
                    )
                )
                // silence
                player.queue(TonePlayer.Silence(200))
            }

            if (!player.isIdle()) return

            lifecycleScope.launch {
                playDistortedTone(TonePlayer.Note.G(1).frequency(), pitch)
                playDistortedTone(TonePlayer.Note.B(1).frequency(), roll)
            }
        }
    }

    val musicalFeedback = MusicalPitchLevelFeedback()

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_zero -> {
                bubbleManager.zero()
                Toast.makeText(this, "Zeroed", Toast.LENGTH_SHORT).show()
                true
            }

            R.id.action_settings -> {
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
                true
            }

            R.id.action_about -> {
                val intent = Intent(this, AboutActivity::class.java)
                startActivity(intent)
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }
}