package com.midroid.debloat.data

import com.midroid.debloat.shizuku.ShellResult
import com.midroid.debloat.shizuku.ShellRunner

private val PKG_PATTERN = Regex("^[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z][a-zA-Z0-9_]*)+$")

enum class Outcome { UNINSTALLED, DISABLED, RESTORED, FAILED }

data class ActionResult(val pkg: String, val outcome: Outcome, val detail: String)

class Debloater(private val shell: ShellRunner, private val log: RemovalLog) {
    fun remove(pkg: String, isSystem: Boolean, tier: Tier, allowUnsafe: Boolean): ActionResult {
        if (!PKG_PATTERN.matches(pkg)) {
            return ActionResult(pkg, Outcome.FAILED, "Invalid package: $pkg")
        }
        if (!isSystem) {
            return ActionResult(
                pkg,
                Outcome.FAILED,
                "user-installed app; uninstall would delete it with no system image to restore from: $pkg",
            )
        }
        if (!maySelect(tier, allowUnsafe)) {
            return ActionResult(pkg, Outcome.FAILED, "Allow unsafe selections setting is required: $pkg")
        }
        if (tier == Tier.UNSAFE) {
            val disable = shell.run(listOf("pm", "disable-user", "--user", "0", pkg))
            val detail = formatDetail(disable.exit, disable.out)
            if (enableOrDisableSucceeded(disable.exit, disable.out)) {
                log.add(pkg, LogAction.DISABLED)
                return ActionResult(pkg, Outcome.DISABLED, detail)
            }
            return ActionResult(pkg, Outcome.FAILED, detail)
        }
        val uninstall = shell.run(listOf("pm", "uninstall", "--user", "0", pkg))
        if (uninstallSucceeded(uninstall.exit, uninstall.out)) {
            log.add(pkg, LogAction.UNINSTALLED)
            return ActionResult(pkg, Outcome.UNINSTALLED, formatDetail(uninstall.exit, uninstall.out))
        }
        val disable = shell.run(listOf("pm", "disable-user", "--user", "0", pkg))
        val detail = details("uninstall" to uninstall, "disable" to disable)
        if (enableOrDisableSucceeded(disable.exit, disable.out)) {
            log.add(pkg, LogAction.DISABLED)
            return ActionResult(pkg, Outcome.DISABLED, detail)
        }
        return ActionResult(pkg, Outcome.FAILED, detail)
    }

    fun restore(pkg: String): ActionResult {
        if (!PKG_PATTERN.matches(pkg)) {
            return ActionResult(pkg, Outcome.FAILED, "Invalid package: $pkg")
        }
        val action = log.entries().firstOrNull { it.pkg == pkg }?.action ?: LogAction.UNINSTALLED
        if (action == LogAction.DISABLED) {
            val enable = shell.run(listOf("pm", "enable", pkg))
            val detail = details("enable" to enable)
            return if (enableOrDisableSucceeded(enable.exit, enable.out)) {
                log.markRestored(pkg)
                ActionResult(pkg, Outcome.RESTORED, detail)
            } else {
                ActionResult(pkg, Outcome.FAILED, detail)
            }
        }
        val cmdExisting = shell.run(listOf("cmd", "package", "install-existing", "--user", "0", pkg))
        if (installExistingSucceeded(cmdExisting.exit, cmdExisting.out)) {
            log.markRestored(pkg)
            return ActionResult(pkg, Outcome.RESTORED, details("install-existing" to cmdExisting))
        }
        val pmExisting = shell.run(listOf("pm", "install-existing", "--user", "0", pkg))
        val detail = details("install-existing" to cmdExisting, "pm install-existing" to pmExisting)
        if (installExistingSucceeded(pmExisting.exit, pmExisting.out)) {
            log.markRestored(pkg)
            return ActionResult(pkg, Outcome.RESTORED, detail)
        }
        return ActionResult(pkg, Outcome.FAILED, detail)
    }

    fun restoreAll(): List<ActionResult> = log.pending().map { restore(it.pkg) }

    private fun details(vararg attempts: Pair<String, ShellResult>): String =
        attempts.joinToString("\n") { (label, result) ->
            "$label: ${formatDetail(result.exit, result.out)}"
        }
}
