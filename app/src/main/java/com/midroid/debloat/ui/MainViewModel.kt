package com.midroid.debloat.ui

import android.app.Application
import android.content.pm.PackageManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.midroid.debloat.R
import com.midroid.debloat.data.AppItem
import com.midroid.debloat.data.Debloater
import com.midroid.debloat.data.Group
import com.midroid.debloat.data.LogEntry
import com.midroid.debloat.data.RemovalLog
import com.midroid.debloat.data.scan as scanApps
import com.midroid.debloat.shizuku.ShizukuRunner
import com.midroid.debloat.shizuku.ShizukuState
import com.midroid.debloat.shizuku.requestShizukuPermission
import com.midroid.debloat.shizuku.shizukuState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import java.io.File

private const val PERMISSION_CODE = 42

enum class Screen { HOME, LOG, ABOUT }

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val log = RemovalLog(File(app.filesDir, "removal_log.json"))
    private val debloater = Debloater(ShizukuRunner, log)

    var screen by mutableStateOf(Screen.HOME)
        private set
    var state by mutableStateOf(ShizukuState.NOT_INSTALLED)
        private set
    var items by mutableStateOf<List<AppItem>>(emptyList())
        private set
    var selected by mutableStateOf<Set<String>>(emptySet())
        private set
    var busy by mutableStateOf(false)
        private set
    var message by mutableStateOf<String?>(null)
        private set
    var logEntries by mutableStateOf<List<LogEntry>>(emptyList())
        private set
    var confirmRemove by mutableStateOf(false)
        private set

    private val binderReceived = Shizuku.OnBinderReceivedListener { refresh() }
    private val binderDead = Shizuku.OnBinderDeadListener { refresh() }
    private val permissionResult = Shizuku.OnRequestPermissionResultListener { _, grant ->
        if (grant != PackageManager.PERMISSION_GRANTED) message = text(R.string.permission_denied)
        refresh()
    }

    init {
        Shizuku.addBinderReceivedListener(binderReceived)
        Shizuku.addBinderDeadListener(binderDead)
        Shizuku.addRequestPermissionResultListener(permissionResult)
        refresh()
        refreshLog()
    }

    override fun onCleared() {
        Shizuku.removeBinderReceivedListener(binderReceived)
        Shizuku.removeBinderDeadListener(binderDead)
        Shizuku.removeRequestPermissionResultListener(permissionResult)
        super.onCleared()
    }

    private fun text(res: Int, vararg args: Any): String = getApplication<Application>().getString(res, *args)

    fun refresh() {
        state = shizukuState(getApplication())
    }

    fun requestPermission() = requestShizukuPermission(PERMISSION_CODE)

    fun showHome() { screen = Screen.HOME }
    fun showLog() { screen = Screen.LOG; refreshLog() }
    fun showAbout() { screen = Screen.ABOUT }

    private fun reload() {
        items = scanApps(getApplication())
        selected = emptySet()
    }

    fun scan() {
        reload()
        message = text(R.string.scanned_count, items.size)
    }

    fun toggle(pkg: String) {
        selected = if (pkg in selected) selected - pkg else selected + pkg
    }

    fun selectAll(all: Boolean) {
        selected = if (all) items.filter { it.group == Group.SAFE }.map { it.pkg }.toSet() else emptySet()
    }

    fun askRemove() { confirmRemove = true }
    fun dismissRemove() { confirmRemove = false }

    fun removeSelected() {
        val pkgs = selected.toList()
        if (busy || state != ShizukuState.READY || pkgs.isEmpty()) return
        busy = true
        viewModelScope.launch {
            val (ok, failed) = withContext(Dispatchers.IO) {
                var ok = 0
                var failed = 0
                pkgs.forEach { if (debloater.remove(it, Group.SAFE).isSuccess) ok++ else failed++ }
                ok to failed
            }
            message = text(R.string.result_removed_failed, ok, failed)
            confirmRemove = false
            refreshLog()
            reload()
            busy = false
        }
    }

    fun restore(pkg: String) {
        if (busy) return
        busy = true
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) { debloater.restore(pkg).isSuccess }
            message = text(if (ok) R.string.result_restored else R.string.result_restore_failed)
            refreshLog()
            reload()
            busy = false
        }
    }

    fun restoreAll() {
        val pending = log.pending().map { it.pkg }
        if (busy || state != ShizukuState.READY || pending.isEmpty()) return
        busy = true
        viewModelScope.launch {
            val (ok, failed) = withContext(Dispatchers.IO) {
                var ok = 0
                var failed = 0
                debloater.restoreAll().forEach { if (it.second.isSuccess) ok++ else failed++ }
                ok to failed
            }
            message = text(R.string.result_restored_failed, ok, failed)
            refreshLog()
            reload()
            busy = false
        }
    }

    private fun refreshLog() {
        logEntries = log.entries()
    }
}
