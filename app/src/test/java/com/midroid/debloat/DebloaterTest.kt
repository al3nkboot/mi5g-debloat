package com.midroid.debloat

import com.midroid.debloat.data.Debloater
import com.midroid.debloat.data.LogAction
import com.midroid.debloat.data.Outcome
import com.midroid.debloat.data.RemovalLog
import com.midroid.debloat.data.Tier
import com.midroid.debloat.data.enableOrDisableSucceeded
import com.midroid.debloat.data.installExistingSucceeded
import com.midroid.debloat.data.isPackageMissing
import com.midroid.debloat.data.uninstallSucceeded
import com.midroid.debloat.shizuku.ShellResult
import com.midroid.debloat.shizuku.ShellRunner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class DebloaterTest {
    private class FakeShell(private val resultFor: (List<String>) -> ShellResult) : ShellRunner {
        val commands = mutableListOf<List<String>>()
        override fun run(cmd: List<String>): ShellResult {
            commands += cmd.toList()
            return resultFor(cmd)
        }
    }

    private fun setup(
        resultFor: (List<String>) -> ShellResult = { ShellResult(0, "Success") },
    ): Triple<Debloater, FakeShell, RemovalLog> {
        val shell = FakeShell(resultFor)
        val log = RemovalLog(File.createTempFile("debloater_test", ".json").also { it.deleteOnExit() })
        return Triple(Debloater(shell, log), shell, log)
    }

    private val pkg = "com.miui.analytics"
    private val uninstallCmd = listOf("pm", "uninstall", "--user", "0", pkg)
    private val disableCmd = listOf("pm", "disable-user", "--user", "0", pkg)
    private val cmdExisting = listOf("cmd", "package", "install-existing", "--user", "0", pkg)
    private val pmExisting = listOf("pm", "install-existing", "--user", "0", pkg)
    private val enableCmd = listOf("pm", "enable", pkg)

    @Test
    fun removeRecommendedSystemAppRunsExactCommandAndLogsUninstalled() {
        val (debloater, shell, log) = setup()
        val result = debloater.remove(pkg, isSystem = true, tier = Tier.RECOMMENDED, allowUnsafe = false)
        assertEquals(Outcome.UNINSTALLED, result.outcome)
        assertEquals(listOf(uninstallCmd), shell.commands)
        assertEquals(listOf(pkg), log.entries().map { it.pkg })
        assertEquals(LogAction.UNINSTALLED, log.entries().single().action)
    }

    @Test
    fun removeRefusesInvalidNames() {
        listOf("com", "1bad.pkg", "com..example", "com.example.pkg;rm", "com.example.pkg ").forEach { bad ->
            val (debloater, shell, log) = setup()
            val result = debloater.remove(bad, isSystem = true, tier = Tier.RECOMMENDED, allowUnsafe = false)
            assertEquals(Outcome.FAILED, result.outcome)
            assertTrue(shell.commands.isEmpty())
            assertTrue(log.entries().isEmpty())
        }
    }

    @Test
    fun removeRefusesUserInstalledAppEvenWhenRecommended() {
        val (debloater, shell, log) = setup()
        val result = debloater.remove("com.realmecomm.app", isSystem = false, tier = Tier.RECOMMENDED, allowUnsafe = false)
        assertEquals(Outcome.FAILED, result.outcome)
        assertTrue(result.detail.contains("user-installed"))
        assertTrue(result.detail.contains("no system image"))
        assertTrue(shell.commands.isEmpty())
        assertTrue(log.entries().isEmpty())
    }

    @Test
    fun removeRefusesExpertAndUnsafeWhenSettingIsOff() {
        listOf(Tier.EXPERT, Tier.UNKNOWN, Tier.UNSAFE).forEach { tier ->
            val (debloater, shell, log) = setup()
            val result = debloater.remove(pkg, isSystem = true, tier = tier, allowUnsafe = false)
            assertEquals(Outcome.FAILED, result.outcome)
            assertTrue(result.detail.contains("unsafe selections setting is required", ignoreCase = true))
            assertTrue(shell.commands.isEmpty())
            assertTrue(log.entries().isEmpty())
        }
    }

    @Test
    fun removeUnsafeOnlyDisablesAndLogsDisabled() {
        val (debloater, shell, log) = setup()
        val result = debloater.remove(pkg, isSystem = true, tier = Tier.UNSAFE, allowUnsafe = true)
        assertEquals(Outcome.DISABLED, result.outcome)
        assertEquals(listOf(disableCmd), shell.commands)
        assertEquals(LogAction.DISABLED, log.entries().single().action)
    }

    @Test
    fun removeUnsafeFailureDoesNotFallBackOrLog() {
        val (debloater, shell, log) = setup { ShellResult(1, "Error [disable failed]") }
        val result = debloater.remove(pkg, isSystem = true, tier = Tier.UNSAFE, allowUnsafe = true)
        assertEquals(Outcome.FAILED, result.outcome)
        assertEquals("exit 1: Error [disable failed]", result.detail)
        assertEquals(listOf(disableCmd), shell.commands)
        assertTrue(log.entries().isEmpty())
    }

    @Test
    fun recommendedAndAdvancedFallBackToDisableWhenUninstallFails() {
        listOf(Tier.RECOMMENDED, Tier.ADVANCED).forEach { tier ->
            listOf(
                ShellResult(1, "Failure [DELETE_FAILED_INTERNAL_ERROR]") to
                    ShellResult(0, "Package $pkg new state: disabled-user"),
                ShellResult(0, "Failed") to ShellResult(0, ""),
            ).forEach { (uninstall, disable) ->
                val (debloater, shell, log) = setup { cmd ->
                    if (cmd == uninstallCmd) uninstall else disable
                }
                val result = debloater.remove(pkg, isSystem = true, tier = tier, allowUnsafe = false)
                assertEquals(Outcome.DISABLED, result.outcome)
                assertEquals(listOf(uninstallCmd, disableCmd), shell.commands)
                assertEquals(LogAction.DISABLED, log.entries().single().action)
                assertTrue(result.detail.contains(uninstall.out.trim()))
            }
        }
    }

    @Test
    fun removeBothFailuresDoNotLog() {
        val (debloater, shell, log) = setup { cmd ->
            if (cmd == uninstallCmd) {
                ShellResult(1, "Failure [DELETE_FAILED_INTERNAL_ERROR]")
            } else {
                ShellResult(1, "Error [disable failed]")
            }
        }
        val result = debloater.remove(pkg, isSystem = true, tier = Tier.RECOMMENDED, allowUnsafe = false)
        assertEquals(Outcome.FAILED, result.outcome)
        assertEquals(listOf(uninstallCmd, disableCmd), shell.commands)
        assertTrue(log.entries().isEmpty())
        assertTrue(result.detail.contains("Failure [DELETE_FAILED_INTERNAL_ERROR]"))
        assertTrue(result.detail.contains("Error [disable failed]"))
    }

    @Test
    fun restoreUninstalledSucceedsOnFirstCommand() {
        val (debloater, shell, log) = setup { cmd ->
            if (cmd == cmdExisting) ShellResult(0, "Package $pkg installed for user: 0")
            else ShellResult(1, "Unexpected command")
        }
        log.add(pkg)
        assertEquals(Outcome.RESTORED, debloater.restore(pkg).outcome)
        assertEquals(listOf(cmdExisting), shell.commands)
        assertTrue(log.pending().isEmpty())
        assertEquals(LogAction.UNINSTALLED, log.entries().single().action)
    }

    @Test
    fun restoreUninstalledFallsBackToPmInstallExisting() {
        val (debloater, shell, log) = setup { cmd ->
            if (cmd == cmdExisting) ShellResult(1, "Failure [not installed]")
            else ShellResult(0, "Package com.miui.analytics installed for user: 0")
        }
        log.add(pkg)
        val result = debloater.restore(pkg)
        assertEquals(Outcome.RESTORED, result.outcome)
        assertEquals(listOf(cmdExisting, pmExisting), shell.commands)
        assertTrue(log.pending().isEmpty())
        assertTrue(result.detail.contains("Failure [not installed]"))
        assertTrue(result.detail.contains("installed for user"))
    }

    @Test
    fun restoreDisabledUsesPmEnable() {
        val (debloater, shell, log) = setup { cmd ->
            if (cmd == enableCmd) ShellResult(0, "") else ShellResult(1, "Unexpected install-existing")
        }
        log.add(pkg, LogAction.DISABLED)
        assertEquals(Outcome.RESTORED, debloater.restore(pkg).outcome)
        assertEquals(listOf(enableCmd), shell.commands)
        assertTrue(log.pending().isEmpty())
        assertEquals(LogAction.DISABLED, log.entries().single().action)
    }

    @Test
    fun restoreLegacyLogEntryUsesInstallExisting() {
        val file = File.createTempFile("debloater_legacy", ".json").also { it.deleteOnExit() }
        file.writeText("""[{"pkg":"com.miui.analytics","timeMillis":1000,"restored":false}]""")
        val log = RemovalLog(file)
        val shell = FakeShell { ShellResult(0, "Package com.miui.analytics installed for user: 0") }
        assertEquals(Outcome.RESTORED, Debloater(shell, log).restore(pkg).outcome)
        assertEquals(listOf(cmdExisting), shell.commands)
        assertTrue(log.pending().isEmpty())
    }

    @Test
    fun restoreRefusesInvalidNames() {
        val (debloater, shell, log) = setup()
        val result = debloater.restore("com.example.pkg;rm")
        assertEquals(Outcome.FAILED, result.outcome)
        assertTrue(shell.commands.isEmpty())
        assertTrue(log.entries().isEmpty())
    }

    @Test
    fun restoreFailureDoesNotMarkRestored() {
        val (debloater, shell, log) = setup { ShellResult(1, "Failure [not installed]") }
        log.add(pkg)
        val result = debloater.restore(pkg)
        assertEquals(Outcome.FAILED, result.outcome)
        assertEquals(listOf(cmdExisting, pmExisting), shell.commands)
        assertEquals(listOf(pkg), log.pending().map { it.pkg })
        assertTrue(result.detail.contains("exit 1"))
        assertTrue(result.detail.contains("not installed"))
    }

    @Test
    fun restoreAllCoversAllPending() {
        val (debloater, shell, log) = setup { ShellResult(0, "Package installed for user: 0") }
        log.add("com.example.one")
        log.add("com.example.two")
        val results = debloater.restoreAll()
        assertEquals(2, results.size)
        assertTrue(results.all { it.outcome == Outcome.RESTORED })
        assertEquals(2, shell.commands.size)
        assertTrue(log.pending().isEmpty())
    }

    @Test
    fun pmOutputParsing() {
        assertTrue(installExistingSucceeded(0, "Package com.foo installed for user: 0"))
        assertTrue(installExistingSucceeded(0, "Success"))
        assertFalse(installExistingSucceeded(0, "Failure [INSTALL_FAILED_INVALID_URI]"))
        assertFalse(installExistingSucceeded(1, "Package com.foo installed for user: 0"))
        assertTrue(uninstallSucceeded(0, "Success"))
        assertFalse(uninstallSucceeded(0, "Failed"))
        assertTrue(enableOrDisableSucceeded(0, ""))
        assertFalse(enableOrDisableSucceeded(1, "Error"))
    }

    @Test
    fun packageMissingParsingMatchesOnlyActualMissingPackageErrors() {
        assertTrue(isPackageMissing("install-existing: exit 1: NameNotFoundException: Package com.realmecomm.app doesn't exist"))
        assertTrue(isPackageMissing("Package COM.EXAMPLE.APP DOES NOT EXIST"))
        assertFalse(isPackageMissing("Failure [not installed]"))
        assertFalse(isPackageMissing("Failure [DELETE_FAILED_INTERNAL_ERROR]"))
    }
}
