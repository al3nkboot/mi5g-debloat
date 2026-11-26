package com.midroid.debloat.data

import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class LogEntry(val pkg: String, val timeMillis: Long, val restored: Boolean)

class RemovalLog(private val file: File) {
    fun entries(): List<LogEntry> {
        if (!file.exists()) return emptyList()
        return try {
            val array = JSONArray(file.readText())
            (0 until array.length()).mapNotNull { i ->
                val obj = array.getJSONObject(i)
                val pkg = obj.optString("pkg")
                if (pkg.isEmpty()) null else LogEntry(
                    pkg,
                    obj.optLong("timeMillis"),
                    obj.optBoolean("restored"),
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun add(pkg: String, now: Long = System.currentTimeMillis()) {
        val kept = entries().filterNot { it.pkg == pkg }
        write(kept + LogEntry(pkg, now, false))
    }

    fun markRestored(pkg: String) {
        val updated = entries().map { if (it.pkg == pkg) it.copy(restored = true) else it }
        write(updated)
    }

    fun pending(): List<LogEntry> = entries().filterNot { it.restored }

    private fun write(entries: List<LogEntry>) {
        val array = JSONArray()
        entries.forEach {
            array.put(JSONObject().put("pkg", it.pkg).put("timeMillis", it.timeMillis).put("restored", it.restored))
        }
        // Write to a temp file and rename so a crash cannot leave a half-written log.
        val tmp = File.createTempFile("removal_log", ".tmp", file.parentFile)
        try {
            tmp.writeText(array.toString())
            if (!tmp.renameTo(file)) {
                file.delete()
                tmp.renameTo(file)
            }
        } finally {
            tmp.delete()
        }
    }
}
