# Tazer

Tazer membantu mahasiswa mencatat tugas, menyusun jadwal belajar, dan melacak sesi fokus. Tugas, jadwal, dan timer dapat dipakai secara offline. Akun Supabase menambahkan sinkronisasi antarperangkat, dan PRO menyediakan hingga 100 analisis AI per bulan melalui server, termasuk scan tugas dari foto atau screenshot. Smart Daily Briefing mengirim ringkasan jadwal setiap pagi dan review setiap malam. Jadwal tetap bisa diisi manual atau diimpor dari .ics, .csv, .xlsx, maupun foto KRS. Tugas bisa dicari dan difilter per mata kuliah dengan warna yang konsisten, dibagikan ke teman kelompok, diekspor ke `.ics`, dan dicadangkan ke file JSON.

## Instalasi Android

1. Buka [GitHub Releases](https://github.com/FirmanFelixNduru/Tazer/releases/latest) dan unduh APK bertanda tangan.
2. Buka APK melalui penginstal Android. Untuk memperbarui instalasi, pasang APK baru langsung di atas versi lama.
3. Di Tazer, buka **Pengaturan → Pembaruan aplikasi → Cek Update** untuk melihat rilis berikutnya.

Rilis GitHub menampilkan SHA-256 APK. Tazer mendukung Android 8.0 (API 26) ke atas.

## Pengembangan lokal

- `android/` memuat aplikasi Kotlin, Jetpack Compose, Room, serta tes perangkat. Jalankan `gradlew.bat :app:assembleDebug :app:testDebugUnitTest` dari folder tersebut dengan JDK 25 dan Android SDK 37.
- `backend/` memuat Worker TypeScript, migrasi Supabase, dan mode mock. Jalankan `npm ci` dan `npm test` dari folder tersebut. [Panduan backend](backend/README.md)
- [Kontrak data/API](docs/DATA_AND_API_CONTRACT.md) dan [spesifikasi scheduler](docs/SCHEDULER_SPEC.md) menjelaskan perilaku aplikasi.
