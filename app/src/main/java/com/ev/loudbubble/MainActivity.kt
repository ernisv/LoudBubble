package com.ev.loudbubble

import android.content.Intent
import android.hardware.SensorManager
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

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
    }

    inner class UIProgressBarsLevelFeedback : BubbleLevelListener {
        override fun onBubbleLevelChanged(pitch: Float, roll: Float) {
            runOnUiThread {
                debugText.text = String.format("pitch: %.2f, roll: %.2f", pitch, roll)
                verticalProgressBar.setProgress(((pitch + 90) / 180 * 100).toInt(), true)
                horizontalProgressBar.setProgress(((-roll + 90) / 180 * 100).toInt(), true)
            }
        }

    }

    inner class MusicalPitchLevelFeedback : BubbleLevelListener {

        private val player = TonePlayer.createBackgroundPlayer()

        fun start() {
            player.start()
        }

        fun stop() {
            player.stop()
        }

        override fun onBubbleLevelChanged(pitch: Float, roll: Float) {
            if (!player.isEmpty()) return
            runOnUiThread {
                lifecycleScope.launch {
                    if (pitch > 20) {
                        player.queue(TonePlayer.Note.C(1000))
                        player.queue(TonePlayer.Silence(1000))
                    } else {
                        player.queue(TonePlayer.Note.D(1000))
                        player.queue(TonePlayer.Silence(1000))
                    }
                }
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

            else -> super.onOptionsItemSelected(item)
        }
    }
}