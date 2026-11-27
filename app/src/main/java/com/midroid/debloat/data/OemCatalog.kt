package com.midroid.debloat.data

import android.content.res.AssetManager
import org.json.JSONObject
import java.io.File
import java.util.Locale

enum class OemId { SAMSUNG, XIAOMI, COLOROS, HONOR, VIVO, GOOGLE, ALL }

data class OemEntry(
    val pkg: String,
    val tier: Tier,
    val reason: String,
)

data class OemList(
    val id: OemId,
    val label: String,
    val packages: List<OemEntry>,
)

class OemCatalog(val lists: List<OemList>) {
    constructor(assets: AssetManager) : this(
        FILE_IDS.map { id ->
            parseOemList(assets.open("lists/${id.assetId()}.json").bufferedReader().use { it.readText() })
        },
    )

    constructor(directory: File) : this(
        FILE_IDS.map { id -> parseOemList(File(directory, "${id.assetId()}.json").readText()) },
    )

    private val unsafeEntries: Map<String, List<OemEntry>> = lists.asSequence()
        .flatMap { it.packages.asSequence() }
        .filter { it.tier == Tier.UNSAFE }
        .groupBy { it.pkg }

    init {
        require(lists.map { it.id }.let { it.size == it.toSet().size }) { "Duplicate OEM list id" }
        require(OemId.ALL !in lists.map { it.id }) { "ALL is a selection, not an OEM list" }
    }

    internal fun unsafeEntries(pkg: String): List<OemEntry> = unsafeEntries[pkg].orEmpty()

    internal fun activeEntries(pkg: String, selected: OemId): List<OemEntry> = lists
        .filter { selected == OemId.ALL || it.id == OemId.GOOGLE || it.id == selected }
        .flatMap { list -> list.packages.filter { it.pkg == pkg } }

    companion object {
        private val FILE_IDS = listOf(
            OemId.SAMSUNG,
            OemId.XIAOMI,
            OemId.COLOROS,
            OemId.HONOR,
            OemId.VIVO,
            OemId.GOOGLE,
        )
    }
}

fun OemId.assetId(): String = when (this) {
    OemId.SAMSUNG -> "samsung"
    OemId.XIAOMI -> "xiaomi"
    OemId.COLOROS -> "coloros"
    OemId.HONOR -> "honor"
    OemId.VIVO -> "vivo"
    OemId.GOOGLE -> "google"
    OemId.ALL -> error("ALL does not have an asset file")
}

fun parseOemList(json: String): OemList = try {
    val root = JSONObject(json)
    val id = when (root.getString("id").trim().lowercase(Locale.ROOT)) {
        "samsung" -> OemId.SAMSUNG
        "xiaomi" -> OemId.XIAOMI
        "coloros" -> OemId.COLOROS
        "honor" -> OemId.HONOR
        "vivo" -> OemId.VIVO
        "google" -> OemId.GOOGLE
        else -> throw IllegalArgumentException("Unknown OEM id")
    }
    val packagesJson = root.getJSONArray("packages")
    val seen = mutableSetOf<String>()
    val packages = (0 until packagesJson.length()).map { index ->
        val entry = packagesJson.getJSONObject(index)
        val pkg = entry.getString("pkg")
        require(pkg.isNotBlank()) { "Blank package at index $index" }
        require(seen.add(pkg)) { "Duplicate package in ${id.assetId()}: $pkg" }
        val tier = when (entry.getString("tier")) {
            "RECOMMENDED" -> Tier.RECOMMENDED
            "ADVANCED" -> Tier.ADVANCED
            "EXPERT" -> Tier.EXPERT
            "UNSAFE" -> Tier.UNSAFE
            else -> throw IllegalArgumentException("Unknown tier for $pkg")
        }
        OemEntry(pkg, tier, entry.getString("reason"))
    }
    OemList(id, root.getString("label"), packages)
} catch (e: Exception) {
    throw IllegalArgumentException("Invalid OEM list JSON: ${e.message}", e)
}

fun mapOem(manufacturer: String, brand: String = manufacturer): OemId {
    val values = listOf(manufacturer, brand).map { it.trim().lowercase(Locale.ROOT) }
    fun matches(vararg tokens: String): Boolean = values.any { it in tokens }
    return when {
        matches("samsung") -> OemId.SAMSUNG
        matches("xiaomi", "redmi", "poco") -> OemId.XIAOMI
        matches("oppo", "realme", "oneplus") -> OemId.COLOROS
        matches("honor", "huawei") -> OemId.HONOR
        matches("vivo", "iqoo") -> OemId.VIVO
        else -> OemId.GOOGLE
    }
}
