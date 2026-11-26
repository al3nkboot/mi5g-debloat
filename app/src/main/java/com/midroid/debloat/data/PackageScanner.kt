package com.midroid.debloat.data

import android.content.Context
import android.content.pm.ApplicationInfo
import androidx.annotation.StringRes

data class AppItem(val pkg: String, val label: String, val group: Group, @StringRes val reason: Int?)

fun scan(context: Context): List<AppItem> {
    val pm = context.packageManager
    val selfPkg = context.packageName
    val reasons = SAFE_LIST.associate { it.pkg to it.reason }
    return pm.getInstalledApplications(0)
        .map { app ->
            val isSystem = (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            AppItem(
                app.packageName,
                app.loadLabel(pm).toString(),
                classify(app.packageName, isSystem, selfPkg),
                reasons[app.packageName],
            )
        }
        .sortedBy { it.label }
}
