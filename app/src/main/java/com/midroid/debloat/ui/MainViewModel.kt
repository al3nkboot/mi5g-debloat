package com.midroid.debloat.ui

import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.midroid.debloat.R
import com.midroid.debloat.data.ActionResult
import com.midroid.debloat.data.AppItem
import com.midroid.debloat.data.Debloater
import com.midroid.debloat.data.Group
import com.midroid.debloat.data.LogEntry
import com.midroid.debloat.data.Outcome
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
import java.util.Locale

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
    var homeMessage by mutableStateOf<String?>(null)
        private set
    var logMessage by mutableStateOf<String?>(null)
        private set
    var logFailedPkgs by mutableStateOf<List<String>>(emptyList())
        private set
    var logEntries by mutableStateOf<List<LogEntry>>(emptyList())
        private set
    var confirmRemove by mutableStateOf(false)
        private set

    private val binderReceived = Shizuku.OnBinderReceivedListener { refresh() }
    private val binderDead = Shizuku.OnBinderDeadListener { refresh() }
    private val permissionResult = Shizuku.OnRequestPermissionResultListener { _, grant ->
        if (grant != PackageManager.PERMISSION_GRANTED) homeMessage = text(R.string.permission_denied)
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

    // Compose's stringResource follows the app locale; getString on the plain application
    // context does not. Build a configuration context for the same locale in the ViewModel.
    private fun localized(): Context {
        val app = getApplication<Application>()
        val tags = AppCompatDelegate.getApplicationLocales().toLanguageTags()
        val tag = if (tags.isEmpty()) "ar" else tags.substringBefore(",")
        val config = Configuration(app.resources.configuration)
        config.setLocale(Locale.forLanguageTag(tag))
        return app.createConfigurationContext(config)
    }

    private fun text(res: Int, vararg args: Any): String = localized().getString(res, *args)

    private fun count(res: Int, n: Int): String = localized().resources.getQuantityString(res, n, n)

    private fun label(pkg: String): String = items.firstOrNull { it.pkg == pkg }?.label ?: pkg

    fun refresh() {
        state = shizukuState(getApplication())
    }

    fun requestPermission() = requestShizukuPermission(PERMISSION_CODE)

    fun showHome() { screen = Screen.HOME }
    fun showLog() {
        screen = Screen.LOG
        refreshLog()
        logMessage = null
        logFailedPkgs = emptyList()
    }
    fun showAbout() { screen = Screen.ABOUT }

    private fun reload() {
        items = scanApps(getApplication())
        selected = emptySet()
    }

    fun scan() {
        reload()
        homeMessage = count(R.plurals.scanned_count, items.size)
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
            val results = withContext(Dispatchers.IO) { pkgs.map { debloater.remove(it, Group.SAFE) } }
            val parts = mutableListOf<String>()
            val removed = results.count { it.outcome == Outcome.UNINSTALLED }
            val disabled = results.count { it.outcome == Outcome.DISABLED }
            val failed = results.count { it.outcome == Outcome.FAILED }
            if (removed > 0) parts += count(R.plurals.removed_count, removed)
            if (disabled > 0) parts += count(R.plurals.disabled_count, disabled)
            if (failed > 0) parts += count(R.plurals.failed_count, failed)
            results.forEach { result ->
                parts += when (result.outcome) {
                    Outcome.UNINSTALLED -> text(R.string.result_pkg_uninstalled, label(result.pkg))
                    Outcome.DISABLED -> text(R.string.result_pkg_disabled, label(result.pkg), result.detail)
                    Outcome.FAILED -> text(R.string.result_pkg_failed, label(result.pkg), result.detail)
                    Outcome.RESTORED -> text(R.string.result_pkg_restored, label(result.pkg))
                }
            }
            homeMessage = parts.joinToString("\n")
            confirmRemove = false
            refreshLog()
            reload()
            busy = false
        }
    }

    private fun restoreMessage(results: List<ActionResult>): String {
        val parts = mutableListOf<String>()
        val restored = results.count { it.outcome == Outcome.RESTORED }
        val failed = results.count { it.outcome == Outcome.FAILED }
        if (restored > 0) parts += count(R.plurals.restored_count, restored)
        if (failed > 0) parts += count(R.plurals.failed_count, failed)
        results.forEach { result ->
            parts += when (result.outcome) {
                Outcome.RESTORED -> text(R.string.result_pkg_restored, label(result.pkg))
                Outcome.FAILED -> text(R.string.result_pkg_restore_failed, label(result.pkg), result.detail)
                else -> text(R.string.result_pkg_failed, label(result.pkg), result.detail)
            }
        }
        return parts.joinToString("\n")
    }

    fun restore(pkg: String) {
        if (busy) return
        busy = true
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { debloater.restore(pkg) }
            logMessage = restoreMessage(listOf(result))
            logFailedPkgs = if (result.outcome == Outcome.FAILED) listOf(pkg) else emptyList()
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
            val results = withContext(Dispatchers.IO) { debloater.restoreAll() }
            logMessage = restoreMessage(results)
            logFailedPkgs = results.filter { it.outcome == Outcome.FAILED }.map { it.pkg }
            refreshLog()
            reload()
            busy = false
        }
    }

    private fun refreshLog() {
        logEntries = log.entries()
    }
}
