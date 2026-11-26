package com.midroid.debloat

import com.midroid.debloat.data.LogAction
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
        log.add("com.example.one", now = 1000L)
        assertEquals(listOf(LogEntry("com.example.one", 1000L, false, LogAction.UNINSTALLED)), log.entries())
        assertEquals(listOf(LogEntry("com.example.one", 1000L, false, LogAction.UNINSTALLED)), RemovalLog(file).entries())
    }

    @Test
    fun disabledActionRoundTrips() {
        val file = tempFile()
        val log = RemovalLog(file)
        log.add("com.example.one", LogAction.DISABLED, now = 1000L)
        assertEquals(LogAction.DISABLED, RemovalLog(file).entries().single().action)
    }

    @Test
    fun entryWithoutActionReadsAsUninstalled() {
        val file = tempFile()
        file.writeText("""[{"pkg":"com.example.one","timeMillis":1000,"restored":false}]""")
        assertEquals(LogAction.UNINSTALLED, RemovalLog(file).entries().single().action)
        file.writeText("""[{"pkg":"com.example.two","timeMillis":1000,"restored":false,"action":"odd"}]""")
        assertEquals(LogAction.UNINSTALLED, RemovalLog(file).entries().single().action)
    }

    @Test
    fun reAddReplacesAndResetsRestored() {
        val log = RemovalLog(tempFile())
        log.add("com.example.one", now = 1000L)
        log.markRestored("com.example.one")
        assertEquals(emptyList<LogEntry>(), log.pending())

        log.add("com.example.one", now = 2000L)
        assertEquals(listOf(LogEntry("com.example.one", 2000L, false, LogAction.UNINSTALLED)), log.entries())
        assertEquals(1, log.pending().size)
    }

    @Test
    fun markRestoredOnlyTouchesMatchingPackage() {
        val log = RemovalLog(tempFile())
        log.add("com.example.one", now = 1L)
        log.add("com.example.two", now = 2L)
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
        log.add("com.example.one", now = 5L)
        assertEquals(listOf("com.example.one"), log.entries().map { it.pkg })
        assertTrue(file.readText().contains("com.example.one"))
    }
}
