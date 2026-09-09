package com.sigmaapps.callvolumebooster

import android.content.Context

/**
 * Small helper around SharedPreferences for storing the list of VIP
 * contacts (name + normalized number). Stored as "name|number" strings
 * inside a StringSet.
 */
object VipStore {

    private const val PREFS = "vip_prefs"
    private const val KEY_CONTACTS = "vip_contacts"
    private const val KEY_ENABLED = "watcher_enabled"

    data class VipContact(val name: String, val number: String)

    fun isWatcherEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_ENABLED, true)
    }

    fun setWatcherEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    fun normalize(number: String): String = number.filter { it.isDigit() }.takeLast(10)

    fun getAll(context: Context): List<VipContact> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val raw = prefs.getStringSet(KEY_CONTACTS, emptySet()) ?: emptySet()
        return raw.mapNotNull {
            val parts = it.split("|", limit = 2)
            if (parts.size == 2) VipContact(parts[0], parts[1]) else null
        }.sortedBy { it.name }
    }

    fun isAdded(context: Context, number: String): Boolean {
        val normalized = normalize(number)
        return getAll(context).any { it.number == normalized }
    }

    /** Returns true if the contact was newly added (false if it was already there). */
    fun add(context: Context, name: String, number: String): Boolean {
        val normalized = normalize(number)
        if (normalized.isEmpty()) return false
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val current = (prefs.getStringSet(KEY_CONTACTS, emptySet()) ?: emptySet()).toMutableSet()
        if (current.any { it.split("|", limit = 2).getOrNull(1) == normalized }) return false
        current.add("$name|$normalized")
        prefs.edit().putStringSet(KEY_CONTACTS, current).apply()
        return true
    }

    fun remove(context: Context, number: String) {
        val normalized = normalize(number)
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val current = (prefs.getStringSet(KEY_CONTACTS, emptySet()) ?: emptySet()).toMutableSet()
        current.removeAll { it.split("|", limit = 2).getOrNull(1) == normalized }
        prefs.edit().putStringSet(KEY_CONTACTS, current).apply()
    }
}
