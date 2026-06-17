package com.ayu.securemedis

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import at.favre.lib.crypto.bcrypt.BCrypt
import com.ayu.securemedis.databinding.ActivityAdminBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * AdminActivity.kt
 *
 * Dashboard untuk pengguna dengan role ADMIN.
 *
 * Fitur:
 * - Melihat daftar semua user (DOKTER, PASIEN, ADMIN)
 * - Tambah user baru (dengan password yang di-hash menggunakan BCrypt)
 * - Hapus user
 * - ADMIN tidak punya akses ke rekam medis sama sekali
 */
class AdminActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminBinding
    private lateinit var sessionManager: SessionManager
    private lateinit var userAdapter: UserAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "Manajemen User"
        supportActionBar?.subtitle = "Admin: ${sessionManager.getUsername()}"

        setupRecyclerView()
        setupFab()
        loadUserList()
    }

    private fun setupRecyclerView() {
        userAdapter = UserAdapter(
            onDelete = { user -> showDeleteConfirmDialog(user) }
        )
        binding.rvUsers.apply {
            layoutManager = LinearLayoutManager(this@AdminActivity)
            adapter = userAdapter
        }
    }

    private fun setupFab() {
        binding.fabTambahUser.setOnClickListener {
            showAddUserDialog()
        }
    }

    private fun loadUserList() {
        showLoading(true)
        lifecycleScope.launch {
            try {
                val users = withContext(Dispatchers.IO) {
                    SupabaseClientProvider.client.postgrest["users"]
                        .select(Columns.list("id", "username", "role"))
                        .decodeList<User>()
                }
                withContext(Dispatchers.Main) {
                    showLoading(false)
                    if (users.isEmpty()) {
                        binding.tvEmpty.visibility = View.VISIBLE
                        binding.rvUsers.visibility = View.GONE
                    } else {
                        binding.tvEmpty.visibility = View.GONE
                        binding.rvUsers.visibility = View.VISIBLE
                        userAdapter.submitList(users)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showLoading(false)
                    Toast.makeText(this@AdminActivity, "Gagal memuat user: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun showAddUserDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_user, null)
        val etUsername = dialogView.findViewById<TextInputEditText>(R.id.et_new_username)
        val etPassword = dialogView.findViewById<TextInputEditText>(R.id.et_new_password)
        val spinnerRole = dialogView.findViewById<android.widget.Spinner>(R.id.spinner_role)

        val roles = arrayOf("DOKTER", "PASIEN", "ADMIN")
        val roleAdapter = android.widget.ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, roles)
        spinnerRole.adapter = roleAdapter

        MaterialAlertDialogBuilder(this)
            .setTitle("Tambah User Baru")
            .setView(dialogView)
            .setPositiveButton("Simpan") { _, _ ->
                val username = etUsername.text.toString().trim()
                val password = etPassword.text.toString()
                val role = spinnerRole.selectedItem.toString()

                if (username.isEmpty() || password.isEmpty()) {
                    Toast.makeText(this, "Username dan password tidak boleh kosong", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                addUser(username, password, role)
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun addUser(username: String, password: String, role: String) {
        showLoading(true)
        lifecycleScope.launch {
            try {
                // Hash password dengan BCrypt sebelum menyimpan ke database
                val passwordHash = withContext(Dispatchers.IO) {
                    BCrypt.withDefaults().hashToString(12, password.toCharArray())
                }

                withContext(Dispatchers.IO) {
                    SupabaseClientProvider.client.postgrest["users"].insert(
                        buildJsonObject {
                            put("username", username)
                            put("password_hash", passwordHash)
                            put("role", role)
                        }
                    )
                }

                withContext(Dispatchers.Main) {
                    showLoading(false)
                    Toast.makeText(this@AdminActivity, "User $username berhasil ditambahkan", Toast.LENGTH_SHORT).show()
                    loadUserList()
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showLoading(false)
                    Toast.makeText(this@AdminActivity, "Gagal menambah user: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun showDeleteConfirmDialog(user: User) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Hapus User")
            .setMessage("Apakah Anda yakin ingin menghapus user '${user.username}'?")
            .setPositiveButton("Hapus") { _, _ -> deleteUser(user) }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun deleteUser(user: User) {
        showLoading(true)
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    SupabaseClientProvider.client.postgrest["users"].delete {
                        filter { eq("id", user.id) }
                    }
                }
                withContext(Dispatchers.Main) {
                    showLoading(false)
                    Toast.makeText(this@AdminActivity, "User ${user.username} dihapus", Toast.LENGTH_SHORT).show()
                    loadUserList()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showLoading(false)
                    Toast.makeText(this@AdminActivity, "Gagal menghapus user: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: android.view.Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_logout -> {
                logout()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun logout() {
        sessionManager.clearSession()
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}
