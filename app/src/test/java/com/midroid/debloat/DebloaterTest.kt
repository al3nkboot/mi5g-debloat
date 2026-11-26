package com.midroid.debloat

import com.midroid.debloat.data.Debloater
import com.midroid.debloat.data.Group
import com.midroid.debloat.data.RemovalLog
import com.midroid.debloat.shizuku.ShellResult
import com.midroid.debloat.shizuku.ShellRunner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class DebloaterTest {
    private class FakeShell(private val result: ShellResult = ShellResult(0, "Success")) : ShellRunner {
        val commands = mutableListOf<List<String>>()
        override fun run(cmd: List<String>): ShellResult {
            commands += cmd.toList()
            return result
        }
    }

    private fun setup(result: ShellResult = ShellResult(0, "Success")): Triple<Debloater, FakeShell, RemovalLog> {
        val shell = FakeShell(result)
        val log = RemovalLog(File.createTempFile("debloater_test", ".json").also { it.deleteOnExit() })
        return Triple(Debloater(shell, log), shell, log)
    }

    private val pkg = "com.miui.analytics"

    @Test
    fun removeRefusesOtherGroupsAndInvalidNames() {
        listOf(Group.USER, Group.PROTECTED).forEach { group ->
            val (debloater, shell, log) = setup()
            assertFalse(debloater.remove(pkg, group).isSuccess)
            assertTrue(shell.commands.isEmpty())
            assertTrue(log.entries().isEmpty())
        }
        listOf("com", "1bad.pkg", "com..example", "com.example.pkg;rm", "com.example.pkg ").forEach { bad ->
            val (debloater, shell, log) = setup()
            assertFalse(debloater.remove(bad, Group.SAFE).isSuccess)
            assertTrue(shell.commands.isEmpty())
            assertTrue(log.entries().isEmpty())
        }
    }

    @Test
    fun removeSafeRunsExactCommandAndLogs() {
        val (debloater, shell, log) = setup()
        assertTrue(debloater.remove(pkg, Group.SAFE).isSuccess)
        assertEquals(listOf(listOf("pm", "uninstall", "--user", "0", pkg)), shell.commands)
        assertEquals(listOf(pkg), log.entries().map { it.pkg })
    }

    @Test
    fun removeFailureDoesNotLog() {
        listOf(
            ShellResult(1, "Success"),
            ShellResult(0, "Failed"),
        ).forEach { result ->
            val (debloater, _, log) = setup(result)
            assertFalse(debloater.remove(pkg, Group.SAFE).isSuccess)
            assertTrue(log.entries().isEmpty())
        }
    }

    @Test
    fun restoreRunsExactCommandAndMarksRestored() {
        val (debloater, shell, log) = setup(ShellResult(0, "Package com.miui.analytics installed for user: 0"))
        log.add(pkg)
        assertTrue(debloater.restore(pkg).isSuccess)
        assertEquals(listOf(listOf("cmd", "package", "install-existing", "--user", "0", pkg)), shell.commands)
        assertEquals(emptyList<String>(), log.pending().map { it.pkg })
    }

    @Test
    fun restoreFailureDoesNotMarkRestored() {
        val (debloater, _, log) = setup(ShellResult(1, "installed for user"))
        log.add(pkg)
        assertFalse(debloater.restore(pkg).isSuccess)
        assertEquals(listOf(pkg), log.pending().map { it.pkg })
    }

    @Test
    fun restoreAllCoversAllPending() {
        val (debloater, shell, log) = setup(ShellResult(0, "Package installed for user: 0"))
        log.add("com.example.one")
        log.add("com.example.two")
        val results = debloater.restoreAll()
        assertEquals(2, results.size)
        assertTrue(results.all { it.second.isSuccess })
        assertEquals(2, shell.commands.size)
        assertTrue(log.pending().isEmpty())
    }
}
