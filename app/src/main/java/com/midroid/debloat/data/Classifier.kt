package com.midroid.debloat.data

enum class Tier { RECOMMENDED, ADVANCED, EXPERT, UNSAFE, UNKNOWN }

val SAFE_PKGS: Set<String> = SAFE_LIST.map { it.pkg }.toSet()

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
)

// An entry ending in "." covers that exact entry and its subpackages; others match exactly.
fun isUnsafe(pkg: String): Boolean = DENY_PREFIXES.any {
    if (it.endsWith(".")) pkg == it || pkg.startsWith(it) else pkg == it
}

fun tierOf(pkg: String, safe: Set<String> = SAFE_PKGS): Tier = when {
    isUnsafe(pkg) -> Tier.UNSAFE
    pkg in EXPERT_PKGS -> Tier.EXPERT
    pkg in ADVANCED_PKGS -> Tier.ADVANCED
    pkg in safe -> Tier.RECOMMENDED
    else -> Tier.UNKNOWN
}

fun maySelect(tier: Tier, allowUnsafe: Boolean): Boolean = when (tier) {
    Tier.RECOMMENDED, Tier.ADVANCED -> true
    Tier.EXPERT, Tier.UNKNOWN, Tier.UNSAFE -> allowUnsafe
}

fun defaultAction(tier: Tier): LogAction =
    if (tier == Tier.UNSAFE) LogAction.DISABLED else LogAction.UNINSTALLED

fun onInstalledList(isSystem: Boolean, pkg: String, selfPkg: String): Boolean =
    isSystem && pkg != selfPkg
