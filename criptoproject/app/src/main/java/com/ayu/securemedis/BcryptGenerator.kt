package com.ayu.securemedis

import at.favre.lib.crypto.bcrypt.BCrypt

/**
 * BcryptGenerator.kt
 *
 * HANYA UNTUK KEPERLUAN DEVELOPMENT / SEEDING DATABASE
 * Jalankan fungsi ini SEKALI untuk generate hash BCrypt yang akan dipakai di SQL seed.
 *
 * Cara penggunaan:
 * 1. Panggil generateAllHashes() dari MainActivity atau unit test
 * 2. Copy output ke supabase_seed.sql
 * 3. Hapus / komentari pemanggilan fungsi ini setelah selesai
 */
object BcryptGenerator {

    fun generateAllHashes(): String {
        val dokterHash = BCrypt.withDefaults().hashToString(12, "dokter123".toCharArray())
        val pasienHash = BCrypt.withDefaults().hashToString(12, "pasien123".toCharArray())
        val adminHash  = BCrypt.withDefaults().hashToString(12, "admin123".toCharArray())

        return """
            === BCrypt Hashes for Seed Data ===
            dokter1 / dokter123 → $dokterHash
            pasien1 / pasien123 → $pasienHash
            pasien2 / pasien123 → $pasienHash
            admin1  / admin123  → $adminHash
            
            === Camellia Encrypted Sample Data ===
            (Gunakan key: "SecureMedis2024!")
            
            Diagnosis 1 (Diabetes Tipe 2):
            ${CamelliaHelper.encrypt("Diabetes Tipe 2 dengan komplikasi neuropati perifer")}
            
            Resep 1:
            ${CamelliaHelper.encrypt("Metformin 500mg 3x1, Glibenklamid 5mg 1x1 pagi")}
            
            Catatan 1:
            ${CamelliaHelper.encrypt("Pasien diminta kontrol gula darah setiap minggu")}
            
            Diagnosis 2 (Hipertensi):
            ${CamelliaHelper.encrypt("Hipertensi Grade 2, TD 160/100 mmHg")}
            
            Resep 2:
            ${CamelliaHelper.encrypt("Amlodipine 10mg 1x1, Candesartan 16mg 1x1")}
            
            Catatan 2:
            ${CamelliaHelper.encrypt("Diet rendah garam, olahraga rutin 30 menit/hari")}
            
            Diagnosis 3 (ISPA):
            ${CamelliaHelper.encrypt("Infeksi Saluran Pernafasan Atas (ISPA) ringan")}
            
            Resep 3:
            ${CamelliaHelper.encrypt("Amoxicillin 500mg 3x1, Paracetamol 500mg 3x1 jika demam")}
            
            Catatan 3:
            ${CamelliaHelper.encrypt("Istirahat cukup, banyak minum air putih, kembali jika tidak membaik dalam 3 hari")}
        """.trimIndent()
    }
}
