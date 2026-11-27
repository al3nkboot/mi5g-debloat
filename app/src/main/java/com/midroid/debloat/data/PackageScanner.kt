package com.midroid.debloat.data

import android.content.Context
import android.content.pm.ApplicationInfo
import androidx.annotation.StringRes

data class AppItem(
    val pkg: String,
    val label: String,
    val tier: Tier,
    val isSystem: Boolean,
    @StringRes val reason: Int?,
)

fun isSystemApp(flags: Int): Boolean =
    (flags and ApplicationInfo.FLAG_SYSTEM) != 0 ||
        (flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0

fun displayLabel(label: String, pkg: String): String = label.trim().ifEmpty { pkg }

fun classify(pkg: String, catalog: OemCatalog, selected: OemId): Pair<Tier, Int?> =
    tierOf(pkg, catalog, selected) to reasonOf(pkg, catalog, selected)?.let(::reasonResource)

fun scan(context: Context, catalog: OemCatalog, selected: OemId): List<AppItem> {
    val pm = context.packageManager
    val selfPkg = context.packageName
    return pm.getInstalledApplications(0)
        .filter { onInstalledList(isSystemApp(it.flags), it.packageName, selfPkg) }
        .map { app ->
            val (tier, reason) = classify(app.packageName, catalog, selected)
            AppItem(
                app.packageName,
                app.loadLabel(pm).toString(),
                tier,
                isSystem = true,
                reason = reason,
            )
        }
        .sortedBy { it.label }
}
