package com.example.nanisini

import android.os.Handler
import android.os.Looper

/** One central wake-up for the nearest deadline; no per-session polling loops. */
class DeadlineScheduler(
    private val clock: ElapsedClock,
    private val nearestDeadline: () -> Long?,
    private val onDeadline: () -> Unit,
    private val handler: Handler = Handler(Looper.getMainLooper()),
) {
    private val wake = Runnable { onDeadline(); schedule() }
    fun schedule() {
        handler.removeCallbacks(wake)
        val deadline = nearestDeadline() ?: return
        handler.postDelayed(wake, (deadline - clock.now()).coerceAtLeast(0L))
    }
    fun stop() = handler.removeCallbacks(wake)
}
