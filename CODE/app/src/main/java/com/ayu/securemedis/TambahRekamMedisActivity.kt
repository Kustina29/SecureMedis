package com.ayu.securemedis

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.ayu.securemedis.databinding.ActivityTambahRekamMedisBinding
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * TambahRekamMedisActivity.kt
 *
 * Form untuk DOKTER menginput rekam medis pasien.
 *
 * Alur enkripsi:
 * 1. Dokter isi form (diagnosis, resep, catatan dalam plaintext)
 * 2. Sebelum dikirim ke Supabase, setiap field dienkripsi menggunakan CamelliaHelper
 * 3. Data yang tersimpan di database selalu dalam bentuk ciphertext
 * 4. Password tidak pernah disimpan atau dikirim sebagai plaintext
 *
 * Bisa menerima PASIEN_ID dari DokterActivity untuk memilih pasien secara otomatis.
 */
class TambahRekamMedisActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTambahRekamMedisBinding
    private lateinit var sessionManager: SessionManager

    private var selectedPasienId: String = ""
    private var pasienList: List<User> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTambahRekamMedisBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "Tambah Rekam Medis"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Cek apakah ada pasien yang sudah dipilih dari DokterActivity
        val preselectedPasienId = intent.getStringExtra("PASIEN_ID")
        val preselectedPasienName = intent.getStringExtra("PASIEN_NAME")

        if (preselectedPasienId != null) {
            selectedPasienId = preselectedPasienId
            binding.spinnerPasien.isEnabled = false
            loadSinglePasien(preselectedPasienId, preselectedPasienName ?: "")
        } else {
            loadPasienList()
        }

        binding.btnSimpan.setOnClickListener {
            submitRekamMedis()
        }
    }

    private fun loadSinglePasien(pasienId: String, pasienName: String) {
        val adapter = android.widget.ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            listOf(pasienName)
        )
        binding.spinnerPasien.adapter = adapter
        selectedPasienId = pasienId
    }

    private fun loadPasienList() {
        showLoading(true)
        lifecycleScope.launch {
            try {
                pasienList = withContext(Dispatchers.IO) {
                    SupabaseClientProvider.client.postgrest["users"]
                        .select(Columns.list("id", "username", "role")) {
                            filter { eq("role", "PASIEN") }
                        }
                        .decodeList<User>()
                }

                withContext(Dispatchers.Main) {
                    showLoading(false)
                    if (pasienList.isEmpty()) {
                        Toast.makeText(this@TambahRekamMedisActivity, "Tidak ada pasien terdaftar", Toast.LENGTH_SHORT).show()
                        return@withContext
                    }

                    val namaList = pasienList.map { it.username }
                    val adapter = android.widget.ArrayAdapter(
                        this@TambahRekamMedisActivity,
                        android.R.layout.simple_spinner_dropdown_item,
                        namaList
                    )
                    binding.spinnerPasien.adapter = adapter
                    selectedPasienId = pasienList.first().id

                    binding.spinnerPasien.onItemSelectedListener =
                        object : android.widget.AdapterView.OnItemSelectedListener {
                            override fun onItemSelected(
                                parent: android.widget.AdapterView<*>?,
                                view: View?,
                                position: Int,
                                id: Long
                            ) {
                                selectedPasienId = pasienList[position].id
                            }
                            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
                        }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showLoading(false)
                    Toast.makeText(this@TambahRekamMedisActivity, "Gagal memuat pasien: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun submitRekamMedis() {
        val diagnosis = binding.etDiagnosis.text.toString().trim()
        val resep = binding.etResep.text.toString().trim()
        val catatan = binding.etCatatan.text.toString().trim()

        if (selectedPasienId.isEmpty()) {
            Toast.makeText(this, "Pilih pasien terlebih dahulu", Toast.LENGTH_SHORT).show()
            return
        }
        if (diagnosis.isEmpty()) {
            binding.etDiagnosis.error = "Diagnosis tidak boleh kosong"
            return
        }

        showLoading(true)

        lifecycleScope.launch {
            try {
                // Enkripsi semua field sensitif sebelum dikirim ke database
                val diagnosisEncrypted = withContext(Dispatchers.IO) {
                    CamelliaHelper.encrypt(diagnosis)
                }
                val resepEncrypted = withContext(Dispatchers.IO) {
                    CamelliaHelper.encrypt(resep.ifEmpty { "-" })
                }
                val catatanEncrypted = withContext(Dispatchers.IO) {
                    CamelliaHelper.encrypt(catatan.ifEmpty { "-" })
                }

                val tanggal = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

                withContext(Dispatchers.IO) {
                    SupabaseClientProvider.client.postgrest["rekam_medis"].insert(
                        buildJsonObject {
                            put("pasien_id", selectedPasienId)
                            put("dokter_id", sessionManager.getUserId())
                            put("diagnosis_encrypted", diagnosisEncrypted)
                            put("resep_encrypted", resepEncrypted)
                            put("catatan_encrypted", catatanEncrypted)
                            put("tanggal", tanggal)
                        }
                    )
                }

                withContext(Dispatchers.Main) {
                    showLoading(false)
                    Toast.makeText(this@TambahRekamMedisActivity, "Rekam medis berhasil disimpan (terenkripsi)", Toast.LENGTH_SHORT).show()
                    finish()
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showLoading(false)
                    Toast.makeText(this@TambahRekamMedisActivity, "Gagal menyimpan: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.btnSimpan.isEnabled = !show
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
