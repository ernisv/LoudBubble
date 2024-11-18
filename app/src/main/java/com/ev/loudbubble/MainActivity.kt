package com.ev.loudbubble

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

    private lateinit var horizontalProgressBar: ProgressBar
    private lateinit var verticalProgressBar: ProgressBar

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

        lifecycleScope.launch {
            while (isActive) {

                val horizValue = Random.nextInt(-10, 10)
                val vertValue = Random.nextInt(-10, 10)

                horizontalProgressBar.setProgress(50 + horizValue, true)
                verticalProgressBar.setProgress(50 + vertValue, true)

                horizontalProgressBar.setBackgroundColor(
                    if (Math.abs(horizValue) < 2) Color.GREEN else Color.RED)
                verticalProgressBar.setBackgroundColor(
                    if (Math.abs(vertValue) < 2) Color.GREEN else Color.RED)

                delay(1000)
            }
        }
    }



    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }
}