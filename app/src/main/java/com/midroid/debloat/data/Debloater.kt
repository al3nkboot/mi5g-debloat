package com.midroid.debloat.data

import com.midroid.debloat.shizuku.ShellRunner

private val PKG_PATTERN = Regex("^[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z][a-zA-Z0-9_]*)+$")

class Debloater(private val shell: ShellRunner, private val log: RemovalLog) {
    fun remove(pkg: String, group: Group): Result<Unit> {
        if (group != Group.SAFE || !PKG_PATTERN.matches(pkg)) {
            return Result.failure(IllegalArgumentException("Not a removable package: $pkg"))
        }
        val result = shell.run(listOf("pm", "uninstall", "--user", "0", pkg))
        if (result.exit != 0 || !result.out.contains("Success")) {
            return Result.failure(RuntimeException("Uninstall failed for $pkg: ${result.out}"))
        }
        log.add(pkg)
        return Result.success(Unit)
    }

    fun restore(pkg: String): Result<Unit> {
        if (!PKG_PATTERN.matches(pkg)) {
            return Result.failure(IllegalArgumentException("Invalid package: $pkg"))
        }
        val result = shell.run(listOf("cmd", "package", "install-existing", "--user", "0", pkg))
        if (result.exit != 0 || !result.out.contains("installed for user", ignoreCase = true)) {
            return Result.failure(RuntimeException("Restore failed for $pkg: ${result.out}"))
        }
        log.markRestored(pkg)
        return Result.success(Unit)
    }

    fun restoreAll(): List<Pair<String, Result<Unit>>> = log.pending().map { it.pkg to restore(it.pkg) }
}
