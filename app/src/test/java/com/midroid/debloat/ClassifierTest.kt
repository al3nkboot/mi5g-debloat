package com.midroid.debloat

import com.midroid.debloat.data.DENY_PREFIXES
import com.midroid.debloat.data.Group
import com.midroid.debloat.data.SAFE_LIST
import com.midroid.debloat.data.classify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ClassifierTest {
    private val self = "com.midroid.debloat"

    @Test
    fun safeSystemAppIsSafe() {
        assertEquals(Group.SAFE, classify("com.miui.analytics", true, self))
    }

    @Test
    fun nonSystemAppOnSafeListIsSafe() {
        assertEquals(Group.SAFE, classify("com.glance.internet", false, self))
    }

    @Test
    fun unknownNonSystemAppIsUser() {
        assertEquals(Group.USER, classify("com.example.notes", false, self))
    }

    @Test
    fun unknownSystemAppIsProtected() {
        assertEquals(Group.PROTECTED, classify("com.example.hidden", true, self))
    }

    @Test
    fun selfIsProtected() {
        assertEquals(Group.PROTECTED, classify(self, false, self))
    }

    @Test
    fun denyPrefixBeatsSafeList() {
        val safe = setOf("com.android.systemui", "com.android.providers.contacts", "com.android.settings")
        assertEquals(Group.PROTECTED, classify("com.android.systemui", true, self, safe))
        assertEquals(Group.PROTECTED, classify("com.android.providers.contacts", false, self, safe))
        assertEquals(Group.PROTECTED, classify("com.android.providers.contacts.foo", false, self, safe))
        assertEquals(Group.PROTECTED, classify("android", false, self, safe))
    }

    @Test
    fun newlyAddedPackagesAreSafe() {
        listOf(
            "com.heytap.pictorial",
            "com.heytap.market",
            "com.oppo.market",
            "com.nearme.browser",
            "com.oplus.games",
            "com.coloros.gamespaceui",
            "com.coloros.gamespace",
            "com.heytap.themestore",
            "com.oplus.themestore",
            "com.nearme.themespace",
            "com.nearme.themestore",
            "com.realmecomm.app",
        ).forEach { assertEquals(Group.SAFE, classify(it, true, self)) }
    }

    @Test
    fun realSafeListIsClean() {
        val pkgs = SAFE_LIST.map { it.pkg }
        assertEquals(pkgs.size, pkgs.toSet().size)
        pkgs.forEach { pkg ->
            assertFalse("$pkg is denied", DENY_PREFIXES.any {
                if (it.endsWith(".")) pkg == it || pkg.startsWith(it) else pkg == it
            })
            assertFalse("$pkg is malformed", !PKG.matches(pkg))
        }
    }

    companion object {
        private val PKG = Regex("^[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z][a-zA-Z0-9_]*)+$")
    }
}
