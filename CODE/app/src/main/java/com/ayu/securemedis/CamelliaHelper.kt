package com.ayu.securemedis

import org.bouncycastle.crypto.engines.CamelliaEngine
import org.bouncycastle.crypto.modes.CBCBlockCipher
import org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher
import org.bouncycastle.crypto.paddings.PKCS7Padding
import org.bouncycastle.crypto.params.KeyParameter
import org.bouncycastle.crypto.params.ParametersWithIV
import java.security.SecureRandom
import android.util.Base64

/**
 * CamelliaHelper.kt
 *
 * Enkripsi dan dekripsi menggunakan algoritma Camellia melalui library Bouncy Castle.
 * Camellia adalah block cipher 128-bit yang dikembangkan oleh Mitsubishi Electric dan NTT Jepang.
 * Menggunakan mode CBC (Cipher Block Chaining) dengan padding PKCS7.
 *
 * Kunci enkripsi (SECRET_KEY) adalah 16 byte = 128-bit.
 * IV (Initialization Vector) di-generate secara acak setiap enkripsi,
 * dan disimpan di 16 byte pertama dari ciphertext yang di-encode Base64.
 */
object CamelliaHelper {

    // Kunci enkripsi 128-bit (16 byte) — GANTI dengan kunci rahasia yang aman di produksi
    private val SECRET_KEY: ByteArray = "SecureMedis2024!".toByteArray(Charsets.UTF_8)

    /**
     * Mengenkripsi plaintext menggunakan Camellia-CBC.
     * Format output: Base64( IV (16 byte) + ciphertext )
     *
     * @param plaintext String yang akan dienkripsi
     * @return String Base64 yang berisi IV + data terenkripsi
     */
    fun encrypt(plaintext: String): String {
        val iv = ByteArray(16)
        SecureRandom().nextBytes(iv)

        val engine = CamelliaEngine()
        val cipher = PaddedBufferedBlockCipher(CBCBlockCipher(engine), PKCS7Padding())
        val keyParam = ParametersWithIV(KeyParameter(SECRET_KEY), iv)
        cipher.init(true, keyParam)

        val inputBytes = plaintext.toByteArray(Charsets.UTF_8)
        val outputBytes = ByteArray(cipher.getOutputSize(inputBytes.size))
        var length = cipher.processBytes(inputBytes, 0, inputBytes.size, outputBytes, 0)
        length += cipher.doFinal(outputBytes, length)

        // Gabungkan IV + ciphertext
        val combined = iv + outputBytes.copyOf(length)
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    /**
     * Mendekripsi ciphertext yang di-encode Base64 menggunakan Camellia-CBC.
     *
     * @param ciphertextBase64 String Base64 yang berisi IV + data terenkripsi
     * @return String plaintext yang sudah didekripsi
     */
    fun decrypt(ciphertextBase64: String): String {
        val combined = Base64.decode(ciphertextBase64, Base64.NO_WRAP)

        // Ambil IV dari 16 byte pertama
        val iv = combined.copyOfRange(0, 16)
        val ciphertext = combined.copyOfRange(16, combined.size)

        val engine = CamelliaEngine()
        val cipher = PaddedBufferedBlockCipher(CBCBlockCipher(engine), PKCS7Padding())
        val keyParam = ParametersWithIV(KeyParameter(SECRET_KEY), iv)
        cipher.init(false, keyParam)

        val outputBytes = ByteArray(cipher.getOutputSize(ciphertext.size))
        var length = cipher.processBytes(ciphertext, 0, ciphertext.size, outputBytes, 0)
        length += cipher.doFinal(outputBytes, length)

        return String(outputBytes, 0, length, Charsets.UTF_8)
    }
}
