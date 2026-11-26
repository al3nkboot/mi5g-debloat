package com.midroid.debloat.data

enum class Group { SAFE, USER, PROTECTED }

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
)

// A prefix ending in "." covers its subpackages; every other entry matches only its exact name.
private fun isDenied(pkg: String): Boolean = DENY_PREFIXES.any {
    if (it.endsWith(".")) pkg == it || pkg.startsWith(it) else pkg == it
}

fun classify(
    pkg: String,
    isSystem: Boolean,
    selfPkg: String,
    safe: Set<String> = SAFE_PKGS,
): Group = when {
    pkg == selfPkg -> Group.PROTECTED
    isDenied(pkg) -> Group.PROTECTED
    pkg in safe -> Group.SAFE
    !isSystem -> Group.USER
    else -> Group.PROTECTED
}
