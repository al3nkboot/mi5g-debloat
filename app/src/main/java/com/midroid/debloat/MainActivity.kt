@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.midroid.debloat

import android.app.Activity
import android.app.Application
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.midroid.debloat.data.AppItem
import com.midroid.debloat.data.LogAction
import com.midroid.debloat.data.LogEntry
import com.midroid.debloat.data.Tier
import com.midroid.debloat.data.maySelect
import com.midroid.debloat.data.tierOf
import com.midroid.debloat.shizuku.ShizukuState
import com.midroid.debloat.ui.HomeTab
import com.midroid.debloat.ui.MainViewModel
import com.midroid.debloat.ui.Screen
import java.text.DateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        val config = Configuration(newBase.resources.configuration)
        config.setLocale(Locale.forLanguageTag(currentLanguageTag()))
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

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

private fun openPlayStore(context: Context, pkg: String) {
    val market = "market://details?id=$pkg"
    val web = "https://play.google.com/store/apps/details?id=$pkg"
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(market)))
    } catch (e: ActivityNotFoundException) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(web)))
    }
}

private fun currentLanguageTag(): String =
    if (AppCompatDelegate.getApplicationLocales().toLanguageTags().startsWith("en")) "en" else "ar"

private fun chooseLanguage(context: Context, tag: String) {
    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
    var base = context
    while (base is ContextWrapper) base = base.baseContext
    (base as? Activity)?.recreate()
}

@Composable
private fun LanguageChoice(label: String, selected: Boolean, onClick: () -> Unit) {
    TextButton(onClick = onClick) {
        Text(
            label,
            color = if (selected) MaterialTheme.colorScheme.primary else Color.Unspecified,
        )
    }
}

@Composable
private fun LanguageRow() {
    val context = LocalContext.current
    var current by remember { mutableStateOf(currentLanguageTag()) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(R.string.language_title), style = MaterialTheme.typography.titleSmall)
        LanguageChoice(stringResource(R.string.language_arabic), current == "ar") {
            chooseLanguage(context, "ar")
            current = "ar"
        }
        LanguageChoice(stringResource(R.string.language_english), current == "en") {
            chooseLanguage(context, "en")
            current = "en"
        }
    }
}

@Composable
private fun LanguageMenu() {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }
    var current by remember { mutableStateOf(currentLanguageTag()) }
    TextButton(onClick = { expanded = true }) { Text(stringResource(R.string.language_title)) }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        DropdownMenuItem(
            text = {
                Text(
                    stringResource(R.string.language_arabic),
                    color = if (current == "ar") MaterialTheme.colorScheme.primary else Color.Unspecified,
                )
            },
            onClick = {
                chooseLanguage(context, "ar")
                current = "ar"
                expanded = false
            },
        )
        DropdownMenuItem(
            text = {
                Text(
                    stringResource(R.string.language_english),
                    color = if (current == "en") MaterialTheme.colorScheme.primary else Color.Unspecified,
                )
            },
            onClick = {
                chooseLanguage(context, "en")
                current = "en"
                expanded = false
            },
        )
    }
}

@Composable
fun MainScreen(vm: MainViewModel = viewModel()) {
    Scaffold { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            when (vm.screen) {
                Screen.HOME -> HomeScreen(vm)
                Screen.SETTINGS -> SettingsScreen(vm)
                Screen.ABOUT -> AboutScreen(vm)
            }
        }
    }
    BackHandler(enabled = vm.screen != Screen.HOME) { vm.showHome() }
}

@Composable
private fun HomeScreen(vm: MainViewModel) {
    var query by remember { mutableStateOf("") }
    var tierFilter by remember { mutableStateOf<Tier?>(null) }
    val labels = remember(vm.items) { vm.items.associate { it.pkg to it.label } }
    val visibleInstalled = vm.items.filter {
        matchesQuery(it.label, it.pkg, query) && (tierFilter == null || it.tier == tierFilter)
    }
    val visibleLog = vm.logEntries.filter {
        val label = labels[it.pkg] ?: it.pkg
        matchesQuery(label, it.pkg, query) && (tierFilter == null || tierOf(it.pkg) == tierFilter)
    }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(stringResource(R.string.app_name)) },
            actions = {
                LanguageMenu()
                TextButton(onClick = vm::showSettings) { Text(stringResource(R.string.settings)) }
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
        vm.homeMessage?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(horizontal = 16.dp))
        }
        SecondaryTabRow(selectedTabIndex = if (vm.homeTab == HomeTab.INSTALLED) 0 else 1) {
            Tab(
                selected = vm.homeTab == HomeTab.INSTALLED,
                onClick = vm::showInstalled,
                text = { Text(stringResource(R.string.tab_installed, vm.items.size)) },
            )
            Tab(
                selected = vm.homeTab == HomeTab.UNINSTALLED,
                onClick = vm::showUninstalled,
                text = { Text(stringResource(R.string.tab_uninstalled, vm.logEntries.size)) },
            )
        }
        ListControls(
            query = query,
            onQueryChange = { query = it },
            tier = tierFilter,
            onTierChange = { tierFilter = it },
        )
        when (vm.homeTab) {
            HomeTab.INSTALLED -> InstalledTab(vm, visibleInstalled, Modifier.weight(1f))
            HomeTab.UNINSTALLED -> UninstalledTab(vm, visibleLog, labels, Modifier.weight(1f))
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

private fun matchesQuery(label: String, pkg: String, query: String): Boolean =
    query.isBlank() || label.contains(query, ignoreCase = true) || pkg.contains(query, ignoreCase = true)

private fun tierLabelRes(tier: Tier): Int = when (tier) {
    Tier.RECOMMENDED -> R.string.tier_recommended
    Tier.ADVANCED -> R.string.tier_advanced
    Tier.EXPERT -> R.string.tier_expert
    Tier.UNSAFE -> R.string.tier_unsafe
    Tier.UNKNOWN -> R.string.tier_unknown
}

@Composable
private fun ListControls(
    query: String,
    onQueryChange: (String) -> Unit,
    tier: Tier?,
    onTierChange: (Tier?) -> Unit,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        label = { Text(stringResource(R.string.search)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
    )
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
        FilterChip(
            selected = tier == null,
            onClick = { onTierChange(null) },
            label = { Text(stringResource(R.string.filter_all)) },
        )
        Tier.values().forEach {
            Spacer(Modifier.width(8.dp))
            FilterChip(
                selected = tier == it,
                onClick = { onTierChange(it) },
                label = { Text(stringResource(tierLabelRes(it))) },
            )
        }
    }
}

@Composable
private fun PackageName(pkg: String) {
    Text(
        "\u2066$pkg\u2069",
        fontSize = 12.sp,
        style = MaterialTheme.typography.bodySmall.copy(
            fontFamily = FontFamily.Monospace,
            textDirection = TextDirection.Ltr,
        ),
    )
}

@Composable
private fun TierBadge(tier: Tier) {
    val background = when (tier) {
        Tier.RECOMMENDED -> Color(0xFF2E7D32)
        Tier.ADVANCED -> Color(0xFFF9A825)
        Tier.EXPERT -> Color(0xFFE65100)
        Tier.UNSAFE -> Color(0xFFC62828)
        Tier.UNKNOWN -> Color.Transparent
    }
    val content = when (tier) {
        Tier.ADVANCED -> Color(0xFF3E2723)
        Tier.UNKNOWN -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> Color.White
    }
    Surface(
        color = background,
        contentColor = content,
        shape = MaterialTheme.shapes.small,
        border = if (tier == Tier.UNKNOWN) BorderStroke(1.dp, Color.Gray) else null,
    ) {
        Text(
            stringResource(tierLabelRes(tier)),
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}

@Composable
private fun SystemChip() {
    Surface(
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Text(
            stringResource(R.string.chip_system),
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}

@Composable
private fun InstalledTab(vm: MainViewModel, visible: List<AppItem>, modifier: Modifier) {
    val selectable = visible.filter { maySelect(it.tier, vm.allowUnsafe) }
    val allSelected = selectable.isNotEmpty() && selectable.all { it.pkg in vm.selected }
    Column(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 16.dp)) {
            Checkbox(
                checked = allSelected,
                onCheckedChange = { vm.selectAll(it, visible) },
                enabled = selectable.isNotEmpty(),
            )
            Text(stringResource(R.string.select_all))
        }
        LazyColumn(Modifier.weight(1f)) {
            items(visible, key = { it.pkg }) { item ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                ) {
                    Checkbox(
                        checked = item.pkg in vm.selected,
                        onCheckedChange = { vm.toggle(item.pkg) },
                        enabled = maySelect(item.tier, vm.allowUnsafe),
                    )
                    Column(Modifier.weight(1f).padding(vertical = 6.dp)) {
                        Text(item.label)
                        PackageName(item.pkg)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TierBadge(item.tier)
                            SystemChip()
                        }
                        item.reason?.let { Text(stringResource(it), style = MaterialTheme.typography.bodySmall) }
                    }
                }
                HorizontalDivider()
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
}

@Composable
private fun ConfirmDialog(vm: MainViewModel) {
    val selectedItems = vm.items.filter { it.pkg in vm.selected }
    val chosen = selectedItems.take(8)
    val extra = (vm.selected.size - chosen.size).coerceAtLeast(0)
    val risk = selectedItems.any { it.tier == Tier.EXPERT || it.tier == Tier.UNKNOWN || it.tier == Tier.UNSAFE }
    val advanced = !risk && selectedItems.any { it.tier == Tier.ADVANCED }
    AlertDialog(
        onDismissRequest = vm::dismissRemove,
        title = {
            Text(stringResource(if (risk) R.string.confirm_risk_title else R.string.confirm_title))
        },
        text = {
            Column {
                val message = when {
                    risk -> stringResource(R.string.confirm_risk_body)
                    advanced -> stringResource(R.string.confirm_advanced)
                    else -> stringResource(R.string.confirm_message, vm.selected.size)
                }
                Text(message)
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
private fun UninstalledTab(
    vm: MainViewModel,
    entries: List<LogEntry>,
    labels: Map<String, String>,
    modifier: Modifier,
) {
    val context = LocalContext.current
    val pending = vm.logEntries.count { !it.restored }
    Column(modifier) {
        Button(
            onClick = vm::restoreAll,
            enabled = vm.state == ShizukuState.READY && pending > 0 && !vm.busy,
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        ) { Text(stringResource(R.string.restore_all)) }
        vm.logMessage?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(horizontal = 16.dp))
        }
        if (vm.logEntries.isEmpty()) {
            Text(stringResource(R.string.log_empty), modifier = Modifier.padding(16.dp))
        }
        LazyColumn(Modifier.weight(1f)) {
            items(entries, key = { "${it.pkg}-${it.timeMillis}" }) { entry ->
                val date = remember(entry.timeMillis) {
                    DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(entry.timeMillis))
                }
                val status = stringResource(
                    when {
                        entry.restored -> R.string.restored
                        entry.action == LogAction.DISABLED -> R.string.disabled
                        else -> R.string.removed
                    }
                )
                val label = labels[entry.pkg] ?: entry.pkg
                Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text(stringResource(R.string.log_entry, label, date, status))
                    if (!entry.restored) {
                        Button(onClick = { vm.restore(entry.pkg) }, enabled = !vm.busy) {
                            Text(stringResource(R.string.restore))
                        }
                    }
                    if (entry.pkg in vm.logFailedPkgs) {
                        if (entry.pkg in vm.logMissingPkgs) {
                            Text(
                                stringResource(R.string.restore_not_on_system, label),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        } else {
                            Text(
                                stringResource(R.string.restore_play_hint),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        Button(onClick = { openPlayStore(context, entry.pkg) }) {
                            Text(stringResource(R.string.open_play_store))
                        }
                    }
                }
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun SettingsScreen(vm: MainViewModel) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        TopAppBar(
            title = { Text(stringResource(R.string.settings)) },
            navigationIcon = { TextButton(onClick = vm::showHome) { Text(stringResource(R.string.back)) } },
        )
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(
                stringResource(R.string.allow_unsafe),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            Switch(checked = vm.allowUnsafe, onCheckedChange = vm::updateAllowUnsafe)
        }
        Text(
            stringResource(R.string.allow_unsafe_explain),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp),
        )
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
        LanguageRow()
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
