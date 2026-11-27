package com.midroid.debloat.data

enum class Tier { RECOMMENDED, ADVANCED, EXPERT, UNSAFE, UNKNOWN }

val DENY_PREFIXES: Set<String> = setOf(
    "android",
    "com.android.systemui",
    "com.android.phone",
    "com.android.settings",
    "com.android.providers.",
    "com.android.vending",
    "com.google.android.gms",
    "com.google.android.gsf",
    "com.android.packageinstaller",
    "com.google.android.packageinstaller",
    "moe.shizuku.privileged.api",
    "com.android.shell",
    "com.android.bluetooth",
    "com.android.nfc",
    "com.android.launcher3",
    "com.google.android.inputmethod",
    "com.android.inputmethod",
    "com.android.launcher",
    "com.oppo.launcher",
    "com.android.internal.systemui.",
    "com.android.incallui",
    "com.android.server.telecom",
    "com.android.dialer",
    "com.google.android.dialer",
    "com.google.android.setupwizard",
    "com.heytap.usercenter",
    "com.oplus.account",
    "com.oplus.camera",
    "com.oplus.cameraextensions",
    "com.oplus.callrecorder",
    "com.oplus.atlas",
    "com.oplus.metis",
    "com.oplus.athena",
    "com.oplus.onetrace",
    "com.oplus.screenrecorder",
    "com.coloros.safecenter",
    "com.oplus.securitypermission",
    "com.oplus.ota",
    "com.oplus.romupdate",
    "com.coloros.bootreg",
    "com.coloros.lockassistant",
    "com.coloros.securepay",
    "com.samsung.android.knox.",
    "com.samsung.knox.",
    "com.sec.enterprise.",
)

// An entry ending in "." covers that exact entry and its subpackages; others match exactly.
fun isUnsafe(pkg: String): Boolean = DENY_PREFIXES.any {
    if (it.endsWith(".")) pkg == it || pkg.startsWith(it) else pkg == it
}

private fun tierRank(tier: Tier): Int = when (tier) {
    Tier.RECOMMENDED -> 1
    Tier.ADVANCED -> 2
    Tier.EXPERT -> 3
    Tier.UNSAFE -> 4
    Tier.UNKNOWN -> 0
}

private fun activeEntry(pkg: String, catalog: OemCatalog, selected: OemId): OemEntry? =
    catalog.activeEntries(pkg, selected).maxWithOrNull(compareBy { tierRank(it.tier) })

fun tierOf(pkg: String, catalog: OemCatalog, selected: OemId): Tier = when {
    isUnsafe(pkg) || catalog.unsafeEntries(pkg).isNotEmpty() -> Tier.UNSAFE
    else -> activeEntry(pkg, catalog, selected)?.tier ?: Tier.UNKNOWN
}

fun reasonOf(pkg: String, catalog: OemCatalog, selected: OemId): String? {
    val unsafe = catalog.unsafeEntries(pkg)
    if (unsafe.isNotEmpty()) return unsafe.firstOrNull { it.reason != "unsafe" }?.reason ?: "unsafe"
    if (isUnsafe(pkg)) return "unsafe"
    return activeEntry(pkg, catalog, selected)?.reason
}

fun maySelect(tier: Tier, allowUnsafe: Boolean): Boolean = when (tier) {
    Tier.RECOMMENDED, Tier.ADVANCED -> true
    Tier.EXPERT, Tier.UNKNOWN, Tier.UNSAFE -> allowUnsafe
}

fun defaultAction(tier: Tier): LogAction =
    if (tier == Tier.UNSAFE) LogAction.DISABLED else LogAction.UNINSTALLED

fun onInstalledList(isSystem: Boolean, pkg: String, selfPkg: String): Boolean =
    isSystem && pkg != selfPkg
