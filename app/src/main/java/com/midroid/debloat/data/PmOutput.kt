package com.midroid.debloat.data

fun combinedOutput(out: String): String = out.trim()

fun looksLikeFailure(out: String): Boolean =
    Regex("(?i)(^|\\n)\\s*(failure|error)\\b").containsMatchIn(out)

fun uninstallSucceeded(exit: Int, out: String): Boolean =
    exit == 0 && out.contains("Success", ignoreCase = true) && !looksLikeFailure(out)

fun installExistingSucceeded(exit: Int, out: String): Boolean =
    exit == 0 && !looksLikeFailure(out) &&
        (out.contains("installed for user", ignoreCase = true) || out.contains("Success", ignoreCase = true))

fun enableOrDisableSucceeded(exit: Int, out: String): Boolean =
    exit == 0 && !looksLikeFailure(out)

fun formatDetail(exit: Int, out: String): String {
    val body = combinedOutput(out).replace(Regex("\\s+"), " ").take(400)
    return "exit $exit: $body"
}

fun isPackageMissing(out: String): Boolean =
    out.contains("NameNotFoundException", ignoreCase = true) ||
        out.contains("doesn't exist", ignoreCase = true) ||
        out.contains("does not exist", ignoreCase = true)
