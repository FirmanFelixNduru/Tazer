package com.tazer.taskorganizer.data

import android.content.Context
import com.tazer.taskorganizer.domain.CourseColors
import org.json.JSONObject

/**
 * Per-account course colour choices. Courses without a choice use the automatic palette slot, so only overrides
 * are stored. shortcut: kept on the device and in the JSON backup, not in Cloud Sync; syncing would need a new
 * Supabase table and RLS migration.
 */
class CourseColorStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences("course_colors", Context.MODE_PRIVATE)

    fun read(uid: String): Map<String, Int> = decode(preferences.getString(uid, null))

    fun save(uid: String, overrides: Map<String, Int>): Boolean =
        preferences.edit().putString(uid, encode(overrides)).commit()

    /** Sets one course to [index], or back to automatic when [index] is null. */
    fun choose(uid: String, course: String, index: Int?): Map<String, Int> {
        val next = read(uid).toMutableMap()
        val key = CourseColors.key(course)
        if (index == null || index == CourseColors.automaticIndex(course)) next.remove(key) else next[key] = index
        save(uid, next)
        return next
    }

    internal companion object {
        fun encode(overrides: Map<String, Int>): String = JSONObject(overrides.toSortedMap()).toString()

        fun decode(text: String?): Map<String, Int> = runCatching {
            val json = JSONObject(text ?: return emptyMap())
            json.keys().asSequence().mapNotNull { key ->
                (json.opt(key) as? Int)?.takeIf { it in CourseColors.palette.indices && key.isNotBlank() }?.let { key to it }
            }.toMap()
        }.getOrDefault(emptyMap())
    }
}
