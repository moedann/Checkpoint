package com.example.nanisini

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.hardware.input.InputManager
import android.media.AudioManager
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import java.util.concurrent.atomic.AtomicLong
import kotlin.concurrent.timer
import android.os.Handler
import android.os.Looper

class PermissionChecker(private val context: Context) {
    fun isAccessibilityEnabled(serviceClassName: String): Boolean {
        val enabled = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        return isServiceListed(enabled, context.packageName, serviceClassName)
    }

    companion object {
        internal fun isServiceListed(
            enabledServices: String?,
            packageName: String,
            serviceClassName: String,
        ): Boolean = enabledServices
            ?.split(':')
            ?.any { flattenedName ->
                val separator = flattenedName.indexOf('/')
                if (separator < 1 || separator == flattenedName.lastIndex) return@any false

                val listedPackage = flattenedName.substring(0, separator)
                val listedClass = flattenedName.substring(separator + 1).let { className ->
                    if (className.startsWith('.')) listedPackage + className else className
                }
                listedPackage.equals(packageName, ignoreCase = true) &&
                    listedClass.equals(serviceClassName, ignoreCase = true)
            } == true
    }

    fun canDrawOverlays(): Boolean = Settings.canDrawOverlays(context)

    fun snapshot(
        serviceRunning: Boolean,
        lastDetectedApp: String,
        exclusionStatus: String,
        serviceClassName: String,
    ): PermissionSnapshot {
        val timerNotifications = TimerNotificationController(context)
        return PermissionSnapshot(
            accessibilityEnabled = isAccessibilityEnabled(serviceClassName),
            overlayEnabled = canDrawOverlays(),
            serviceRunning = serviceRunning,
            lastDetectedApp = lastDetectedApp,
            exclusionStatus = exclusionStatus,
            timerNotificationsEnabled = timerNotifications.notificationsEnabled(),
            liveUpdateStatus = timerNotifications.liveUpdateStatus().name,
        )
    }
}

class SystemStateResolver(private val context: Context) {
    fun snapshot(): SystemStateSnapshot {
        val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as android.app.KeyguardManager
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        return SystemStateSnapshot(
            isKeyguardLocked = keyguardManager.isKeyguardLocked,
            isScreenOn = if (android.os.Build.VERSION.SDK_INT >= 20) powerManager.isInteractive else powerManager.isScreenOn,
            isCallActive = audioManager.mode == AudioManager.MODE_IN_CALL || audioManager.mode == AudioManager.MODE_IN_COMMUNICATION,
            isImeForeground = false,
        )
    }
}

data class AppDisplayInfo(
    val packageName: String,
    val label: String,
    val icon: Drawable?,
)

class AppInfoResolver(private val context: Context) {
    private val cache = mutableMapOf<String, AppDisplayInfo>()

    fun resolve(packageName: String?): AppDisplayInfo {
        if (packageName.isNullOrBlank()) {
            return AppDisplayInfo("", "未設定", null)
        }
        return cache.getOrPut(packageName) { load(packageName) }
    }

    private fun load(packageName: String): AppDisplayInfo {
        val packageManager = context.packageManager
        return try {
            val applicationInfo = packageManager.getApplicationInfo(packageName, 0)
            val launcherActivity = packageManager.getLaunchIntentForPackage(packageName)
                ?.resolveActivity(packageManager)
                ?.let { component -> runCatching { packageManager.getActivityInfo(component, 0) }.getOrNull() }
            val label = launcherActivity?.loadLabel(packageManager)?.toString()
                .orEmpty()
                .ifBlank { packageManager.getApplicationLabel(applicationInfo).toString() }
                .ifBlank { packageName }
            // Activity icons retain adaptive/vector source quality and are scaled by
            // ImageView at render time instead of pre-rendering a low-resolution bitmap.
            val icon = launcherActivity?.loadIcon(packageManager) ?: packageManager.getApplicationIcon(applicationInfo)
            AppDisplayInfo(packageName, label, icon)
        } catch (_: Exception) {
            AppDisplayInfo(packageName, packageName, null)
        }
    }
}

class ForegroundAppDebouncer(
    private val handler: Handler = Handler(Looper.getMainLooper()),
    private val delayMillis: Long = 200L,
) {
    private var pending: ForegroundCandidate? = null
    private var runnable: Runnable? = null

    fun submit(candidate: ForegroundCandidate, onStable: (ForegroundCandidate) -> Unit) {
        if (candidate.packageName.isNullOrBlank()) return
        // Replace the pending candidate and wait for a stable window. Transient
        // launcher, recents, permission and animation packages must not mutate a session.
        pending = candidate
        val currentRunnable = runnable
        if (currentRunnable != null) {
            handler.removeCallbacks(currentRunnable)
        }
        val next = Runnable {
            val stable = pending
            pending = null
            runnable = null
            stable?.let(onStable)
        }
        runnable = next
        handler.postDelayed(next, delayMillis)
    }

    fun clear() {
        runnable?.let { handler.removeCallbacks(it) }
        runnable = null
        pending = null
    }
}

class AndroidPackageIntelligence(private val context: Context) {
    fun homePackages(): Set<String> {
        val packages = linkedSetOf<String>()
        val packageManager = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        packageManager.queryIntentActivities(intent, 0).forEach { packages.add(it.activityInfo.packageName) }
        return packages
    }

    fun settingsPackages(): Set<String> = setOf("com.android.settings")

    fun systemUiPackages(): Set<String> = setOf(
        "com.android.systemui",
        "com.google.android.photopicker",
    )

    fun cameraPackages(): Set<String> {
        val intent = Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE)
        return context.packageManager.queryIntentActivities(intent, 0).map { it.activityInfo.packageName }.toSet()
    }

    fun calculatorPackages(): Set<String> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_CALCULATOR)
        return context.packageManager.queryIntentActivities(intent, 0).map { it.activityInfo.packageName }.toSet()
    }

    fun dialerPackages(): Set<String> {
        val intent = Intent(Intent.ACTION_DIAL)
        return context.packageManager.queryIntentActivities(intent, 0).map { it.activityInfo.packageName }.toSet()
    }

    fun alarmPackages(): Set<String> {
        val intent = Intent("android.intent.action.SET_ALARM")
        return context.packageManager.queryIntentActivities(intent, 0).map { it.activityInfo.packageName }.toSet()
    }

    fun defaultInputMethodPackage(): String? {
        val flattened = Settings.Secure.getString(context.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD) ?: return null
        return flattened.substringBefore('/').ifBlank { null }
    }

    fun permissionControllerPackage(): String? {
        val actions = listOf(
            "android.content.pm.action.REQUEST_PERMISSIONS",
            "android.intent.action.MANAGE_PERMISSIONS",
            "android.intent.action.REVIEW_PERMISSIONS",
        )
        return actions.firstNotNullOfOrNull { action ->
            context.packageManager.resolveActivity(Intent(action), 0)?.activityInfo?.packageName
        }
    }

    fun defaultBrowserPackage(): String? {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.example.com"))
            .addCategory(Intent.CATEGORY_BROWSABLE)
        val resolved = context.packageManager
            .resolveActivity(intent, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY)
            ?.activityInfo ?: return null
        // With no user-selected default, Android can return its chooser activity.
        // Never treat that system component as a browser exclusion.
        return resolved.packageName.takeUnless {
            it == "android" || resolved.name.contains("ResolverActivity", ignoreCase = true)
        }
    }

    fun globalSearchPackage(): String? =
        (context.getSystemService(Context.SEARCH_SERVICE) as? android.app.SearchManager)
            ?.globalSearchActivity
            ?.packageName

    fun defaultExcludedPackages(): Set<String> = buildSet {
        add("com.google.android.googlequicksearchbox")
        add("com.android.vending")
        globalSearchPackage()?.let(::add)
    }

    fun installerPackages(): Set<String> = listOf(Intent.ACTION_INSTALL_PACKAGE, Intent.ACTION_VIEW).flatMap { action ->
        val intent = Intent(action).apply { if (action == Intent.ACTION_VIEW) { setDataAndType(Uri.parse("content://package.apk"), "application/vnd.android.package-archive") } }
        context.packageManager.queryIntentActivities(intent, 0).map { it.activityInfo.packageName }
    }.toSet()

    fun dialerRolePackage(): String? = (context.getSystemService(Context.TELECOM_SERVICE) as? android.telecom.TelecomManager)?.defaultDialerPackage
}

/** Temporarily asks media apps to pause while a blocking modal is on screen. */
class ModalAudioFocusController(context: Context) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val listener = AudioManager.OnAudioFocusChangeListener { }
    private val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .setOnAudioFocusChangeListener(listener)
        .setWillPauseWhenDucked(true)
        .build()
    private var held = false

    fun acquire() {
        if (!held) held = audioManager.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    fun release() {
        if (held) audioManager.abandonAudioFocusRequest(request)
        held = false
    }
}
