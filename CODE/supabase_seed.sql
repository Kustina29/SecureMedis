-- ============================================================
-- SecureMedis - Supabase SQL Seed Script
-- ============================================================
-- Jalankan script ini di Supabase SQL Editor
-- Dashboard → SQL Editor → New Query → Paste & Run
-- ============================================================

-- ============================================================
-- 1. BUAT TABEL users
-- ============================================================
CREATE TABLE IF NOT EXISTS users (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password_hash TEXT NOT NULL,
    role VARCHAR(10) NOT NULL CHECK (role IN ('DOKTER', 'PASIEN', 'ADMIN')),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- ============================================================
-- 2. BUAT TABEL rekam_medis
-- ============================================================
CREATE TABLE IF NOT EXISTS rekam_medis (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    pasien_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    dokter_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    diagnosis_encrypted TEXT NOT NULL,
    resep_encrypted TEXT NOT NULL,
    catatan_encrypted TEXT NOT NULL,
    tanggal DATE NOT NULL DEFAULT CURRENT_DATE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- ============================================================
-- 3. DISABLE ROW LEVEL SECURITY (untuk development)
-- Untuk produksi, aktifkan RLS dan buat policy yang sesuai
-- ============================================================
ALTER TABLE users DISABLE ROW LEVEL SECURITY;
ALTER TABLE rekam_medis DISABLE ROW LEVEL SECURITY;

-- ============================================================
-- 4. SEED DATA - USERS
-- Password di-hash dengan BCrypt cost factor 12
-- 
-- Plaintext passwords:
--   dokter1 / dokter123
--   pasien1 / pasien123
--   pasien2 / pasien123
--   admin1  / admin123
--
-- Hash dibuat dengan: BCrypt.withDefaults().hashToString(12, password.toCharArray())
-- ============================================================

-- Hapus data lama jika ada (untuk fresh seed)
DELETE FROM rekam_medis;
DELETE FROM users;

-- Insert users dengan BCrypt hashed password
-- Catatan: Hash di bawah adalah hasil generator BCrypt cost=12 yang valid.
INSERT INTO users (id, username, password_hash, role) VALUES
(
    'a1b2c3d4-e5f6-7890-abcd-ef1234567890',
    'dokter1',
    '$2a$12$PTu0NIe97ZckLmW.EoN1E.wFQiUBvfbW7cQMZAWjc9eDuBmzbuJiu',  -- dokter123
    'DOKTER'
),
(
    'b2c3d4e5-f6a7-8901-bcde-f12345678901',
    'pasien1',
    '$2a$12$lJ9rtMp77Uwx2DzCVIZmuOVn5LV4INAZc0HcWZ5BFeMFuhLItTj/K',  -- pasien123
    'PASIEN'
),
(
    'c3d4e5f6-a7b8-9012-cdef-123456789012',
    'pasien2',
    '$2a$12$lJ9rtMp77Uwx2DzCVIZmuOVn5LV4INAZc0HcWZ5BFeMFuhLItTj/K',  -- pasien123
    'PASIEN'
),
(
    'd4e5f6a7-b8c9-0123-defa-234567890123',
    'admin1',
    '$2a$12$RankCiCCxZzCE9GZOOg3xOH991cUsB5MjOoYq1LOC9oSZwLoOJkoy',  -- admin123
    'ADMIN'
);

-- ============================================================
-- 5. SEED DATA - REKAM MEDIS
-- 
-- Field diagnosis_encrypted, resep_encrypted, catatan_encrypted
-- berisi data yang sudah dienkripsi dengan Camellia-128 CBC
-- menggunakan kunci "SecureMedis2024!" (16 byte)
-- 
-- CATATAN PENTING:
-- Data di bawah adalah PLACEHOLDER.
-- Anda perlu generate ciphertext yang sebenarnya menggunakan aplikasi:
-- 1. Login sebagai dokter1
-- 2. Tambah rekam medis untuk pasien1 dan pasien2
-- Atau gunakan kode Kotlin berikut untuk generate ciphertext:
-- 
--   val encrypted = CamelliaHelper.encrypt("Diabetes Tipe 2")
--   println(encrypted) // salin hasilnya ke sini
--
-- ALTERNATIF: Jalankan query ini setelah aplikasi di-setup dan 
-- masukkan ciphertext nyata dari CamelliaHelper.kt
-- ============================================================

-- Contoh rekam medis (gunakan CamelliaHelper untuk generate nilai sebenarnya)
-- Jalankan aplikasi → Login sebagai dokter1 → Tambah Rekam Medis
-- untuk mendapatkan data terenkripsi yang valid.

-- Berikut placeholder yang perlu diganti:
INSERT INTO rekam_medis (pasien_id, dokter_id, diagnosis_encrypted, resep_encrypted, catatan_encrypted, tanggal)
VALUES
(
    'b2c3d4e5-f6a7-8901-bcde-f12345678901',  -- pasien1
    'a1b2c3d4-e5f6-7890-abcd-ef1234567890',  -- dokter1
    'G8h3XPcbh5F6f4uoB1uvlbjq/im/r7Ky0h654r7RnJSsMtgrP5b16QJZd+aGjm5TdfCauUYFg+IkU7nX/ggWbIAswrPtTk9q1MgcBlD8HrM=',
    'rV38NFyxWPkYNoltdbZf6oR8p3aIdjWwGY4AzD6CUt8RA9fEJC60kGUnTBtZTSRaKjyXQPoRqbABSyalYKvIEQ==',
    'BeWb3WKmhtTepsP7oeK80+K2pjJZAx5rj4pd2XC2DFoqbsieUCc/BoxOmogvLxpedXMve822VhDAxdagwSlzUg==',
    '2024-01-15'
),
(
    'b2c3d4e5-f6a7-8901-bcde-f12345678901',  -- pasien1
    'a1b2c3d4-e5f6-7890-abcd-ef1234567890',  -- dokter1
    'glvWSnr2xCsEjB5+SW+hubCW6YB4O+N+ys+1amuTsFNaHn0Svb/BN+NI+68QvTFG3zh6KaAW2XpvVTtMdqC0eA==',
    'qi41Nh8V9QTEI5TVbkhiHVns7c0/FLgmXPQzkuuznfVVcabZCTVeZ6fuix2Ksvqjf6VxBMXOY81tm+YHphJTJw==',
    'VQwfcK3oRgZE3cfB0JAaMeTjNCTdi6huNR/N4+VWxd2POllC+mab67tIc0lyY2U1A5/4n/YSXfFgDhIWiLGvOg==',
    '2024-02-10'
),
(
    'c3d4e5f6-a7b8-9012-cdef-123456789012',  -- pasien2
    'a1b2c3d4-e5f6-7890-abcd-ef1234567890',  -- dokter1
    'JE3om3vvjV6v7l1fuSaHtiChQ4JivyUNAJfZYXaAfE6248OWWOt7U8Gnm7d0MhMuFRG2EVYjiVsXd6/A7c2Pvw==',
    'oRGuwp31KTqfEPW3KyMbVK3tZSfw8mIpIVfketZZsdv2IRXYzS/WWpG1FDAZ4pSLo1bcrNqKH3WZ/kTqL5kur03AlcUPOQ/E7FkgwzOGyOI=',
    'IHdLjrIH5NGc4TOV36QBQ3DgcatechE8y4M3qbQwDChunibmGwSAwvqtsYRp09h6uMAxwDWqY6rADAB0WHVb0+Azfa480prc5ggL8aiDERqJpTosZ+STh8ch3fyvA+gti4hzkggYks3UbSpzfx9TYg==',
    '2024-03-05'
);

-- ============================================================
-- VERIFIKASI DATA
-- ============================================================
SELECT id, username, role FROM users ORDER BY role;
SELECT id, pasien_id, dokter_id, tanggal FROM rekam_medis ORDER BY tanggal;
