package com.example.swiftlab.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.swiftlab.BuildConfig
import java.util.UUID

class SessionManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("swiftlab_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_TOKEN = "jwt_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_ROLE = "user_role"
        private const val KEY_ORG_ID = "org_id"
        private const val KEY_ORG_NAME = "org_name"
        private const val KEY_CURRENCY_SYMBOL = "currency_symbol"
        private const val KEY_DATA_MODE = "data_mode" // "LOCAL_ONLY" or "CLOUD_SYNC"
        private const val KEY_BASE_URL = "base_url"
        private const val KEY_DEVICE_ID = "device_id"

        const val MODE_LOCAL_ONLY = "LOCAL_ONLY"
        const val MODE_CLOUD_SYNC = "CLOUD_SYNC"
    }

    var token: String?
        get() = prefs.getString(KEY_TOKEN, null)
        set(value) = prefs.edit().putString(KEY_TOKEN, value).apply()

    var userId: Long
        get() = prefs.getLong(KEY_USER_ID, 1L)
        set(value) = prefs.edit().putLong(KEY_USER_ID, value).apply()

    var userEmail: String
        get() = prefs.getString(KEY_USER_EMAIL, "admin@swiftlab.local") ?: "admin@swiftlab.local"
        set(value) = prefs.edit().putString(KEY_USER_EMAIL, value).apply()

    var userName: String
        get() = prefs.getString(KEY_USER_NAME, "Administrator") ?: "Administrator"
        set(value) = prefs.edit().putString(KEY_USER_NAME, value).apply()

    var role: String
        get() = prefs.getString(KEY_ROLE, "OWNER") ?: "OWNER"
        set(value) = prefs.edit().putString(KEY_ROLE, value).apply()

    var orgId: Long
        get() = prefs.getLong(KEY_ORG_ID, 1L)
        set(value) = prefs.edit().putLong(KEY_ORG_ID, value).apply()

    var orgName: String
        get() = prefs.getString(KEY_ORG_NAME, "SwiftLab Demo Org") ?: "SwiftLab Demo Org"
        set(value) = prefs.edit().putString(KEY_ORG_NAME, value).apply()

    var currencySymbol: String
        get() = prefs.getString(KEY_CURRENCY_SYMBOL, "₹") ?: "₹"
        set(value) = prefs.edit().putString(KEY_CURRENCY_SYMBOL, value).apply()

    var dataMode: String
        get() = prefs.getString(KEY_DATA_MODE, MODE_LOCAL_ONLY) ?: MODE_LOCAL_ONLY
        set(value) = prefs.edit().putString(KEY_DATA_MODE, value).apply()

    var baseUrl: String
        get() = prefs.getString(KEY_BASE_URL, BuildConfig.BASE_URL) ?: BuildConfig.BASE_URL
        set(value) {
            val url = if (value.endsWith("/")) value else "$value/"
            prefs.edit().putString(KEY_BASE_URL, url).apply()
        }

    val deviceId: String
        get() {
            var id = prefs.getString(KEY_DEVICE_ID, null)
            if (id == null) {
                id = "AND-" + UUID.randomUUID().toString().substring(0, 8).uppercase()
                prefs.edit().putString(KEY_DEVICE_ID, id).apply()
            }
            return id
        }

    val isLoggedIn: Boolean
        get() = !token.isNullOrBlank()

    val isLocalOnly: Boolean
        get() = dataMode == MODE_LOCAL_ONLY

    val isCloudSync: Boolean
        get() = dataMode == MODE_CLOUD_SYNC

    fun saveLogin(token: String, userId: Long, email: String, name: String, role: String, orgId: Long, orgName: String) {
        prefs.edit()
            .putString(KEY_TOKEN, token)
            .putLong(KEY_USER_ID, userId)
            .putString(KEY_USER_EMAIL, email)
            .putString(KEY_USER_NAME, name)
            .putString(KEY_ROLE, role)
            .putLong(KEY_ORG_ID, orgId)
            .putString(KEY_ORG_NAME, orgName)
            .apply()
    }

    fun logout() {
        prefs.edit()
            .remove(KEY_TOKEN)
            .remove(KEY_USER_ID)
            .remove(KEY_USER_EMAIL)
            .remove(KEY_USER_NAME)
            .apply()
    }
}
