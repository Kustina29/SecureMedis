package com.ayu.securemedis

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import at.favre.lib.crypto.bcrypt.BCrypt
import com.ayu.securemedis.databinding.ActivityLoginBinding
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * LoginActivity.kt
 *
 * Activity untuk proses autentikasi pengguna.
 *
 * Alur:
 * 1. User input username & password
 * 2. Query ke Supabase untuk mendapatkan user berdasarkan username
 * 3. Verifikasi password dengan BCrypt (password tidak pernah disimpan plaintext)
 * 4. Simpan sesi via SessionManager
 * 5. Redirect ke activity sesuai role:
 *    - DOKTER  → DokterActivity
 *    - PASIEN  → PasienActivity
 *    - ADMIN   → AdminActivity
 */
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)

        // Jika sudah login, langsung redirect
        if (sessionManager.isLoggedIn()) {
            redirectByRole(sessionManager.getRole())
            return
        }

        binding.btnLogin.setOnClickListener {
            val username = binding.etUsername.text.toString().trim()
            val password = binding.etPassword.text.toString()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Username dan password tidak boleh kosong", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            performLogin(username, password)
        }
    }

    private fun performLogin(username: String, password: String) {
        showLoading(true)

        lifecycleScope.launch {
            try {
                // Query user dari Supabase berdasarkan username
                val users = withContext(Dispatchers.IO) {
                    SupabaseClientProvider.client.postgrest["users"]
                        .select(Columns.list("id", "username", "password_hash", "role")) {
                            filter {
                                eq("username", username)
                            }
                        }
                        .decodeList<User>()
                }

                if (users.isEmpty()) {
                    showError("Username tidak ditemukan")
                    return@launch
                }

                val user = users.first()

                // Verifikasi password dengan BCrypt
                val passwordValid = withContext(Dispatchers.IO) {
                    BCrypt.verifyer().verify(
                        password.toCharArray(),
                        user.passwordHash
                    ).verified
                }

                if (!passwordValid) {
                    showError("Password salah")
                    return@launch
                }

                // Simpan sesi
                sessionManager.saveLoginSession(user.id, user.username, user.role)

                // Redirect sesuai role
                withContext(Dispatchers.Main) {
                    redirectByRole(user.role)
                }

            } catch (e: Exception) {
                showError("Error: ${e.message}")
            }
        }
    }

    private fun redirectByRole(role: String) {
        val intent = when (role) {
            "DOKTER" -> Intent(this, DokterActivity::class.java)
            "PASIEN" -> Intent(this, PasienActivity::class.java)
            "ADMIN"  -> Intent(this, AdminActivity::class.java)
            else     -> {
                Toast.makeText(this, "Role tidak dikenal: $role", Toast.LENGTH_SHORT).show()
                return
            }
        }
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.btnLogin.isEnabled = !show
    }

    private fun showError(msg: String) {
        lifecycleScope.launch(Dispatchers.Main) {
            showLoading(false)
            Toast.makeText(this@LoginActivity, msg, Toast.LENGTH_LONG).show()
        }
    }
}
