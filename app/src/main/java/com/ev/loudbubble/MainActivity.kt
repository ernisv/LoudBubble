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
        bubbleManager.bubleStateListener = this::bubbleStateListener

        bubbleManager.init()
    }

    override fun onPause() {
        super.onPause()
        bubbleManager.pause()
    }

    override fun onResume() {
        super.onResume()
        bubbleManager.resume()
    }

    private fun bubbleStateListener(pitch: Float, roll: Float) {
        runOnUiThread {
            debugText.text = String.format("pitch: %.2f, roll: %.2f", pitch, roll)
            verticalProgressBar.setProgress(((pitch + 90) / 180 * 100).toInt(), true)
            horizontalProgressBar.setProgress(((roll + 90) / 180 * 100).toInt(), true)
        }
    }

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