package com.ayu.securemedis

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.ayu.securemedis.databinding.ActivityDokterBinding
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * DokterActivity.kt
 *
 * Dashboard untuk pengguna dengan role DOKTER.
 *
 * Fitur:
 * - Menampilkan daftar semua pasien yang pernah memiliki rekam medis dari dokter ini
 * - Klik pasien → buka daftar rekam medis pasien tersebut (PasienDetailActivity atau buka PasienActivity dengan user yang berbeda)
 * - Tombol "+ Tambah Rekam Medis" untuk menginput rekam medis baru
 * - Logout
 *
 * Akses data: hanya rekam_medis dengan dokter_id = id dokter yang login
 */
class DokterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDokterBinding
    private lateinit var sessionManager: SessionManager
    private lateinit var pasienAdapter: PasienAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDokterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "Dashboard Dokter"
        supportActionBar?.subtitle = "dr. ${sessionManager.getUsername()}"

        setupRecyclerView()
        setupFab()
        loadPasienList()
    }

    private fun setupRecyclerView() {
        pasienAdapter = PasienAdapter { pasien ->
            // Klik pasien → buka rekam medis pasien tersebut
            val intent = Intent(this, RekamMedisListActivity::class.java).apply {
                putExtra("PASIEN_ID", pasien.id)
                putExtra("PASIEN_NAME", pasien.username)
                putExtra("IS_DOKTER_VIEW", true)
            }
            startActivity(intent)
        }
        binding.rvPasien.apply {
            layoutManager = LinearLayoutManager(this@DokterActivity)
            adapter = pasienAdapter
        }
    }

    private fun setupFab() {
        binding.fabTambahRekamMedis.setOnClickListener {
            startActivity(Intent(this, TambahRekamMedisActivity::class.java))
        }
    }

    private fun loadPasienList() {
        showLoading(true)
        val dokterId = sessionManager.getUserId()

        lifecycleScope.launch {
            try {
                // Ambil semua rekam medis milik dokter ini
                val rekamMedisList = withContext(Dispatchers.IO) {
                    SupabaseClientProvider.client.postgrest["rekam_medis"]
                        .select(Columns.list("pasien_id")) {
                            filter { eq("dokter_id", dokterId) }
                        }
                        .decodeList<PasienIdWrapper>()
                }

                // Ambil unique pasien_id
                val pasienIds = rekamMedisList.map { it.pasienId }.distinct()

                if (pasienIds.isEmpty()) {
                    showEmpty(true)
                    showLoading(false)
                    return@launch
                }

                // Ambil data user (pasien) berdasarkan ID
                val pasienList = withContext(Dispatchers.IO) {
                    SupabaseClientProvider.client.postgrest["users"]
                        .select(Columns.list("id", "username", "role")) {
                            filter {
                                isIn("id", pasienIds)
                            }
                        }
                        .decodeList<User>()
                }

                withContext(Dispatchers.Main) {
                    showLoading(false)
                    if (pasienList.isEmpty()) {
                        showEmpty(true)
                    } else {
                        showEmpty(false)
                        pasienAdapter.submitList(pasienList)
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showLoading(false)
                    Toast.makeText(this@DokterActivity, "Gagal memuat data: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadPasienList() // Refresh setelah kembali dari TambahRekamMedis
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun showEmpty(show: Boolean) {
        binding.tvEmpty.visibility = if (show) View.VISIBLE else View.GONE
        binding.rvPasien.visibility = if (show) View.GONE else View.VISIBLE
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

    override fun onCreateOptionsMenu(menu: android.view.Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
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
