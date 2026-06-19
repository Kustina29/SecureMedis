package com.ayu.securemedis

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.ayu.securemedis.databinding.ActivityRekamMedisListBinding
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * RekamMedisListActivity.kt
 *
 * Menampilkan daftar rekam medis untuk pasien tertentu.
 * Digunakan oleh DOKTER untuk melihat rekam medis pasien pilihan.
 * Data diagnosis, resep, dan catatan di-decrypt otomatis sebelum ditampilkan.
 */
class RekamMedisListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRekamMedisListBinding
    private lateinit var sessionManager: SessionManager
    private lateinit var adapter: RekamMedisAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRekamMedisListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)

        val pasienId = intent.getStringExtra("PASIEN_ID") ?: ""
        val pasienName = intent.getStringExtra("PASIEN_NAME") ?: "Pasien"
        val isDokterView = intent.getBooleanExtra("IS_DOKTER_VIEW", false)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "Rekam Medis"
        supportActionBar?.subtitle = pasienName
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        setupRecyclerView()

        if (isDokterView) {
            binding.fabTambah.visibility = View.VISIBLE
            binding.fabTambah.setOnClickListener {
                val intent = Intent(this, TambahRekamMedisActivity::class.java).apply {
                    putExtra("PASIEN_ID", pasienId)
                    putExtra("PASIEN_NAME", pasienName)
                }
                startActivity(intent)
            }
        } else {
            binding.fabTambah.visibility = View.GONE
        }

        loadRekamMedis(pasienId)
    }

    private fun setupRecyclerView() {
        adapter = RekamMedisAdapter { item ->
            showEncryptionDetailDialog(item)
        }
        binding.rvRekamMedis.apply {
            layoutManager = LinearLayoutManager(this@RekamMedisListActivity)
            adapter = this@RekamMedisListActivity.adapter
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

    private fun loadRekamMedis(pasienId: String) {
        showLoading(true)
        val dokterId = sessionManager.getUserId()
        val role = sessionManager.getRole()

        lifecycleScope.launch {
            try {
                val rekamMedisList = withContext(Dispatchers.IO) {
                    val query = SupabaseClientProvider.client.postgrest["rekam_medis"]
                        .select {
                            filter {
                                eq("pasien_id", pasienId)
                                // Jika DOKTER, filter juga berdasarkan dokter_id
                                if (role == "DOKTER") {
                                    eq("dokter_id", dokterId)
                                }
                            }
                        }
                    query.decodeList<RekamMedis>()
                }

                // Dekripsi setiap rekam medis
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
                    Toast.makeText(this@RekamMedisListActivity, "Gagal: ${e.message}", Toast.LENGTH_LONG).show()
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

    override fun onResume() {
        super.onResume()
        val pasienId = intent.getStringExtra("PASIEN_ID") ?: ""
        loadRekamMedis(pasienId)
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun showEmpty(show: Boolean) {
        binding.tvEmpty.visibility = if (show) View.VISIBLE else View.GONE
        binding.rvRekamMedis.visibility = if (show) View.GONE else View.VISIBLE
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
