package com.example.nanisini

import android.os.SystemClock

interface ElapsedClock {
    fun now(): Long
}

object SystemElapsedClock : ElapsedClock {
    override fun now(): Long = SystemClock.elapsedRealtime()
}
