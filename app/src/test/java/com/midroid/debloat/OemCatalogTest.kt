package com.midroid.debloat

import com.midroid.debloat.data.OemCatalog
import com.midroid.debloat.data.OemEntry
import com.midroid.debloat.data.OemId
import com.midroid.debloat.data.OemList
import com.midroid.debloat.data.Tier
import com.midroid.debloat.data.isUnsafe
import com.midroid.debloat.data.parseOemList
import com.midroid.debloat.data.tierOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class OemCatalogTest {
    private val catalog = OemCatalog(File("src/main/assets/lists"))

    @Test
    fun everyAssetIsStrictAndInternallyConsistent() {
        assertEquals(6, catalog.lists.size)
        val allEntries = catalog.lists.flatMap { it.packages }
        val unsafePkgs = allEntries.filter { it.tier == Tier.UNSAFE }.map { it.pkg }.toSet()

        catalog.lists.forEach { list ->
            val pkgs = list.packages.map { it.pkg }
            assertEquals("duplicate package in ${list.id}", pkgs.size, pkgs.toSet().size)
            list.packages.forEach { entry ->
                assertTrue("unknown reason ${entry.reason}", entry.reason in KNOWN_REASONS)
                if (entry.tier == Tier.RECOMMENDED) {
                    assertFalse("${entry.pkg} matches a deny prefix", isUnsafe(entry.pkg))
                    assertFalse("${entry.pkg} is Unsafe in another list", entry.pkg in unsafePkgs)
                }
            }
        }

        allEntries.groupBy { it.pkg }.forEach { (pkg, entries) ->
            assertEquals("conflicting tiers for $pkg", 1, entries.map { it.tier }.toSet().size)
        }
    }

    @Test
    fun activeListsAndGlobalUnsafeFollowSelectionRules() {
        assertEquals(Tier.UNKNOWN, tierOf("com.samsung.android.app.tips", catalog, OemId.COLOROS))
        assertEquals(Tier.RECOMMENDED, tierOf("com.samsung.android.app.tips", catalog, OemId.SAMSUNG))
        assertEquals(Tier.RECOMMENDED, tierOf("com.facebook.appmanager", catalog, OemId.SAMSUNG))
        assertEquals(Tier.RECOMMENDED, tierOf("com.facebook.appmanager", catalog, OemId.GOOGLE))
        assertEquals(Tier.UNSAFE, tierOf("com.xiaomi.account", catalog, OemId.GOOGLE))
    }

    @Test
    fun conflictingActiveTiersUseTheMoreConservativeTier() {
        val pkg = "com.example.shared"
        val fixture = OemCatalog(listOf(
            OemList(OemId.SAMSUNG, "Samsung", listOf(OemEntry(pkg, Tier.RECOMMENDED, "optional_tool"))),
            OemList(OemId.GOOGLE, "Google", listOf(OemEntry(pkg, Tier.EXPERT, "optional_tool"))),
        ))
        assertEquals(Tier.EXPERT, tierOf(pkg, fixture, OemId.SAMSUNG))
        assertEquals(Tier.EXPERT, tierOf(pkg, fixture, OemId.ALL))
    }

    @Test
    fun anUnsafeEntryInAnyListWinsForEverySelection() {
        val pkg = "com.example.shared"
        val fixture = OemCatalog(listOf(
            OemList(OemId.SAMSUNG, "Samsung", listOf(OemEntry(pkg, Tier.UNSAFE, "unsafe"))),
            OemList(OemId.GOOGLE, "Google", listOf(OemEntry(pkg, Tier.RECOMMENDED, "optional_tool"))),
        ))
        assertEquals(Tier.UNSAFE, tierOf(pkg, fixture, OemId.GOOGLE))
        assertEquals(Tier.UNSAFE, tierOf(pkg, fixture, OemId.COLOROS))
    }

    @Test
    fun parserRejectsUnknownTiersMissingFieldsAndDuplicates() {
        assertThrows(IllegalArgumentException::class.java) {
            parseOemList("""{"id":"google","label":"Google","packages":[{"pkg":"a.b","tier":"UNKNOWN","reason":"unsafe"}]}""")
        }
        assertThrows(IllegalArgumentException::class.java) {
            parseOemList("""{"id":"google","label":"Google","packages":[{"tier":"RECOMMENDED","reason":"unsafe"}]}""")
        }
        assertThrows(IllegalArgumentException::class.java) {
            parseOemList("""{"id":"google","label":"Google","packages":[{"pkg":"a.b","reason":"unsafe"}]}""")
        }
        assertThrows(IllegalArgumentException::class.java) {
            parseOemList("""{"id":"google","label":"Google","packages":[{"pkg":"a.b","tier":"RECOMMENDED","reason":"unsafe"},{"pkg":"a.b","tier":"ADVANCED","reason":"unsafe"}]}""")
        }
    }

    companion object {
        private val KNOWN_REASONS = setOf(
            "ads", "partner_stub", "social", "oem_store", "oem_browser", "oem_themes",
            "oem_games", "oem_media", "oem_community", "oem_tips", "lockscreen_feed", "ar",
            "kids", "cloud", "assistant", "share", "optional_google", "optional_tool",
            "telemetry", "keyboard", "find_device", "account_service", "payment", "unsafe",
        )
    }
}
