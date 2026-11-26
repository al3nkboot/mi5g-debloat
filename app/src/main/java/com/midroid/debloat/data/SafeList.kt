package com.midroid.debloat.data

import androidx.annotation.StringRes
import com.midroid.debloat.R

data class SafeEntry(val pkg: String, @StringRes val reason: Int)

val SAFE_LIST: List<SafeEntry> = listOf(
    SafeEntry("com.glance.internet", R.string.reason_glance),
    SafeEntry("com.facebook.appmanager", R.string.reason_fb_appmanager),
    SafeEntry("com.facebook.services", R.string.reason_fb_services),
    SafeEntry("com.facebook.system", R.string.reason_fb_system),
    SafeEntry("com.facebook.katana", R.string.reason_fb_katana),
    SafeEntry("com.miui.analytics", R.string.reason_miui_analytics),
    SafeEntry("com.miui.msa.global", R.string.reason_miui_msa),
    SafeEntry("com.miui.bugreport", R.string.reason_miui_bugreport),
    SafeEntry("com.xiaomi.glgm", R.string.reason_xiaomi_glgm),
    SafeEntry("com.heytap.browser", R.string.reason_heytap_browser),
    SafeEntry("com.nearme.gamecenter", R.string.reason_nearme_gamecenter),
    SafeEntry("com.samsung.android.bixby.agent", R.string.reason_bixby),
    SafeEntry("com.samsung.android.app.spage", R.string.reason_spage),
    SafeEntry("com.samsung.android.rubin.app", R.string.reason_rubin),
    SafeEntry("com.sec.android.diagmonagent", R.string.reason_diagmon),
    SafeEntry("com.microsoft.appmanager", R.string.reason_ms_appmanager),
    SafeEntry("com.netflix.partner.activation", R.string.reason_netflix_activation),
)
