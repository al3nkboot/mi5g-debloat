package com.midroid.debloat

import android.content.pm.ApplicationInfo
import com.midroid.debloat.data.ADVANCED_LIST
import com.midroid.debloat.data.EXPERT_LIST
import com.midroid.debloat.data.LogAction
import com.midroid.debloat.data.SAFE_LIST
import com.midroid.debloat.data.Tier
import com.midroid.debloat.data.defaultAction
import com.midroid.debloat.data.isSystemApp
import com.midroid.debloat.data.isUnsafe
import com.midroid.debloat.data.maySelect
import com.midroid.debloat.data.onInstalledList
import com.midroid.debloat.data.tierOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ClassifierTest {
    private val self = "com.midroid.debloat"

    @Test
    fun recommendedSystemPackageIsRecommended() {
        assertEquals(Tier.RECOMMENDED, tierOf("com.miui.analytics"))
        assertTrue(onInstalledList(isSystem = true, pkg = "com.miui.analytics", selfPkg = self))
    }

    @Test
    fun nonSystemRecommendedPackageIsClassifiedButHidden() {
        assertEquals(Tier.RECOMMENDED, tierOf("com.realmecomm.app"))
        assertFalse(onInstalledList(isSystem = false, pkg = "com.realmecomm.app", selfPkg = self))
    }

    @Test
    fun unknownSystemPackageIsUnknown() {
        assertEquals(Tier.UNKNOWN, tierOf("com.example.hidden"))
        assertTrue(onInstalledList(isSystem = true, pkg = "com.example.hidden", selfPkg = self))
    }

    @Test
    fun unsafeBeatsCustomRecommendedSetAndMatchesPrefixes() {
        val safe = setOf("com.android.systemui", "com.android.providers.contacts", "com.android.settings")
        assertEquals(Tier.UNSAFE, tierOf("com.android.systemui", safe))
        assertEquals(Tier.UNSAFE, tierOf("com.android.providers.contacts", safe))
        assertEquals(Tier.UNSAFE, tierOf("com.android.providers.contacts.foo", safe))
        assertEquals(Tier.UNSAFE, tierOf("android", safe + "android"))
        assertTrue(isUnsafe("com.oplus.camera"))
        assertTrue(isUnsafe("com.android.internal.systemui.navigation"))
    }

    @Test
    fun selfIsNeverOnInstalledList() {
        assertFalse(onInstalledList(isSystem = true, pkg = self, selfPkg = self))
        assertFalse(onInstalledList(isSystem = false, pkg = self, selfPkg = self))
    }

    @Test
    fun systemFlagsIncludeUpdatedSystemApps() {
        assertTrue(isSystemApp(ApplicationInfo.FLAG_SYSTEM))
        assertTrue(isSystemApp(ApplicationInfo.FLAG_UPDATED_SYSTEM_APP))
        assertTrue(isSystemApp(ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP))
        assertFalse(isSystemApp(0))
    }

    @Test
    fun selectionRulesMatchTierRisk() {
        assertTrue(maySelect(Tier.RECOMMENDED, allowUnsafe = false))
        assertTrue(maySelect(Tier.ADVANCED, allowUnsafe = false))
        listOf(Tier.EXPERT, Tier.UNKNOWN, Tier.UNSAFE).forEach {
            assertFalse(maySelect(it, allowUnsafe = false))
            assertTrue(maySelect(it, allowUnsafe = true))
        }
    }

    @Test
    fun unsafeDefaultsToDisableAndRecommendedToUninstall() {
        assertEquals(LogAction.DISABLED, defaultAction(Tier.UNSAFE))
        assertEquals(LogAction.UNINSTALLED, defaultAction(Tier.RECOMMENDED))
        assertEquals(LogAction.UNINSTALLED, defaultAction(Tier.ADVANCED))
    }

    @Test
    fun advancedAndExpertListsAreClassified() {
        ADVANCED_LIST.forEach { assertEquals(it.pkg, Tier.ADVANCED, tierOf(it.pkg)) }
        EXPERT_LIST.forEach { assertEquals(it.pkg, Tier.EXPERT, tierOf(it.pkg)) }
        assertEquals(13, ADVANCED_LIST.size)
        assertEquals(4, EXPERT_LIST.size)
        assertEquals(Tier.ADVANCED, tierOf("com.heytap.mcs"))
        assertEquals(Tier.EXPERT, tierOf("com.coloros.smartsidebar"))
    }

    @Test
    fun recommendedListIsCleanAndNeverUnsafe() {
        val pkgs = SAFE_LIST.map { it.pkg }
        assertEquals(pkgs.size, pkgs.toSet().size)
        pkgs.forEach { pkg ->
            assertFalse("$pkg is unsafe", isUnsafe(pkg))
            assertTrue("$pkg is malformed", PKG.matches(pkg))
        }
    }

    companion object {
        private val PKG = Regex("^[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z][a-zA-Z0-9_]*)+$")
    }
}
