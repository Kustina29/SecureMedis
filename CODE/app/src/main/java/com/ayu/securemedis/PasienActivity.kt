package com.ayu.securemedis

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.ayu.securemedis.databinding.ActivityPasienBinding
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * PasienActivity.kt
 *
 * Dashboard untuk pengguna dengan role PASIEN.
 *
 * Fitur:
 * - Langsung menampilkan semua rekam medis milik pasien yang login
 * - Data diagnosis, resep, catatan di-decrypt otomatis sebelum ditampilkan
 * - Pasien TIDAK bisa input atau edit rekam medis
 * - Pasien TIDAK bisa melihat rekam medis orang lain
 * - Logout
 */
class PasienActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPasienBinding
    private lateinit var sessionManager: SessionManager
    private lateinit var adapter: RekamMedisAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPasienBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "Rekam Medis Saya"
        supportActionBar?.subtitle = sessionManager.getUsername()

        setupRecyclerView()
        loadMyRekamMedis()
    }

    private fun setupRecyclerView() {
        adapter = RekamMedisAdapter { item ->
            showEncryptionDetailDialog(item)
        }
        binding.rvRekamMedis.apply {
            layoutManager = LinearLayoutManager(this@PasienActivity)
            adapter = this@PasienActivity.adapter
        }
    }

    private fun showEncryptionDetailDialog(item: RekamMedisDecrypted) {
        val message = """
            🔒 [DI DATABASE SUPABASE]
            • Diagnosis (Terenkripsi):
            ${item.diagnosisEncrypted}
            
            • Resep (Terenkripsi):
            ${item.resepEncrypted}
            
            • Catatan (Terenkripsi):
            ${item.catatanEncrypted}
            
            🔓 [HASIL DEKRIPSI APLIKASI]
            • Diagnosis: ${item.diagnosis}
            • Resep: ${item.resep}
            • Catatan: ${item.catatan}
        """.trimIndent()

        com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle("Detail Keamanan Camellia")
            .setMessage(message)
            .setPositiveButton("Tutup", null)
            .show()
    }

    private fun loadMyRekamMedis() {
        showLoading(true)
        val pasienId = sessionManager.getUserId()

        lifecycleScope.launch {
            try {
                // Query rekam medis berdasarkan pasien_id = id pasien yang login
                val rekamMedisList = withContext(Dispatchers.IO) {
                    SupabaseClientProvider.client.postgrest["rekam_medis"]
                        .select {
                            filter { eq("pasien_id", pasienId) }
                        }
                        .decodeList<RekamMedis>()
                }

                // Dekripsi semua field sensitif
                val decryptedList = withContext(Dispatchers.IO) {
                    rekamMedisList.map { rm ->
                        RekamMedisDecrypted(
                            id = rm.id,
                            pasienId = rm.pasienId,
                            dokterId = rm.dokterId,
                            diagnosis = safeDecrypt(rm.diagnosisEncrypted),
                            resep = safeDecrypt(rm.resepEncrypted),
                            catatan = safeDecrypt(rm.catatanEncrypted),
                            tanggal = rm.tanggal,
                            diagnosisEncrypted = rm.diagnosisEncrypted,
                            resepEncrypted = rm.resepEncrypted,
                            catatanEncrypted = rm.catatanEncrypted
                        )
                    }
                }

                withContext(Dispatchers.Main) {
                    showLoading(false)
                    if (decryptedList.isEmpty()) {
                        showEmpty(true)
                    } else {
                        showEmpty(false)
                        adapter.submitList(decryptedList)
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showLoading(false)
                    Toast.makeText(this@PasienActivity, "Gagal memuat rekam medis: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun safeDecrypt(encrypted: String): String {
        return try {
            if (encrypted.isBlank()) "-" else CamelliaHelper.decrypt(encrypted)
        } catch (e: Exception) {
            "[Error dekripsi]"
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

    private fun showEmpty(show: Boolean) {
        binding.tvEmpty.visibility = if (show) View.VISIBLE else View.GONE
        binding.rvRekamMedis.visibility = if (show) View.GONE else View.VISIBLE
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
