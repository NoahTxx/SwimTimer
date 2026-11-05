package com.example.util

import android.os.SystemClock
import androidx.compose.runtime.Immutable

@Immutable
fun interface MonotonicClock {
    fun nowMillis(): Long
}

@Immutable
class AndroidMonotonicClock : MonotonicClock {
    override fun nowMillis(): Long = SystemClock.elapsedRealtime()
}

@Immutable
class FakeClock(var currentMillis: Long = 0L) : MonotonicClock {
    override fun nowMillis(): Long = currentMillis

    fun advance(millis: Long) {
        currentMillis += millis
    }
}
