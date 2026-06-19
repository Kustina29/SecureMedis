package com.ayu.securemedis

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.serializer.KotlinXSerializer
import kotlinx.serialization.json.Json

/**
 * SupabaseClient.kt
 *
 * Singleton object untuk setup koneksi ke Supabase.
 * Menggunakan library supabase-kt dengan plugin Postgrest (REST API).
 *
 * PENTING: Ganti SUPABASE_URL dan SUPABASE_KEY dengan credential project Anda.
 * URL dan Key bisa ditemukan di dashboard Supabase:
 * Settings → API → Project URL & anon/public key
 */
object SupabaseClientProvider {

    // ⚠️ GANTI DENGAN URL DAN ANON KEY SUPABASE ANDA
    private const val SUPABASE_URL = "https://slkqfmxdxhhqqucjwcwi.supabase.co"
    private const val SUPABASE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InNsa3FmbXhkeGhocXF1Y2p3Y3dpIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODEwMTg5MTIsImV4cCI6MjA5NjU5NDkxMn0.FEV8PmKQ86rSK1Nbepn-c0uMcWC-3SEDkUaCVrQ0D7c"

    /**
     * Instance Supabase client yang diinisialisasi secara lazy.
     * Hanya dibuat sekali dan digunakan di seluruh aplikasi.
     */
    val client: SupabaseClient by lazy {
        createSupabaseClient(
            supabaseUrl = SUPABASE_URL,
            supabaseKey = SUPABASE_KEY
        ) {
            defaultSerializer = KotlinXSerializer(Json {
                ignoreUnknownKeys = true
                isLenient = true
                encodeDefaults = true
            })
            install(Postgrest)
        }
    }
}
