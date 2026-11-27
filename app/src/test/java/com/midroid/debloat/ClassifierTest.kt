package com.midroid.debloat

import android.content.pm.ApplicationInfo
import com.midroid.debloat.data.LogAction
import com.midroid.debloat.data.OemCatalog
import com.midroid.debloat.data.OemId
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
import java.io.File

class ClassifierTest {
    private val self = "com.midroid.debloat"
    private val catalog = OemCatalog(File("src/main/assets/lists"))

    private fun tier(pkg: String, selected: OemId = OemId.ALL): Tier = tierOf(pkg, catalog, selected)

    @Test
    fun recommendedSystemPackageIsRecommended() {
        assertEquals(Tier.RECOMMENDED, tier("com.miui.analytics"))
        assertTrue(onInstalledList(isSystem = true, pkg = "com.miui.analytics", selfPkg = self))
    }

    @Test
    fun nonSystemRecommendedPackageIsClassifiedButHidden() {
        assertEquals(Tier.RECOMMENDED, tier("com.realmecomm.app"))
        assertFalse(onInstalledList(isSystem = false, pkg = "com.realmecomm.app", selfPkg = self))
    }

    @Test
    fun unknownSystemPackageIsUnknown() {
        assertEquals(Tier.UNKNOWN, tier("com.example.hidden"))
        assertTrue(onInstalledList(isSystem = true, pkg = "com.example.hidden", selfPkg = self))
    }

    @Test
    fun unsafeBeatsCatalogEntriesAndMatchesPrefixes() {
        assertEquals(Tier.UNSAFE, tier("com.android.systemui"))
        assertEquals(Tier.UNSAFE, tier("com.android.providers.contacts"))
        assertEquals(Tier.UNSAFE, tier("com.android.providers.contacts.foo"))
        assertEquals(Tier.UNSAFE, tier("android"))
        assertTrue(isUnsafe("com.oplus.camera"))
        assertTrue(isUnsafe("com.android.internal.systemui.navigation"))
        assertTrue(isUnsafe("com.samsung.android.knox.securefolder"))
        assertTrue(isUnsafe("com.samsung.knox.securefolder"))
        assertTrue(isUnsafe("com.sec.enterprise.knox"))
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
    fun migratedAdvancedAndExpertEntriesAreClassified() {
        catalog.lists.flatMap { it.packages }
            .filter { it.tier == Tier.ADVANCED || it.tier == Tier.EXPERT }
            .filterNot { isUnsafe(it.pkg) }
            .forEach { assertEquals(it.pkg, it.tier, tier(it.pkg)) }
        assertEquals(Tier.RECOMMENDED, tier("com.heytap.market"))
        assertEquals(Tier.ADVANCED, tier("com.heytap.mcs"))
        assertEquals(Tier.EXPERT, tier("com.heytap.cloud"))
        assertEquals(Tier.EXPERT, tier("com.coloros.smartsidebar"))
        assertEquals(Tier.UNSAFE, tier("com.coloros.safecenter"))
    }

    @Test
    fun recommendedCatalogIsCleanAndNeverUnsafe() {
        catalog.lists.forEach { list ->
            val pkgs = list.packages.map { it.pkg }
            assertEquals(pkgs.size, pkgs.toSet().size)
            pkgs.forEach { pkg -> assertTrue("$pkg is malformed", PKG.matches(pkg)) }
        }
        catalog.lists.flatMap { it.packages }.filter { it.tier == Tier.RECOMMENDED }.forEach {
            assertFalse("${it.pkg} is unsafe", isUnsafe(it.pkg))
            assertEquals(it.pkg, Tier.RECOMMENDED, tier(it.pkg))
        }
    }

    companion object {
        private val PKG = Regex("^[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z][a-zA-Z0-9_]*)+$")
    }
}
