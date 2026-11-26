package com.midroid.debloat

import com.midroid.debloat.data.LogEntry
import com.midroid.debloat.data.RemovalLog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class RemovalLogTest {
    private fun tempFile(): File = File.createTempFile("removal_log_test", ".json").also { it.deleteOnExit() }

    @Test
    fun addAndReadRoundTrip() {
        val file = tempFile()
        val log = RemovalLog(file)
        log.add("com.example.one", 1000L)
        assertEquals(listOf(LogEntry("com.example.one", 1000L, false)), log.entries())
        assertEquals(listOf(LogEntry("com.example.one", 1000L, false)), RemovalLog(file).entries())
    }

    @Test
    fun reAddReplacesAndResetsRestored() {
        val log = RemovalLog(tempFile())
        log.add("com.example.one", 1000L)
        log.markRestored("com.example.one")
        assertEquals(emptyList<LogEntry>(), log.pending())

        log.add("com.example.one", 2000L)
        assertEquals(listOf(LogEntry("com.example.one", 2000L, false)), log.entries())
        assertEquals(1, log.pending().size)
    }

    @Test
    fun markRestoredOnlyTouchesMatchingPackage() {
        val log = RemovalLog(tempFile())
        log.add("com.example.one", 1L)
        log.add("com.example.two", 2L)
        log.markRestored("com.example.one")
        assertEquals(listOf("com.example.two"), log.pending().map { it.pkg })
    }

    @Test
    fun missingFileReturnsEmpty() {
        val missing = File(tempFile().also { it.delete() }.path)
        assertEquals(emptyList<LogEntry>(), RemovalLog(missing).entries())
        assertEquals(emptyList<LogEntry>(), RemovalLog(missing).pending())
    }

    @Test
    fun corruptFileReturnsEmptyAndCanBeOverwritten() {
        val file = tempFile()
        file.writeText("{not json at all")
        val log = RemovalLog(file)
        assertEquals(emptyList<LogEntry>(), log.entries())
        log.add("com.example.one", 5L)
        assertEquals(listOf("com.example.one"), log.entries().map { it.pkg })
        assertTrue(file.readText().contains("com.example.one"))
    }
}
