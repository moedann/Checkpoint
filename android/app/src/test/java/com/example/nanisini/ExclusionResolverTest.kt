package com.example.nanisini

import org.junit.Assert.assertEquals
import org.junit.Test

class ExclusionResolverTest {
    private val resolver = ExclusionResolver(
        selfPackageName = "com.example.nanisini",
        homePackages = setOf("com.android.launcher"),
        settingsPackages = setOf("com.android.settings"),
        cameraPackages = setOf("com.android.camera"),
        calculatorPackages = setOf("com.android.calculator"),
        dialerPackages = setOf("com.android.dialer"),
        alarmPackages = setOf("com.android.deskclock"),
        systemUiPackages = setOf(
            "com.android.systemui",
            "com.google.android.photopicker",
        ),
        permissionControllerPackage = "com.android.permissioncontroller",
        defaultInputMethodPackage = "com.android.ime",
        alwaysExcludedPackages = setOf(
            "com.google.android.googlequicksearchbox",
            "com.android.chrome",
            "com.android.vending",
            "com.vendor.browser",
        ),
    )

    @Test
    fun excludesSelfLauncherSettingsCameraCalculatorDialerImeAndNull() {
        assertEquals("self", resolver.resolve(ForegroundCandidate("com.example.nanisini", null, 0, 0, 0), system()).reason)
        assertEquals("launcher", resolver.resolve(ForegroundCandidate("com.android.launcher", null, 0, 0, 0), system()).reason)
        assertEquals("settings", resolver.resolve(ForegroundCandidate("com.android.settings", null, 0, 0, 0), system()).reason)
        assertEquals("camera", resolver.resolve(ForegroundCandidate("com.android.camera", null, 0, 0, 0), system()).reason)
        assertEquals("calculator", resolver.resolve(ForegroundCandidate("com.android.calculator", null, 0, 0, 0), system()).reason)
        assertEquals("dialer", resolver.resolve(ForegroundCandidate("com.android.dialer", null, 0, 0, 0), system()).reason)
        assertEquals("ime", resolver.resolve(ForegroundCandidate("com.android.ime", null, 0, 0, 0), system()).reason)
        assertEquals("null package", resolver.resolve(ForegroundCandidate(null, null, 0, 0, 0), system()).reason)
    }

    @Test
    fun excludesGooglePhotoPickerAsSystemUi() {
        val decision = resolver.resolve(
            ForegroundCandidate("com.google.android.photopicker", null, 0, 0, 0),
            system(),
        )

        assertEquals(true, decision.excluded)
        assertEquals("system ui", decision.reason)
    }

    @Test
    fun normalAppIsNotExcluded() {
        val decision = resolver.resolve(ForegroundCandidate("com.instagram.android", "MainActivity", 0, 0, 0), system())
        assertEquals(false, decision.excluded)
        assertEquals("normal app", decision.reason)
    }

    @Test
    fun excludesGoogleChromePlayStoreAndDetectedDefaultBrowser() {
        listOf(
            "com.google.android.googlequicksearchbox",
            "com.android.chrome",
            "com.android.vending",
            "com.vendor.browser",
        ).forEach { packageName ->
            assertEquals(
                "default excluded app",
                resolver.resolve(ForegroundCandidate(packageName, null, 0, 0, 0), system()).reason,
            )
        }
    }

    private fun system() = SystemStateSnapshot(false, true, false, false)
}
