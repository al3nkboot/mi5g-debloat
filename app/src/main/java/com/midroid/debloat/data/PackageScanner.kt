package com.midroid.debloat.data

import android.content.Context
import android.content.pm.ApplicationInfo
import androidx.annotation.StringRes
import com.midroid.debloat.R

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

fun scan(context: Context): List<AppItem> {
    val pm = context.packageManager
    val selfPkg = context.packageName
    val reasons = (SAFE_LIST + ADVANCED_LIST + EXPERT_LIST).associate { it.pkg to it.reason }
    return pm.getInstalledApplications(0)
        .filter { onInstalledList(isSystemApp(it.flags), it.packageName, selfPkg) }
        .map { app ->
            val tier = tierOf(app.packageName)
            AppItem(
                app.packageName,
                app.loadLabel(pm).toString(),
                tier,
                isSystem = true,
                reason = if (tier == Tier.UNSAFE) R.string.reason_unsafe else reasons[app.packageName],
            )
        }
        .sortedBy { it.label }
}
