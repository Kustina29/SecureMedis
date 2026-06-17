package com.ayu.securemedis

import android.content.Context
import android.content.SharedPreferences

/**
 * SessionManager.kt
 *
 * Menyimpan dan mengelola state login pengguna menggunakan SharedPreferences.
 * Data yang disimpan: user ID, username, dan role (DOKTER/PASIEN/ADMIN).
 *
 * Digunakan oleh semua Activity untuk menentukan siapa yang sedang login
 * dan apa yang boleh mereka akses.
 */
class SessionManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREF_NAME = "securemedis_session"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USERNAME = "username"
        private const val KEY_ROLE = "role"
    }

    /**
     * Menyimpan data sesi setelah login berhasil.
     *
     * @param userId   UUID pengguna dari database Supabase
     * @param username Nama pengguna yang digunakan untuk login
     * @param role     Role pengguna: DOKTER, PASIEN, atau ADMIN
     */
    fun saveLoginSession(userId: String, username: String, role: String) {
        prefs.edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, true)
            putString(KEY_USER_ID, userId)
            putString(KEY_USERNAME, username)
            putString(KEY_ROLE, role)
            apply()
        }
    }

    /**
     * Mengecek apakah pengguna sedang dalam kondisi login.
     */
    fun isLoggedIn(): Boolean = prefs.getBoolean(KEY_IS_LOGGED_IN, false)

    /**
     * Mendapatkan UUID pengguna yang sedang login.
     */
    fun getUserId(): String = prefs.getString(KEY_USER_ID, "") ?: ""

    /**
     * Mendapatkan username pengguna yang sedang login.
     */
    fun getUsername(): String = prefs.getString(KEY_USERNAME, "") ?: ""

    /**
     * Mendapatkan role pengguna yang sedang login.
     * @return "DOKTER", "PASIEN", atau "ADMIN"
     */
    fun getRole(): String = prefs.getString(KEY_ROLE, "") ?: ""

    /**
     * Menghapus semua data sesi (logout).
     */
    fun clearSession() {
        prefs.edit().clear().apply()
    }
}
