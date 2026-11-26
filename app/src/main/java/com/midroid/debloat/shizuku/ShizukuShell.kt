package com.midroid.debloat.shizuku

import android.content.Context
import android.content.pm.PackageManager
import rikka.shizuku.Shizuku

enum class ShizukuState { NOT_INSTALLED, NOT_RUNNING, NEEDS_PERMISSION, READY }

fun shizukuState(context: Context): ShizukuState {
    val installed = try {
        @Suppress("DEPRECATION")
        context.packageManager.getPackageInfo("moe.shizuku.privileged.api", 0)
        true
    } catch (e: PackageManager.NameNotFoundException) {
        false
    }
    if (!installed) return ShizukuState.NOT_INSTALLED
    if (Shizuku.isPreV11()) return ShizukuState.NEEDS_PERMISSION
    if (!Shizuku.pingBinder()) return ShizukuState.NOT_RUNNING
    return try {
        if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
            ShizukuState.READY
        } else {
            ShizukuState.NEEDS_PERMISSION
        }
    } catch (e: RuntimeException) {
        ShizukuState.NEEDS_PERMISSION
    }
}

fun requestShizukuPermission(code: Int) = Shizuku.requestPermission(code)

interface ShellRunner {
    fun run(cmd: List<String>): ShellResult
}

data class ShellResult(val exit: Int, val out: String)

object ShizukuRunner : ShellRunner {
    override fun run(cmd: List<String>): ShellResult = try {
        // shortcut: reflection on private Shizuku.newProcess, switch to a UserService if a future Shizuku version blocks it
        val method = Shizuku::class.java.getDeclaredMethod(
            "newProcess", Array<String>::class.java, Array<String>::class.java, String::class.java
        ).apply { isAccessible = true }
        val process = method.invoke(null, cmd.toTypedArray(), null, null) as Process
        val error = StringBuilder()
        val stderr = Thread { error.append(process.errorStream.bufferedReader().use { it.readText() }) }
        stderr.start()
        val out = process.inputStream.bufferedReader().use { it.readText() }
        val exit = process.waitFor()
        stderr.join()
        ShellResult(exit, "$out\n$error")
    } catch (e: Exception) {
        ShellResult(1, e.toString())
    }
}
