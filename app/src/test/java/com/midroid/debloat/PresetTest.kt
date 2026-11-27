package com.midroid.debloat

import com.midroid.debloat.data.AppItem
import com.midroid.debloat.data.OemCatalog
import com.midroid.debloat.data.OemId
import com.midroid.debloat.data.applyPreset
import com.midroid.debloat.data.tierOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class PresetTest {
    private val catalog = OemCatalog(File("src/main/assets/lists"))
    private val selected = OemId.COLOROS

    private fun item(pkg: String, isSystem: Boolean = true) = AppItem(
        pkg = pkg,
        label = pkg,
        tier = tierOf(pkg, catalog, selected),
        isSystem = isSystem,
        reason = null,
    )

    @Test
    fun colorOsPresetSelectsOnlyPresentRecommendedSystemPackages() {
        val items = listOf(
            item("com.heytap.market"),
            item("com.facebook.appmanager"),
            item("com.heytap.mcs"),
            item("com.heytap.cloud"),
            item("com.coloros.safecenter"),
            item("com.example.unknown"),
            item("com.samsung.android.app.tips"),
            item("com.nearme.gamecenter", isSystem = false),
        )

        assertEquals(
            setOf("com.heytap.market", "com.facebook.appmanager"),
            applyPreset(items, catalog, selected),
        )
    }

    @Test
    fun emptyInputSelectsNothing() {
        assertTrue(applyPreset(emptyList(), catalog, selected).isEmpty())
    }
}
