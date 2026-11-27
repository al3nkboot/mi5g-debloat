package com.midroid.debloat

import com.midroid.debloat.data.displayLabel
import org.junit.Assert.assertEquals
import org.junit.Test

class DisplayLabelTest {
    @Test
    fun blankLabelFallsBackToPackageAndNonBlankIsTrimmed() {
        assertEquals("com.example.app", displayLabel("", "com.example.app"))
        assertEquals("com.example.app", displayLabel("   ", "com.example.app"))
        assertEquals("Example", displayLabel("  Example  ", "com.example.app"))
    }
}
