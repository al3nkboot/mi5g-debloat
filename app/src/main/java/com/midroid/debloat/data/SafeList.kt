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
    SafeEntry("com.heytap.pictorial", R.string.reason_heytap_pictorial),
    SafeEntry("com.heytap.market", R.string.reason_heytap_market),
    SafeEntry("com.oppo.market", R.string.reason_oppo_market),
    SafeEntry("com.nearme.browser", R.string.reason_nearme_browser),
    SafeEntry("com.oplus.games", R.string.reason_oplus_games),
    SafeEntry("com.coloros.gamespaceui", R.string.reason_gamespaceui),
    SafeEntry("com.coloros.gamespace", R.string.reason_gamespace),
    SafeEntry("com.heytap.themestore", R.string.reason_heytap_themestore),
    SafeEntry("com.oplus.themestore", R.string.reason_oplus_themestore),
    SafeEntry("com.nearme.themespace", R.string.reason_nearme_themespace),
    SafeEntry("com.nearme.themestore", R.string.reason_nearme_themestore),
    SafeEntry("com.realmecomm.app", R.string.reason_realme_community),
)

val ADVANCED_LIST: List<SafeEntry> = listOf(
    SafeEntry("com.heytap.mcs", R.string.reason_heytap_mcs),
    SafeEntry("com.heytap.music", R.string.reason_heytap_music),
    SafeEntry("com.heytap.accessory", R.string.reason_heytap_accessory),
    SafeEntry("com.coloros.assistantscreen", R.string.reason_coloros_assistantscreen),
    SafeEntry("com.coloros.oshare", R.string.reason_coloros_oshare),
    SafeEntry("com.coloros.compass2", R.string.reason_coloros_compass2),
    SafeEntry("com.coloros.childrenspace", R.string.reason_coloros_childrenspace),
    SafeEntry("com.coloros.floatassistant", R.string.reason_coloros_floatassistant),
    SafeEntry("com.coloros.ocrscanner", R.string.reason_coloros_ocrscanner),
    SafeEntry("com.coloros.operationManual", R.string.reason_coloros_operationmanual),
    SafeEntry("com.coloros.soundrecorder", R.string.reason_coloros_soundrecorder),
    SafeEntry("com.coloros.phonemanager", R.string.reason_coloros_phonemanager),
    SafeEntry("com.oplus.aiplaymate", R.string.reason_oplus_aiplaymate),
)

val EXPERT_LIST: List<SafeEntry> = listOf(
    SafeEntry("com.heytap.cloud", R.string.reason_heytap_cloud),
    SafeEntry("com.nearme.statistics.rom", R.string.reason_nearme_statistics_rom),
    SafeEntry("com.coloros.pictorial", R.string.reason_coloros_pictorial),
    SafeEntry("com.coloros.smartsidebar", R.string.reason_coloros_smartsidebar),
)

val ADVANCED_PKGS: Set<String> = ADVANCED_LIST.map { it.pkg }.toSet()
val EXPERT_PKGS: Set<String> = EXPERT_LIST.map { it.pkg }.toSet()
