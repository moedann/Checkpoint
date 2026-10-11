package com.example.nanisini

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PermissionCheckerTest {
    private val packageName = "com.example.nanisini"
    private val serviceClassName = "$packageName.AppDetectionAccessibilityService"

    @Test
    fun matchesFullyQualifiedFlattenedComponent() {
        assertTrue(
            PermissionChecker.isServiceListed(
                "$packageName/$serviceClassName",
                packageName,
                serviceClassName,
            ),
        )
    }

    @Test
    fun matchesShortFlattenedComponentAmongOtherServices() {
        assertTrue(
            PermissionChecker.isServiceListed(
                "other.app/other.app.Service:$packageName/.AppDetectionAccessibilityService",
                packageName,
                serviceClassName,
            ),
        )
    }

    @Test
    fun rejectsClassNameWithoutMatchingPackageComponent() {
        assertFalse(
            PermissionChecker.isServiceListed(
                "other.app/$serviceClassName",
                packageName,
                serviceClassName,
            ),
        )
    }
}
