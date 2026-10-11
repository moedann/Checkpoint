package com.example.nanisini

class ExclusionResolver(
    private val selfPackageName: String,
    private val homePackages: Set<String>,
    private val settingsPackages: Set<String>,
    private val cameraPackages: Set<String>,
    private val calculatorPackages: Set<String>,
    private val dialerPackages: Set<String>,
    private val alarmPackages: Set<String>,
    private val systemUiPackages: Set<String>,
    private val permissionControllerPackage: String?,
    private val defaultInputMethodPackage: String?,
    private val installerPackages: Set<String> = emptySet(),
    private val alwaysExcludedPackages: Set<String> = emptySet(),
) {
    fun resolve(candidate: ForegroundCandidate, systemState: SystemStateSnapshot): ExclusionDecision {
        val packageName = candidate.packageName
        if (packageName.isNullOrBlank()) {
            return ExclusionDecision(true, "null package")
        }
        if (systemState.isKeyguardLocked) {
            return ExclusionDecision(true, "lock screen")
        }
        if (!systemState.isScreenOn) {
            return ExclusionDecision(true, "screen off")
        }
        if (systemState.isCallActive) {
            return ExclusionDecision(true, "call state")
        }
        if (packageName == selfPackageName) {
            return ExclusionDecision(true, "self")
        }
        if (packageName in homePackages) {
            return ExclusionDecision(true, "launcher")
        }
        if (packageName in settingsPackages || candidate.className?.contains("Settings", ignoreCase = true) == true) {
            return ExclusionDecision(true, "settings")
        }
        if (packageName in cameraPackages || candidate.className?.contains("Camera", ignoreCase = true) == true) {
            return ExclusionDecision(true, "camera")
        }
        if (packageName in calculatorPackages) {
            return ExclusionDecision(true, "calculator")
        }
        if (packageName in dialerPackages || candidate.className?.contains("Dial", ignoreCase = true) == true) {
            return ExclusionDecision(true, "dialer")
        }
        if (packageName in alarmPackages || candidate.className?.contains("Alarm", ignoreCase = true) == true) {
            return ExclusionDecision(true, "alarm")
        }
        if (packageName in systemUiPackages) {
            return ExclusionDecision(true, "system ui")
        }
        if (packageName == permissionControllerPackage) {
            return ExclusionDecision(true, "permission controller")
        }
        if (packageName in installerPackages || candidate.className?.contains("PackageInstaller", ignoreCase = true) == true) {
            return ExclusionDecision(true, "installer")
        }
        if (packageName == defaultInputMethodPackage) {
            return ExclusionDecision(true, "ime")
        }
        if (packageName in alwaysExcludedPackages) {
            return ExclusionDecision(true, "default excluded app")
        }
        return ExclusionDecision(false, "normal app")
    }
}
