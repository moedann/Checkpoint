package com.example.nanisini

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Opens native checkpoint UI directly without launching the Flutter activity. */
class NotificationTapReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (!intent.getBooleanExtra(TimerNotificationController.EXTRA_RUNNING_CHECK, false)) return
        val sessionId = intent.getStringExtra(TimerNotificationController.EXTRA_SESSION_ID) ?: return
        AppDetectionAccessibilityService.current()?.showRunningSessionCheck(sessionId)
    }
}
