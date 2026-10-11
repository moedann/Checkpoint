package com.example.nanisini

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TargetAppResolverTest {
    @Test
    fun supportedAppsAreAnExplicitSixPackageAllowList() {
        assertEquals(6, SupportedTargetApps.all.size)
        assertEquals(SupportedTargetApps.packageNames, SupportedTargetApps.defaultEnabledPackageNames)
        assertTrue("com.instagram.android" in SupportedTargetApps.packageNames)
        assertTrue("com.google.android.youtube" in SupportedTargetApps.packageNames)
        assertFalse("com.android.chrome" in SupportedTargetApps.packageNames)
        assertFalse("jp.naver.line.android" in SupportedTargetApps.packageNames)
    }
}
