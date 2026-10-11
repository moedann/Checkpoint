package com.example.nanisini

data class TargetApp(
    val packageName: String,
    val displayName: String,
    val defaultEnabled: Boolean = true,
)

object SupportedTargetApps {
    val all: List<TargetApp> = listOf(
        TargetApp("com.instagram.android", "Instagram"),
        TargetApp("com.twitter.android", "X"),
        TargetApp("com.zhiliaoapp.musically", "TikTok"),
        TargetApp("com.instagram.barcelona", "Threads"),
        TargetApp("com.facebook.katana", "Facebook"),
        TargetApp("com.google.android.youtube", "YouTube"),
    )

    val packageNames: Set<String> = all.mapTo(linkedSetOf()) { it.packageName }
    val defaultEnabledPackageNames: Set<String> = all
        .filter { it.defaultEnabled }
        .mapTo(linkedSetOf()) { it.packageName }
}

class TargetAppResolver(private val repository: NativeSettingsRepository) {
    fun isTargetApp(packageName: String): Boolean =
        packageName in SupportedTargetApps.packageNames &&
            packageName in repository.getEnabledTargetPackages()
}
