package com.ayu.securemedis

import android.util.Base64

/**
 * CamelliaManual.kt
 *
 * ============================================================
 * IMPLEMENTASI MANUAL ALGORITMA CAMELLIA DARI SCRATCH
 * (HANYA UNTUK KEPERLUAN EDUKASI - TIDAK DIPAKAI DI APP)
 * ============================================================
 *
 * Camellia adalah simetric block cipher dengan spesifikasi:
 * - Block size: 128 bit (16 byte)
 * - Key size: 128, 192, atau 256 bit
 * - Struktur: Feistel Network 18 round (untuk kunci 128-bit)
 *
 * Dikembangkan oleh Mitsubishi Electric dan NTT (Jepang), 2000.
 * Distandarisasi oleh ISO/IEC 18033-3 dan NESSIE.
 *
 * Referensi: RFC 3713 - A Description of the Camellia Encryption Algorithm
 *
 * Komponen utama:
 * 1. Key Schedule  → Memperluas kunci asli menjadi subkey untuk setiap round
 * 2. F-Function    → Fungsi non-linear inti Camellia (S-box + P-function)
 * 3. FL/FL-inv     → Fungsi linear tambahan setiap 6 round
 * 4. 18 Round Feistel → Proses enkripsi/dekripsi
 */
object CamelliaManual {

    // =====================================================================
    // S-BOX: 4 buah substitution box 8-bit
    // Mengubah byte input menjadi byte output secara non-linear
    // SBOX1 adalah tabel substitusi utama
    // =====================================================================
    private val SBOX1 = intArrayOf(
        112, 130,  44, 236, 179,  39, 192, 229, 228, 133,  87,  53, 234,  12, 174,  65,
         35, 239, 107, 147,  69,  25, 165,  33, 237,  14,  79,  78,  29, 101, 146, 189,
        134, 184, 175, 143, 124, 235,  31, 206,  62,  48, 220,  95,  94, 197,  11,  26,
        166, 225,  57, 202, 213,  71,  93,  61, 217,   1,  90, 214,  81,  86, 108,  77,
        139,  13, 154, 102, 251, 204, 176,  45, 116,  18,  43,  32, 240, 177, 132, 153,
        223,  76, 203, 194,  52, 126, 118,   5, 109, 183, 169,  49, 209,  23,   4, 215,
         20,  88,  58,  97, 222,  27,  17,  28,  50,  15, 156,  22,  83,  24, 242,  34,
        254,  68, 207, 178, 195, 181, 122, 145,  36,   8, 232, 168,  96, 252, 105,  80,
        170, 208, 160, 125, 161, 137,  98, 151,  84,  91,  30, 149, 224, 255, 100, 210,
         16, 196,   0,  72, 163, 247, 117, 219, 138,   3, 230, 218,   9,  63, 221, 148,
        135,  92, 131,   2, 205,  74, 144,  51, 115, 103, 246, 243, 157, 127, 191, 226,
         82, 155, 216,  38, 200,  55, 198,  59, 129, 150, 111,  75,  19, 190,  99,  46,
        233, 121, 167, 140, 159, 110, 188, 142,  41, 245, 249, 182,  47, 253, 180,  89,
        120, 152,   6, 106, 231,  70, 113, 186, 212,  37, 171,  66, 136, 162, 141, 250,
        114,   7, 185,  85, 248, 238, 172,  10,  54,  73,  42, 104,  60,  56, 241, 164,
         64,  40, 211, 123, 187, 201,  67, 193,  21, 227, 173, 244, 119, 199, 128, 158
    )
    // SBOX2: SBOX1 dirotasi kiri 1 bit setiap elemen
    private val SBOX2 = IntArray(256) { i ->
        val v = SBOX1[i]
        ((v shl 1) or (v ushr 7)) and 0xFF
    }
    // SBOX3: SBOX1 dirotasi kanan 1 bit setiap elemen
    private val SBOX3 = IntArray(256) { i ->
        val v = SBOX1[i]
        ((v ushr 1) or (v shl 7)) and 0xFF
    }
    // SBOX4: SBOX1[input dirotasi kiri 1 bit]
    private val SBOX4 = IntArray(256) { i ->
        SBOX1[((i shl 1) or (i ushr 7)) and 0xFF]
    }

    // =====================================================================
    // KONSTANTA SIGMA (σ): 6 konstanta 64-bit untuk key schedule
    // Digunakan saat membangkitkan subkey dari kunci asli
    // =====================================================================
    private val SIGMA = arrayOf(
        longArrayOf(0xA09E667FL, 0x3BCC908BL),
        longArrayOf(0xB67AE879L, 0x4F3108A4L),
        longArrayOf(0xB2FB1366L, 0xEA397F2FL),
        longArrayOf(0xC4BEA1DBL, 0xA8599294L),
        longArrayOf(0xA08FAE9BL, 0xBC394BD8L),
        longArrayOf(0xBEF85F0EL, 0xCA7CBC72L)
    )

    // =====================================================================
    // P-FUNCTION (Diffusion Layer)
    // Mengkombinasikan 8 byte S-box output menjadi 8 byte baru
    // Memastikan setiap bit output bergantung pada banyak bit input
    // =====================================================================
    private fun pFunction(x: LongArray): LongArray {
        val s = IntArray(8)
        // Terapkan 4 S-box berbeda secara bergantian
        s[0] = SBOX1[((x[0] ushr 56) and 0xFF).toInt()]
        s[1] = SBOX2[((x[0] ushr 48) and 0xFF).toInt()]
        s[2] = SBOX3[((x[0] ushr 40) and 0xFF).toInt()]
        s[3] = SBOX4[((x[0] ushr 32) and 0xFF).toInt()]
        s[4] = SBOX2[((x[0] ushr 24) and 0xFF).toInt()]
        s[5] = SBOX3[((x[0] ushr 16) and 0xFF).toInt()]
        s[6] = SBOX4[((x[0] ushr  8) and 0xFF).toInt()]
        s[7] = SBOX1[((x[0]       ) and 0xFF).toInt()]

        // XOR antar byte untuk diffusion (matriks MDS 8x8)
        val t = IntArray(8)
        t[0] = s[0] xor s[2] xor s[3] xor s[5] xor s[6] xor s[7]
        t[1] = s[0] xor s[1] xor s[3] xor s[4] xor s[6] xor s[7]
        t[2] = s[0] xor s[1] xor s[2] xor s[4] xor s[5] xor s[7]
        t[3] = s[1] xor s[2] xor s[3] xor s[4] xor s[5] xor s[6]
        t[4] = s[0] xor s[1] xor s[5] xor s[6] xor s[7]
        t[5] = s[1] xor s[2] xor s[4] xor s[6] xor s[7]
        t[6] = s[2] xor s[3] xor s[4] xor s[5] xor s[7]
        t[7] = s[0] xor s[3] xor s[4] xor s[5] xor s[6]

        val result = LongArray(2)
        result[0] = (t[0].toLong() shl 56) or (t[1].toLong() shl 48) or
                (t[2].toLong() shl 40) or (t[3].toLong() shl 32) or
                (t[4].toLong() shl 24) or (t[5].toLong() shl 16) or
                (t[6].toLong() shl  8) or t[7].toLong()
        result[1] = 0L
        return result
    }

    // =====================================================================
    // F-FUNCTION: Fungsi inti setiap round Camellia
    // Input: 64-bit data (F), 64-bit subkey (KE)
    // Output: 64-bit hasil transformasi
    //
    // F(F, KE) = P( S( F XOR KE ) )
    // =====================================================================
    private fun fFunction(f: Long, ke: Long): Long {
        val x = f xor ke
        val input = longArrayOf(x)
        return pFunction(input)[0]
    }

    // =====================================================================
    // FL-FUNCTION: Fungsi linear tambahan setiap 6 round
    // Menambah kompleksitas untuk melawan kriptanalisis linear/diferensial
    //
    // FL(FL_IN, KE):
    //   x1 = FL_IN >> 32
    //   x2 = FL_IN & 0xFFFFFFFF
    //   x2 = x2 XOR ROL((x1 AND KE_L), 1)
    //   x1 = x1 XOR (x2 OR KE_R)
    //   return (x1 << 32) | x2
    // =====================================================================
    private fun flFunction(flIn: Long, ke: Long): Long {
        var x1 = (flIn ushr 32).toInt()
        var x2 = (flIn and 0xFFFFFFFFL).toInt()
        val keL = (ke ushr 32).toInt()
        val keR = (ke and 0xFFFFFFFFL).toInt()
        x2 = x2 xor Integer.rotateLeft(x1 and keL, 1)
        x1 = x1 xor (x2 or keR)
        return ((x1.toLong() and 0xFFFFFFFFL) shl 32) or (x2.toLong() and 0xFFFFFFFFL)
    }

    // =====================================================================
    // FL-INVERSE FUNCTION: Invers dari FL (dipakai saat dekripsi)
    // =====================================================================
    private fun flInvFunction(flIn: Long, ke: Long): Long {
        var x1 = (flIn ushr 32).toInt()
        var x2 = (flIn and 0xFFFFFFFFL).toInt()
        val keL = (ke ushr 32).toInt()
        val keR = (ke and 0xFFFFFFFFL).toInt()
        x1 = x1 xor (x2 or keR)
        x2 = x2 xor Integer.rotateLeft(x1 and keL, 1)
        return ((x1.toLong() and 0xFFFFFFFFL) shl 32) or (x2.toLong() and 0xFFFFFFFFL)
    }

    // =====================================================================
    // ROTASI KIRI 128-BIT (dipakai dalam key schedule)
    // =====================================================================
    private fun rotL128(kl: Long, kr: Long, n: Int): LongArray {
        return if (n >= 64) {
            val sh = n - 64
            longArrayOf(
                (kr shl sh) or (kl ushr (64 - sh)),
                (kl shl sh) or (kr ushr (64 - sh))
            )
        } else {
            longArrayOf(
                (kl shl n) or (kr ushr (64 - n)),
                (kr shl n) or (kl ushr (64 - n))
            )
        }
    }

    // =====================================================================
    // KEY SCHEDULE: Membangkitkan subkey dari kunci 128-bit
    //
    // Proses:
    // 1. Bagi kunci menjadi KL (kiri 64-bit) dan KR (kanan 64-bit)
    // 2. Hitung KA dan KB menggunakan 4x F-function dengan konstanta SIGMA
    // 3. Rotasi KL dan KA untuk menghasilkan 26 subkey (K1..K18 + KE1..KE4 + KW1..KW4)
    // =====================================================================
    data class KeySchedule(
        val k: Array<Long>,    // 18 subkey round
        val ke: Array<Long>,   // 4 subkey FL
        val kw: Array<Long>    // 4 subkey whitening
    )

    private fun keySchedule(key: ByteArray): KeySchedule {
        require(key.size == 16) { "Kunci Camellia harus 128 bit (16 byte)" }

        fun bytesToLong(b: ByteArray, off: Int): Long {
            var v = 0L
            for (i in 0 until 8) v = (v shl 8) or (b[off + i].toLong() and 0xFF)
            return v
        }

        val kl = bytesToLong(key, 0)
        val kr = bytesToLong(key, 8)

        // Step 1: Hitung D1, D2 dari KL XOR KR
        var d1 = kl xor kr
        var d2 = 0L // Diproses dengan 6 F-function dan SIGMA

        // Step 2: 6x F-function dengan konstanta SIGMA → menghasilkan KA
        d2 = d2 xor fFunction(d1, SIGMA[0][0])
        d1 = d1 xor fFunction(d2, SIGMA[1][0])
        d1 = d1 xor kl
        d2 = d2 xor kr
        d2 = d2 xor fFunction(d1, SIGMA[2][0])
        d1 = d1 xor fFunction(d2, SIGMA[3][0])

        val kaL = d1; val kaR = d2

        // Step 3: Bangkitkan 26 subkey dengan rotasi KL dan KA
        val k = Array(18) { 0L }
        val ke = Array(4) { 0L }
        val kw = Array(4) { 0L }

        // KW1..KW2 dari KL[0..127]
        kw[0] = kl; kw[1] = kr
        // K1..K2 dari KA[0..127]
        k[0] = kaL; k[1] = kaR
        // K3..K4 dari KL rotasi kiri 15 bit
        var rot = rotL128(kl, kr, 15)
        k[2] = rot[0]; k[3] = rot[1]
        // K5..K6 dari KA rotasi kiri 15 bit
        rot = rotL128(kaL, kaR, 15)
        k[4] = rot[0]; k[5] = rot[1]
        // KE1..KE2 dari KA rotasi kiri 30 bit
        rot = rotL128(kaL, kaR, 30)
        ke[0] = rot[0]; ke[1] = rot[1]
        // K7..K8 dari KL rotasi kiri 45 bit
        rot = rotL128(kl, kr, 45)
        k[6] = rot[0]; k[7] = rot[1]
        // K9 dari KA rotasi kiri 45 bit (hanya kiri)
        rot = rotL128(kaL, kaR, 45)
        k[8] = rot[0]
        // K10 dari KL rotasi kiri 77 bit (hanya kanan)
        rot = rotL128(kl, kr, 77)
        k[9] = rot[1]
        // K11..K12 dari KL rotasi kiri 94 bit
        rot = rotL128(kl, kr, 94)
        k[10] = rot[0]; k[11] = rot[1]
        // KE3..KE4 dari KL rotasi kiri 94 bit sebagian
        rot = rotL128(kaL, kaR, 94)
        ke[2] = rot[0]; ke[3] = rot[1]
        // K13..K14 dari KL rotasi kiri 111 bit
        rot = rotL128(kl, kr, 111)
        k[12] = rot[0]; k[13] = rot[1]
        // K15..K16 dari KA rotasi kiri 111 bit
        rot = rotL128(kaL, kaR, 111)
        k[14] = rot[0]; k[15] = rot[1]
        // K17..K18 dari KL rotasi kiri 127 bit
        rot = rotL128(kl, kr, 127)
        k[16] = rot[0]; k[17] = rot[1]
        // KW3..KW4 dari KA rotasi kiri 111 bit bagian kanan
        rot = rotL128(kaL, kaR, 94)
        kw[2] = rot[0]; kw[3] = rot[1]

        return KeySchedule(k, ke, kw)
    }

    // =====================================================================
    // ENKRIPSI BLOK 128-BIT (1 block Camellia)
    //
    // Struktur 18 round Feistel:
    // Round  1- 6: 6x F-function
    // Round  7- 8: FL / FL-inv
    // Round  9-12: 4x F-function
    // Round 13-14: FL / FL-inv
    // Round 15-18: 4x F-function
    // Finalize: whitening dengan KW3..KW4
    // =====================================================================
    private fun encryptBlock(block: ByteArray, ks: KeySchedule): ByteArray {
        require(block.size == 16) { "Block harus 128 bit (16 byte)" }
        fun bytes2long(b: ByteArray, off: Int): Long {
            var v = 0L; for (i in 0 until 8) v = (v shl 8) or (b[off+i].toLong() and 0xFF); return v
        }
        fun long2bytes(v: Long, b: ByteArray, off: Int) {
            for (i in 7 downTo 0) { b[off + (7-i)] = ((v ushr (i*8)) and 0xFF).toByte() }
        }

        var d1 = bytes2long(block, 0)
        var d2 = bytes2long(block, 8)

        // Pre-whitening
        d1 = d1 xor ks.kw[0]
        d2 = d2 xor ks.kw[1]

        // Round 1-6
        d2 = d2 xor fFunction(d1, ks.k[0])
        d1 = d1 xor fFunction(d2, ks.k[1])
        d2 = d2 xor fFunction(d1, ks.k[2])
        d1 = d1 xor fFunction(d2, ks.k[3])
        d2 = d2 xor fFunction(d1, ks.k[4])
        d1 = d1 xor fFunction(d2, ks.k[5])

        // FL layer setelah round 6
        d1 = flFunction(d1, ks.ke[0])
        d2 = flInvFunction(d2, ks.ke[1])

        // Round 7-12
        d2 = d2 xor fFunction(d1, ks.k[6])
        d1 = d1 xor fFunction(d2, ks.k[7])
        d2 = d2 xor fFunction(d1, ks.k[8])
        d1 = d1 xor fFunction(d2, ks.k[9])
        d2 = d2 xor fFunction(d1, ks.k[10])
        d1 = d1 xor fFunction(d2, ks.k[11])

        // FL layer setelah round 12
        d1 = flFunction(d1, ks.ke[2])
        d2 = flInvFunction(d2, ks.ke[3])

        // Round 13-18
        d2 = d2 xor fFunction(d1, ks.k[12])
        d1 = d1 xor fFunction(d2, ks.k[13])
        d2 = d2 xor fFunction(d1, ks.k[14])
        d1 = d1 xor fFunction(d2, ks.k[15])
        d2 = d2 xor fFunction(d1, ks.k[16])
        d1 = d1 xor fFunction(d2, ks.k[17])

        // Post-whitening
        d2 = d2 xor ks.kw[2]
        d1 = d1 xor ks.kw[3]

        val out = ByteArray(16)
        long2bytes(d2, out, 0)
        long2bytes(d1, out, 8)
        return out
    }

    // =====================================================================
    // ENKRIPSI ECB MODE (tanpa IV, hanya untuk edukasi)
    // Dalam praktek, selalu gunakan CBC atau GCM (seperti di CamelliaHelper)
    // =====================================================================
    fun encryptECB(plaintext: String, key: ByteArray): String {
        val ks = keySchedule(key)
        val data = plaintext.toByteArray(Charsets.UTF_8)

        // PKCS7 Padding
        val padLen = 16 - (data.size % 16)
        val padded = data + ByteArray(padLen) { padLen.toByte() }

        val output = ByteArray(padded.size)
        for (i in padded.indices step 16) {
            val block = padded.copyOfRange(i, i + 16)
            val enc = encryptBlock(block, ks)
            enc.copyInto(output, i)
        }
        return Base64.encodeToString(output, Base64.NO_WRAP)
    }

    /**
     * Demonstrasi penggunaan implementasi manual ini
     * (hanya untuk keperluan testing/edukasi)
     */
    fun demo(): String {
        val key = "SecureMedis2024!".toByteArray(Charsets.UTF_8)
        val plaintext = "Rekam Medis Rahasia: Diabetes Tipe 2"
        val encrypted = encryptECB(plaintext, key)
        return """
            === Demo Camellia Manual ===
            Plaintext  : $plaintext
            Key        : ${String(key)}
            Encrypted  : $encrypted
            
            Struktur Algoritma:
            - Block size  : 128 bit
            - Key size    : 128 bit
            - Round count : 18 round
            - S-boxes     : 4 buah (SBOX1-4)
            - F-function  : S-box + P-function (diffusion)
            - FL-function : Setiap 6 round (anti linear cryptanalysis)
            - Key schedule: Rotasi kiri 128-bit pada KL dan KA
        """.trimIndent()
    }
}
