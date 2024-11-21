package com.ev.loudbubble

interface BubbleLevelListener {
    fun onBubbleLevelChanged(pitch: Float, roll: Float)
}

class CompositeBubbleLevelListener(private val listeners: List<BubbleLevelListener>) : BubbleLevelListener {
    override fun onBubbleLevelChanged(pitch: Float, roll: Float) {
        listeners.forEach {
            it.onBubbleLevelChanged(pitch, roll)
        }
    }
}