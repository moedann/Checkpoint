package com.example.nanisini

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import androidx.core.app.NotificationCompat

/** Presents RUNNING sessions. Reminder state and expiry remain owned by SessionManager. */
class TimerNotificationController(private val context: Context) {
    private val manager = context.getSystemService(NotificationManager::class.java)
    private val postedIds = linkedMapOf<String, Int>()
    private val postedExpirationIds = linkedMapOf<String, Int>()

    init { createChannel() }

    fun notificationsEnabled(): Boolean =
        manager.areNotificationsEnabled() &&
            (Build.VERSION.SDK_INT < 33 || context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)

    fun liveUpdateStatus(): LiveUpdateStatus = when {
        Build.VERSION.SDK_INT < 36 -> LiveUpdateStatus.UNAVAILABLE
        !notificationsEnabled() -> LiveUpdateStatus.USER_DISABLED
        manager.canPostPromotedNotifications() -> LiveUpdateStatus.AVAILABLE
        else -> LiveUpdateStatus.USER_DISABLED
    }

    /** Reconciles notifications from a session snapshot; no periodic reposting is used. */
    fun sync(sessions: Collection<ActiveSession>) {
        // Keep the notification while a RUNNING session is temporarily paused by
        // System UI, the keyguard, or another excluded window. Removing it here made
        // the timer appear to vanish when the notification shade was opened.
        val running = sessions.distinctBy { it.sessionId }.filter { it.timerState == TimerState.RUNNING }
            .sortedWith(compareBy<ActiveSession> { it.timerDeadlineElapsedRealtime }.thenBy { it.sessionId })
        val runningIds = running.mapTo(hashSetOf()) { it.sessionId }
        postedIds.keys.filterNot(runningIds::contains).toList().forEach(::cancelRunning)
        if (!notificationsEnabled()) return
        running.forEachIndexed { index, session -> post(session, requestPromotion = index == 0) }
    }

    fun cancel(sessionId: String) {
        val id = postedIds.remove(sessionId) ?: notificationIdFor(sessionId)
        manager.cancel(id)
        cancelExpiration(sessionId)
    }

    fun cancelRunning(sessionId: String) {
        val id = postedIds.remove(sessionId) ?: notificationIdFor(sessionId)
        manager.cancel(id)
    }

    /** Posts the one-shot alert; SessionManager remains the authority for expiry. */
    fun postExpiration(session: ActiveSession) {
        if (!notificationsEnabled() || postedExpirationIds.containsKey(session.sessionId)) return
        val id = expirationNotificationIdFor(session.sessionId)
        postedExpirationIds[session.sessionId] = id
        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?: Intent(context, MainActivity::class.java)
        launchIntent.putExtra(EXTRA_SESSION_ID, session.sessionId)
            .putExtra(EXTRA_PACKAGE_NAME, session.packageName)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pendingIntent = PendingIntent.getActivity(
            context, id, launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val purpose = session.purposeText?.takeIf { it.isNotBlank() } ?: "未設定の目的"
        val notification = NotificationCompat.Builder(context, EXPIRATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("チェックポイントに到着しました")
            .setContentText("『$purpose』に設定した時間になりました。")
            .setStyle(NotificationCompat.BigTextStyle().bigText("『$purpose』に設定した時間になりました。"))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()
        manager.notify(id, notification)
    }

    fun cancelExpiration(sessionId: String) {
        val id = postedExpirationIds.remove(sessionId) ?: expirationNotificationIdFor(sessionId)
        manager.cancel(id)
    }

    fun cancelAll() {
        (postedIds.keys + postedExpirationIds.keys).toSet().forEach(::cancel)
    }

    private fun post(session: ActiveSession, requestPromotion: Boolean) {
        val id = allocateNotificationId(session.sessionId)
        val remaining = remainingForNotification(session, SystemClock.elapsedRealtime())
        if (remaining <= 0L) { manager.cancel(id); return }
        val extras = Bundle().apply {
            putString(EXTRA_SESSION_ID, session.sessionId)
            putString(EXTRA_PACKAGE_NAME, session.packageName)
        }
        val overlayIntent = Intent(context, NotificationTapReceiver::class.java)
        overlayIntent.putExtra(EXTRA_SESSION_ID, session.sessionId)
            .putExtra(EXTRA_PACKAGE_NAME, session.packageName)
            .putExtra(EXTRA_RUNNING_CHECK, true)
        val pendingIntent = PendingIntent.getBroadcast(
            context, id, overlayIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val purpose = session.purposeText?.takeIf { it.isNotBlank() } ?: "未設定"
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("${session.applicationLabel}を利用中")
            .setContentText("目的：$purpose")
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setGroup(GROUP_KEY)
            .setExtras(extras)
        builder
            .setWhen(System.currentTimeMillis() + remaining)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
        builder.setRequestPromotedOngoing(Build.VERSION.SDK_INT >= 36 && requestPromotion)
        manager.notify(id, builder.build())
    }

    private fun allocateNotificationId(sessionId: String): Int {
        postedIds[sessionId]?.let { return it }
        var id = notificationIdFor(sessionId)
        val occupied = postedIds.values.toHashSet()
        while (id in occupied) id = if (id == Int.MAX_VALUE) ID_BASE else id + 1
        postedIds[sessionId] = id
        return id
    }

    private fun createChannel() {
        val channel = NotificationChannel(CHANNEL_ID, "利用中タイマー", NotificationManager.IMPORTANCE_LOW).apply {
            description = "利用中のアプリごとの残り時間"
            setSound(null, null)
            enableVibration(false)
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)
        manager.createNotificationChannel(
            NotificationChannel(EXPIRATION_CHANNEL_ID, "チェックポイント到着", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "設定した前景利用時間に到達したことを知らせます"
                setShowBadge(true)
            },
        )
    }

    companion object {
        const val CHANNEL_ID = "active_timers"
        const val EXPIRATION_CHANNEL_ID = "checkpoint_arrivals"
        const val GROUP_KEY = "nanishini_active_timers"
        const val EXTRA_SESSION_ID = "sessionId"
        const val EXTRA_PACKAGE_NAME = "packageName"
        const val EXTRA_RUNNING_CHECK = "openRunningCheck"
        private const val ID_BASE = 10_000

        fun notificationIdFor(sessionId: String): Int = ID_BASE + (sessionId.hashCode() and 0x3fffffff)
        fun expirationNotificationIdFor(sessionId: String): Int = Int.MIN_VALUE + (sessionId.hashCode() and 0x3fffffff)
        fun remainingForNotification(session: ActiveSession, nowElapsedRealtime: Long): Long =
            (session.timerDeadlineElapsedRealtime - nowElapsedRealtime).coerceAtLeast(0L)

        fun formatRemaining(remainingMillis: Long): String {
            val totalSeconds = (remainingMillis.coerceAtLeast(0L) + 999L) / 1_000L
            return "%d:%02d".format(totalSeconds / 60L, totalSeconds % 60L)
        }
    }
}

enum class LiveUpdateStatus { AVAILABLE, UNAVAILABLE, USER_DISABLED }
