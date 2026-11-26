package com.midroid.debloat.shizuku

import android.content.Context
import android.content.pm.PackageManager
import rikka.shizuku.Shizuku
import java.io.InputStream

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

private fun streamOf(process: Any, getter: String): InputStream =
    if (process is Process) {
        if (getter == "getInputStream") process.inputStream else process.errorStream
    } else {
        process.javaClass.getMethod(getter).invoke(process) as InputStream
    }

private fun waitForOf(process: Any): Int =
    if (process is Process) process.waitFor()
    else process.javaClass.getMethod("waitFor").invoke(process) as Int

object ShizukuRunner : ShellRunner {
    override fun run(cmd: List<String>): ShellResult = try {
        // shortcut: reflection on private Shizuku.newProcess, switch to a UserService if a future Shizuku version blocks it
        val method = Shizuku::class.java.getDeclaredMethod(
            "newProcess", Array<String>::class.java, Array<String>::class.java, String::class.java
        ).apply { isAccessible = true }
        val command = cmd.toTypedArray()
        if (command.isNotEmpty() && !command[0].contains("/")) command[0] = "/system/bin/${command[0]}"
        val process = method.invoke(
            null, command, arrayOf("PATH=/system/bin:/system/xbin:/vendor/bin"), null
        )!!
        val out = StringBuilder()
        val error = StringBuilder()
        val stdout = Thread { out.append(streamOf(process, "getInputStream").bufferedReader().use { it.readText() }) }
        val stderr = Thread { error.append(streamOf(process, "getErrorStream").bufferedReader().use { it.readText() }) }
        stdout.start()
        stderr.start()
        val exit = waitForOf(process)
        stdout.join()
        stderr.join()
        ShellResult(exit, "$out\n$error")
    } catch (e: Exception) {
        ShellResult(1, e.toString())
    }
}
