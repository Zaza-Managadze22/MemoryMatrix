package edu.sdsu.cs646.memory_matrix

import android.os.SystemClock

class Timer(private val durationMillis: Long) {
    private var startTime: Long = 0

    init {
        startTime = SystemClock.uptimeMillis()
    }

    private val elapsedTime: Long
        get() {
            return SystemClock.uptimeMillis() - startTime
        }

    val finished: Boolean
        get() {
            return elapsedTime >= durationMillis
        }

    val progressPercent: Int
        get() {
            return if (finished) {
                1
            } else {
                (elapsedTime * 100 / durationMillis).toInt()
            }
        }
}