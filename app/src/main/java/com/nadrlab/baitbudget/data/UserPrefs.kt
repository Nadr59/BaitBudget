package com.nadrlab.baitbudget.data

import android.content.Context

class UserPrefs(context: Context) {
    private val prefs = context.getSharedPreferences("baitbudget_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_IS_ADMIN = "is_admin"
        private const val KEY_ADMIN_PASSWORD = "admin_password"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val DEFAULT_PASSWORD = "1234"
    }

    var isAdmin: Boolean
        get() = prefs.getBoolean(KEY_IS_ADMIN, false)
        set(value) = prefs.edit().putBoolean(KEY_IS_ADMIN, value).apply()

    var adminPassword: String
        get() = prefs.getString(KEY_ADMIN_PASSWORD, DEFAULT_PASSWORD) ?: DEFAULT_PASSWORD
        set(value) = prefs.edit().putString(KEY_ADMIN_PASSWORD, value).apply()

    var userName: String
        get() = prefs.getString(KEY_USER_NAME, "") ?: ""
        set(value) = prefs.edit().putString(KEY_USER_NAME, value).apply()

    var isLoggedIn: Boolean
        get() = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        set(value) = prefs.edit().putBoolean(KEY_IS_LOGGED_IN, value).apply()

    val isSetupComplete: Boolean
        get() = userName.isNotBlank() || isAdmin

    fun clear() {
        prefs.edit().clear().apply()
    }
}
