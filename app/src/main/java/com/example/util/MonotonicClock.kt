package com.example.util

import android.os.SystemClock

fun interface MonotonicClock {
    fun nowMillis(): Long
}

class AndroidMonotonicClock : MonotonicClock {
    override fun nowMillis(): Long = SystemClock.elapsedRealtime()
}

class FakeClock(var currentMillis: Long = 0L) : MonotonicClock {
    override fun nowMillis(): Long = currentMillis

    fun advance(millis: Long) {
        currentMillis += millis
    }
}
