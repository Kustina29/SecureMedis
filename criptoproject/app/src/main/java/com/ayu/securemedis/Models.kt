package com.ayu.securemedis

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Data models untuk serialisasi/deserialisasi data dari Supabase.
 */

@Serializable
data class User(
    val id: String = "",
    val username: String = "",
    @SerialName("password_hash")
    val passwordHash: String = "",
    val role: String = ""
)

@Serializable
data class PasienIdWrapper(
    @SerialName("pasien_id")
    val pasienId: String
)

@Serializable
data class RekamMedis(
    val id: String = "",
    @SerialName("pasien_id")
    val pasienId: String = "",
    @SerialName("dokter_id")
    val dokterId: String = "",
    @SerialName("diagnosis_encrypted")
    val diagnosisEncrypted: String = "",
    @SerialName("resep_encrypted")
    val resepEncrypted: String = "",
    @SerialName("catatan_encrypted")
    val catatanEncrypted: String = "",
    val tanggal: String = ""
)

// Model yang sudah di-decrypt untuk tampilan UI
data class RekamMedisDecrypted(
    val id: String,
    val pasienId: String,
    val dokterId: String,
    val diagnosis: String,
    val resep: String,
    val catatan: String,
    val tanggal: String,
    val diagnosisEncrypted: String,
    val resepEncrypted: String,
    val catatanEncrypted: String
)
