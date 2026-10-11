package com.example.nanisini

class TestClock(private var current: Long) : ElapsedClock {
    override fun now(): Long = current

    fun advance(millis: Long) {
        current += millis
    }
}
