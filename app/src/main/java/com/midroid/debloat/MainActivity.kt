@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.midroid.debloat

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.midroid.debloat.data.AppItem
import com.midroid.debloat.data.Group
import com.midroid.debloat.shizuku.ShizukuState
import com.midroid.debloat.ui.MainViewModel
import com.midroid.debloat.ui.Screen
import java.text.DateFormat
import java.util.Date

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { DebloatTheme { MainScreen() } }
    }
}

@Composable
fun DebloatTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val dark = androidx.compose.foundation.isSystemInDarkTheme()
    val scheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && dark -> dynamicDarkColorScheme(context)
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> dynamicLightColorScheme(context)
        dark -> darkColorScheme()
        else -> lightColorScheme()
    }
    MaterialTheme(colorScheme = scheme, content = content)
}

private fun openUrl(context: Context, res: Int) {
    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(context.getString(res))))
}

@Composable
fun MainScreen(vm: MainViewModel = viewModel()) {
    Scaffold { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            when (vm.screen) {
                Screen.HOME -> HomeScreen(vm)
                Screen.LOG -> LogScreen(vm)
                Screen.ABOUT -> AboutScreen(vm)
            }
        }
    }
    BackHandler(enabled = vm.screen != Screen.HOME) { vm.showHome() }
}

@Composable
private fun HomeScreen(vm: MainViewModel) {
    val tab = remember { mutableIntStateOf(0) }
    val safe = vm.items.filter { it.group == Group.SAFE }
    val user = vm.items.filter { it.group == Group.USER }
    val protected = vm.items.filter { it.group == Group.PROTECTED }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(stringResource(R.string.app_name)) },
            actions = {
                TextButton(onClick = vm::showLog) { Text(stringResource(R.string.nav_log)) }
                TextButton(onClick = vm::showAbout) { Text(stringResource(R.string.nav_about)) }
            },
        )
        StatusCard(vm)
        Text(
            stringResource(R.string.scan_hint),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Button(onClick = vm::scan, modifier = Modifier.padding(16.dp)) {
            Text(stringResource(R.string.scan))
        }
        vm.message?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(horizontal = 16.dp))
        }
        SecondaryTabRow(selectedTabIndex = tab.intValue) {
            Tab(
                selected = tab.intValue == 0,
                onClick = { tab.intValue = 0 },
                text = { Text(stringResource(R.string.tab_safe, safe.size)) },
            )
            Tab(
                selected = tab.intValue == 1,
                onClick = { tab.intValue = 1 },
                text = { Text(stringResource(R.string.tab_user, user.size)) },
            )
            Tab(
                selected = tab.intValue == 2,
                onClick = { tab.intValue = 2 },
                text = { Text(stringResource(R.string.tab_protected, protected.size)) },
            )
        }
        when (tab.intValue) {
            0 -> SafeTab(vm, safe, Modifier.weight(1f))
            1 -> InfoTab(user, R.string.user_info, Modifier.weight(1f))
            else -> InfoTab(protected, R.string.protected_info, Modifier.weight(1f))
        }
    }
    if (vm.confirmRemove) ConfirmDialog(vm)
}

@Composable
private fun StatusCard(vm: MainViewModel) {
    val context = LocalContext.current
    val title = when (vm.state) {
        ShizukuState.NOT_INSTALLED -> stringResource(R.string.shizuku_not_installed)
        ShizukuState.NOT_RUNNING -> stringResource(R.string.shizuku_not_running)
        ShizukuState.NEEDS_PERMISSION -> stringResource(R.string.shizuku_needs_permission)
        ShizukuState.READY -> stringResource(R.string.shizuku_ready)
    }
    val hint = when (vm.state) {
        ShizukuState.NOT_INSTALLED -> stringResource(R.string.shizuku_not_installed_hint)
        ShizukuState.NOT_RUNNING -> stringResource(R.string.shizuku_not_running_hint)
        ShizukuState.NEEDS_PERMISSION -> stringResource(R.string.shizuku_needs_permission_hint)
        ShizukuState.READY -> stringResource(R.string.shizuku_ready_hint)
    }
    Card(Modifier.fillMaxWidth().padding(16.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(hint, style = MaterialTheme.typography.bodySmall)
            when (vm.state) {
                ShizukuState.NOT_INSTALLED -> Button(onClick = {
                    openUrl(context, R.string.url_shizuku_download)
                }) { Text(stringResource(R.string.install_shizuku)) }
                ShizukuState.NOT_RUNNING -> Button(onClick = {
                    val intent = context.packageManager.getLaunchIntentForPackage("moe.shizuku.privileged.api")
                    if (intent != null) context.startActivity(intent) else openUrl(context, R.string.url_shizuku_download)
                }) { Text(stringResource(R.string.open_shizuku)) }
                ShizukuState.NEEDS_PERMISSION -> Button(onClick = vm::requestPermission) {
                    Text(stringResource(R.string.grant_permission))
                }
                ShizukuState.READY -> Text(
                    stringResource(R.string.check_mark),
                    color = Color(0xFF2E7D32),
                    style = MaterialTheme.typography.titleLarge,
                )
            }
        }
    }
}

@Composable
private fun PackageName(pkg: String) {
    Text(
        "\u2066$pkg\u2069",
        fontFamily = FontFamily.Monospace,
        fontSize = 12.sp,
    )
}

@Composable
private fun SafeTab(vm: MainViewModel, safe: List<AppItem>, modifier: Modifier) {
    val allSelected = safe.isNotEmpty() && safe.all { it.pkg in vm.selected }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 16.dp)) {
        Checkbox(checked = allSelected, onCheckedChange = vm::selectAll)
        Text(stringResource(R.string.select_all))
    }
    LazyColumn(modifier) {
        items(safe, key = { it.pkg }) { item ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 16.dp)) {
                Checkbox(checked = item.pkg in vm.selected, onCheckedChange = { vm.toggle(item.pkg) })
                Column(Modifier.weight(1f).padding(vertical = 4.dp)) {
                    Text(item.label)
                    PackageName(item.pkg)
                    item.reason?.let { Text(stringResource(it), style = MaterialTheme.typography.bodySmall) }
                }
            }
        }
    }
    Button(
        onClick = vm::askRemove,
        enabled = vm.state == ShizukuState.READY && vm.selected.isNotEmpty() && !vm.busy,
        modifier = Modifier.fillMaxWidth().padding(16.dp),
    ) {
        Text(stringResource(R.string.remove_selected, vm.selected.size))
    }
}

@Composable
private fun InfoTab(items: List<AppItem>, infoRes: Int, modifier: Modifier) {
    Text(
        stringResource(infoRes),
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
    LazyColumn(modifier) {
        items(items, key = { it.pkg }) { item ->
            Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(item.label)
                PackageName(item.pkg)
            }
            HorizontalDivider()
        }
    }
}

@Composable
private fun ConfirmDialog(vm: MainViewModel) {
    val chosen = vm.items.filter { it.pkg in vm.selected }.take(8)
    val extra = (vm.selected.size - chosen.size).coerceAtLeast(0)
    AlertDialog(
        onDismissRequest = vm::dismissRemove,
        title = { Text(stringResource(R.string.confirm_title)) },
        text = {
            Column {
                Text(stringResource(R.string.confirm_message, vm.selected.size))
                chosen.forEach {
                    Text(it.label)
                    PackageName(it.pkg)
                }
                if (extra > 0) Text(stringResource(R.string.more_items, extra))
            }
        },
        confirmButton = { TextButton(onClick = vm::removeSelected) { Text(stringResource(R.string.confirm)) } },
        dismissButton = { TextButton(onClick = vm::dismissRemove) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
private fun LogScreen(vm: MainViewModel) {
    val labels = remember(vm.items) { vm.items.associate { it.pkg to it.label } }
    val pending = vm.logEntries.count { !it.restored }
    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(stringResource(R.string.log_title)) },
            navigationIcon = { TextButton(onClick = vm::showHome) { Text(stringResource(R.string.back)) } },
        )
        Button(
            onClick = vm::restoreAll,
            enabled = vm.state == ShizukuState.READY && pending > 0 && !vm.busy,
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        ) { Text(stringResource(R.string.restore_all)) }
        vm.message?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(horizontal = 16.dp))
        }
        if (vm.logEntries.isEmpty()) {
            Text(stringResource(R.string.log_empty), modifier = Modifier.padding(16.dp))
        } else {
            LazyColumn(Modifier.weight(1f)) {
                items(vm.logEntries, key = { "${it.pkg}-${it.timeMillis}" }) { entry ->
                    val date = remember(entry.timeMillis) {
                        DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(entry.timeMillis))
                    }
                    val status = stringResource(if (entry.restored) R.string.restored else R.string.removed)
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Text(stringResource(R.string.log_entry, labels[entry.pkg] ?: entry.pkg, date, status))
                        if (!entry.restored) {
                            Button(onClick = { vm.restore(entry.pkg) }, enabled = !vm.busy) {
                                Text(stringResource(R.string.restore))
                            }
                        }
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun AboutScreen(vm: MainViewModel) {
    val context = LocalContext.current
    val version = remember {
        try {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: ""
        } catch (e: Exception) {
            ""
        }
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        TopAppBar(
            title = { Text(stringResource(R.string.nav_about)) },
            navigationIcon = { TextButton(onClick = vm::showHome) { Text(stringResource(R.string.back)) } },
        )
        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium)
        Text(stringResource(R.string.about_version, version))
        Text(stringResource(R.string.about_by))
        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.about_support))
        LinkText(stringResource(R.string.url_paypal))
        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.about_credit))
        LinkText(stringResource(R.string.url_credit))
        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.about_independent))
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.about_disclaimer))
    }
}

@Composable
private fun LinkText(url: String) {
    val context = LocalContext.current
    Text(
        text = "\u2066$url\u2069",
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.bodyMedium.copy(textDirection = TextDirection.Ltr),
        modifier = Modifier.clickable { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) },
    )
}
