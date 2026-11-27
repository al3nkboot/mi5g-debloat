package com.midroid.debloat

import com.midroid.debloat.data.OemId
import com.midroid.debloat.data.mapOem
import org.junit.Assert.assertEquals
import org.junit.Test

class ManufacturerMapperTest {
    private data class Case(
        val manufacturer: String,
        val brand: String,
        val expected: OemId,
    )

    @Test
    fun aliasesAreTrimmedAndCaseInsensitive() {
        val cases = listOf(
            Case(" samsung ", "samsung", OemId.SAMSUNG),
            Case("Xiaomi", "Redmi", OemId.XIAOMI),
            Case("REDMI", "unknown", OemId.XIAOMI),
            Case("unknown", " pOcO ", OemId.XIAOMI),
            Case("realme", "realme", OemId.COLOROS),
            Case(" OPPO", "unknown", OemId.COLOROS),
            Case("unknown", "OnePlus", OemId.COLOROS),
            Case("HUAWEI", "huawei", OemId.HONOR),
            Case(" Honor ", "unknown", OemId.HONOR),
            Case("iQOO", "iQOO", OemId.VIVO),
            Case(" vivo ", "unknown", OemId.VIVO),
            Case("Google", "Google", OemId.GOOGLE),
            Case("unknown", " Pixel ", OemId.GOOGLE),
        )
        cases.forEach { case ->
            assertEquals(
                "${case.manufacturer}/${case.brand}",
                case.expected,
                mapOem(case.manufacturer, case.brand),
            )
        }
    }

    @Test
    fun unknownManufacturersUseTheGenericGoogleList() {
        assertEquals(OemId.GOOGLE, mapOem("Nothing", "Nothing"))
        assertEquals(OemId.GOOGLE, mapOem("  ", "unknown"))
    }
}
