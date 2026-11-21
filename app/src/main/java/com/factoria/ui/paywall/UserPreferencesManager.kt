package com.factoria.ui.paywall

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class UserPreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("factoria_user_prefs", Context.MODE_PRIVATE)

    private val _isAdFreeUnlocked = MutableStateFlow(prefs.getBoolean(KEY_AD_FREE_UNLOCKED, false))
    val isAdFreeUnlocked: StateFlow<Boolean> = _isAdFreeUnlocked.asStateFlow()

    private val _favoriteItemIds = MutableStateFlow(prefs.getStringSet(KEY_FAVORITE_ITEMS, emptySet())?.toSet() ?: emptySet())
    val favoriteItemIds: StateFlow<Set<String>> = _favoriteItemIds.asStateFlow()

    fun unlockAdFree() {
        prefs.edit().putBoolean(KEY_AD_FREE_UNLOCKED, true).apply()
        _isAdFreeUnlocked.value = true
    }

    fun resetAdFree() {
        prefs.edit().putBoolean(KEY_AD_FREE_UNLOCKED, false).apply()
        _isAdFreeUnlocked.value = false
    }

    fun toggleFavorite(itemId: String) {
        val current = _favoriteItemIds.value.toMutableSet()
        if (current.contains(itemId)) {
            current.remove(itemId)
        } else {
            current.add(itemId)
        }
        prefs.edit().putStringSet(KEY_FAVORITE_ITEMS, current).apply()
        _favoriteItemIds.value = current
    }

    fun isFavorite(itemId: String): Boolean {
        return _favoriteItemIds.value.contains(itemId)
    }

    companion object {
        private const val KEY_AD_FREE_UNLOCKED = "is_ad_free_unlocked"
        private const val KEY_FAVORITE_ITEMS = "favorite_items_set"

        @Volatile
        private var INSTANCE: UserPreferencesManager? = null

        fun getInstance(context: Context): UserPreferencesManager {
            return INSTANCE ?: synchronized(this) {
                val instance = UserPreferencesManager(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
