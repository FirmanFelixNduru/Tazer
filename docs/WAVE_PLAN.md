# Rencana eksekusi Tazer

Estimasi per wave adalah waktu kerja aktif. Jalur paralel mengikuti gate dan pemilik. Lead menangani keputusan lintas modul dan membuka pekerjaan berikutnya setelah bukti gate tersedia.

| Wave | Pekerjaan dan pemilik | Gate |
| --- | --- | --- |
| **0 — Blueprint** (~25 menit) | Lead menyusun aturan repo, kontrak, scheduler, dan brief. Architect memeriksa contoh algoritma. QA memeriksa konsistensi kontrak dan gate. | Kontrak ditautkan, contoh lolos pemeriksaan, graph dokumen bisa ditelusuri, ruang lingkup Wave 1 tercatat. |
| **1 — Fondasi** (~3 jam) | Core membuat `:app`, Room v1, repository per UID dan analyzer offline. Backend membuat HTTP, auth Supabase, kuota, dan adapter Gemini/Groq dengan mock. UI membuat login demo dan dua tab. Architect menyiapkan fixture scheduler. | Unit test Android dan backend lulus. Schema Room diekspor dan `assembleDebug` menghasilkan APK dengan package ID `com.tazer.taskorganizer`. |
| **2 — Tugas, PRO dan BYOK** (~5 jam) | Core mengimplementasikan ranking, weighted scheduler dan workflow arsip. UI menghubungkan form, profil dan jadwal tetap. Integrasikan login Supabase, cloud PRO, dan BYOK terenkripsi. | Jalur FREE offline, login Supabase, kuota PRO, dan BYOK terverifikasi. Cap, deadline, blok minimum, dan histori reopen punya bukti tes. |
| **3 — Focus dan replan** (~5 jam) | Core menghubungkan service, alarm, Room, Smart Shift, dan dynamic reschedule. UI menyambungkan countdown, aksi overtime, input tambahan, izin, dan peringatan. QA menguji race dan lifecycle perangkat. | Cutoff, kedua keputusan, pergeseran sekitar jadwal tetap, serta “Saya Lagi Sibuk” lulus integrasi dan pemulihan. |
| **4 — Audit dan kandidat rilis** (~4 jam) | Lead dan QA memeriksa signed APK sideload, staging free tier, aksesibilitas, dan regresi. Architect memeriksa invariant scheduler. | APK release ditandatangani dan diuji di HP. Google/email, kuota Supabase, AI free tier, Doze/reboot, dan permission matrix punya bukti. Distribusikan via sideload atau GitHub Releases gratis. |

## Urutan handoff Wave 1

1. **Lead (~20 menit).** Inisialisasi repo dan commit kontrak Rp 0. Kunci package `com.tazer.taskorganizer` dan baseline toolchain. Kirim lokasi kontrak dan graph ke pemilik.
2. **Core dan Backend (paralel, ~90 menit).** Core melakukan bootstrap Room dan kontrak repository. Backend melakukan bootstrap auth Supabase, tier/kuota, dan analyzer mock. Masing-masing menulis di area sendiri.
3. **Architect (~30 menit).** Kirim fixture scheduler berikut output ekspektasi ke Core dan QA. Ajukan revisi kontrak lewat Lead jika fixture menemukan ambiguitas.
4. **UI (~60 menit).** Hubungkan shell setelah struktur `:app` tersedia. Ambil schema UI dari kontrak dan gunakan state fixture sampai repository siap.
5. **QA dan Lead (~30 menit).** Jalankan gate Wave 1, selaraskan graph gabungan, dan keluarkan satu handoff yang menetapkan pemilik Wave 2. Kredensial eksternal diperlukan saat uji staging sesuai wave. Gunakan mock untuk gate lokal.

## Awalan system prompt untuk semua peran

> Kamu anggota tim Tazer. Baca `AGENTS.md` dan kontrak `docs/` sesuai area tugas. Sebelum membuka file lintas modul, jalankan `graphify query`, `path`, atau `explain`. Perbarui graph dengan `--update` setelah perubahan struktur. Terapkan `ponytail` dan pertahankan jalur kode minimal yang memenuhi kontrak. Jalankan gate yang relevan. Gunakan `i-have-adhd` untuk semua komunikasi dan handoff. Maksimal lima butir per kelompok dan satu aksi berikutnya di akhir.

Tempel awalan itu sebelum instruksi peran. Lead memakai model yang ditetapkan pada setiap dispatch jika tersedia di host. Catat model dan effort aktual pada handoff.

| Peran | Model dan effort | System prompt tambahan |
| --- | --- | --- |
| Orchestrator (Tech Lead / PM) | `claude-sonnet-5-5` medium, high untuk handover dan gate rilis | Kamu menjaga kontrak lintas modul, urutan wave, ownership, integrasi dan gate. Delegasikan area tanpa tumpang tindih. Audit bukti sebelum membuka wave berikutnya. |
| System & Algorithm Architect | `claude-sonnet-5-5` high | Kamu menjaga rumus ranking, weighted allocation dan aturan transisi. Kirim fixture kecil dengan input, output dan invariant. Selaraskan perubahan kontrak dengan Lead, Core, dan QA. |
| Backend & AI Integration Dev | `claude-sonnet-5-5` medium, high untuk Auth, RLS, verifikasi tier/kuota, anti-bypass dan keamanan Workers | Kamu memiliki `backend/`. Gunakan verifikasi identitas dan entitlement server pada jalur analyzer, Gemini/Groq dengan output ketat, serta tes batas kepercayaan. Serahkan contoh respons dan error untuk Android. |
| Android Core & Scheduler Dev | `claude-sonnet-5-5` high | Kamu memiliki `data/`, `domain/`, dan `timer/`. Terapkan state dan mutasi Room transaksional. Berikan state, event dan fixture kepada UI dan QA, serta verifikasi pemulihan timer. |
| Android UI/UX (Compose) Dev | `claude-haiku-5-5` medium untuk komponen ringan; `claude-sonnet-5-5` medium untuk layar kompleks, state timer, dialog dan polish `impeccable` | Kamu memiliki `ui/`. Buat layar Compose Material 3 untuk dua tab, input dan aksi timer. Gunakan state ViewModel serta label dan fokus yang dapat diakses. |
| Visual & UX Reference Analyst | `claude-sonnet-5-5` high; `claude-opus-5-5` high hanya untuk audit konflik design system atau alur interaksi kompleks | Kamu memiliki `docs/references/`. Analisis foto, screenshot, atau video referensi UI dan alur kerja dari owner. Simpan aset dengan nama deskriptif setelah memeriksa ukuran, lisensi, dan watermark. Susun token visual (palet Light/Dark, shadow/elevasi, tipografi, hierarki, kontras WCAG, pemetaan komponen), lalu kirim saran konkret kepada UI Dev, Core Dev, dan Architect sesuai `impeccable` dan `ponytail`. Kode UI menunggu persetujuan mockup dari owner. |
| QA & Security Reviewer | `claude-sonnet-5-5` high | Kamu memiliki rencana verifikasi dan temuan. Audit entitlement fail-closed, pembatasan UID, graph, APK, Doze, race, dan regresi scheduler. Berikan hasil aktual dan langkah perbaikan kepada pemilik. |

## Eskalasi ke Claude Opus 5.5

1. Eskalasi hanya jika ada satu pemicu. **Two-Strike Rule** berlaku saat `claude-sonnet-5-5` `high` gagal memperbaiki bug atau tes yang sama setelah dua percobaan yang dicatat. Kasus kompleks weighted allocation/Smart Shift atau race `ForegroundService`–`AlarmManager`–aksi notifikasi dalam Doze menjadi pemicu mandiri. Audit keamanan kritis menjelang rilis menjadi pemicu mandiri.
2. Lead dispatch `claude-opus-5-5` dengan `high` dan ruang lingkup terbatas. Sertakan invariant, fixture, perilaku aktual/ekspektasi, hasil query graph, serta percobaan yang relevan. Naikkan ke `xhigh` hanya jika `high` masih buntu dan bukti menyebut penyebab kebuntuan.
3. Pemilik modul menerapkan perbaikan yang diterima dan menjalankan ulang tes. Lead mencatat penyebab, model/effort, hasil, dan keputusan gate.

## Gate rilis 1.3.0

1. Verifikasi migrasi SQL dengan dua akun PostgreSQL lokal, RPC staging dan percobaan akses anon. Terapkan migrasi `20261126000000_cloud_sync.sql` sebelum smoke APK.
2. Deploy Worker dengan `npx wrangler deploy --keep-vars`. Uji readiness, penolakan 401, respons analyzer lama, dan respons dengan header saran checklist.
3. Uji Room v3 ke v4, antrean offline, replay konflik, checklist, kalibrasi, dan widget dari layar utama. Build APK signed versi 1.3.0 kode 9 dan uji upgrade dari APK 1.2.2 dengan akun, tugas, acara, dan pengaturan yang sudah tersimpan.

## Gate rilis 1.4.0

1. Uji parser iCalendar, pratinjau, impor ulang, dan replikasi hasil ke staging. Uji agregat statistik, rentang tujuh tanggal, akurasi, serta streak.
2. Uji tiga preset pada Scheduler dan simulasikan DND tanpa akses, aktif, jeda, selesai, dan pemulihan. Periksa Statistik dan impor file pada emulator API 37 di Light dan Dark.
3. Build APK signed 1.4.0 kode 10. Uji upgrade dari signed 1.3.0, termasuk akun, Cloud Sync, checklist, kalender, tema, dan widget yang telah dipasang. Periksa Worker `/health`, lint, tanda tangan, secret scan, dan graph.

## Gate rilis 1.5.0

1. Uji migrasi kuota 100 pada PostgreSQL dengan pemakaian lama, dua UID, akses role, dan permintaan paralel. Terapkan migrasi di Supabase Free, lalu cek angka dan satu pengurangan kuota melalui Worker.
2. Aktifkan provider Google di Supabase. Uji PKCE S256, state, callback sukses atau dibatalkan, email login, dan status provider di staging.
3. Audit working tree, seluruh sejarah Git, dan APK sebelum publikasi. Build signed 1.5.0 kode 11 dan uji upgrade dari 1.4.0. Publikasikan APK dan checksum, lalu cek endpoint GitHub Releases serta status Cek Update pada aplikasi.

## Gate rilis 1.6.0

1. Uji `/extract-task-image` untuk token salah/kedaluwarsa/tercabut, tier FREE/PRO/BYOK, MIME/signature/base64, batas 2 MiB, kuota bersama dan persaingan, output AI tidak valid, serta nol panggilan AI pada cabang tanpa otorisasi. Deploy Worker lalu uji `/health`, `401`, `400`/`413`, dan satu scan nyata ke Gemini Free.
2. Uji kompresi dan EXIF di perangkat, prefill form dari Photo Picker tanpa menyimpan sebelum ditinjau, serta pesan non-PRO. Uji Pengaturan Daily Briefing dengan spinner, persistensi, alarm terpasang dan dibatalkan, channel terpisah, deep link Jadwal/Statistik, serta penjadwalan ulang setelah reboot.
3. Build APK signed 1.6.0 kode 12 dan uji upgrade dari signed 1.5.0 dengan akun dan data. Lakukan smoke test Light dan Dark, lint, secret scan histori dan APK, lalu publikasikan rilis dan cek Cek Update dari 1.5.0.

## Gate rilis 1.7.0

1. Uji pencarian dan filter di dua tab, determinisme dan kontras warna mata kuliah, generator `.ics` beserta round-trip ke parser, backup JSON tanpa secret dengan deduplikasi, codec paket tugas, dan formatter catatan rilis di JVM.
2. Uji di emulator API 37: alur search/filter, warna chip pada Light dan Dark serta warna pilihan, ekspor `.ics` dan backup lewat SAF, restore dengan pratinjau, paket tugas melalui Share Sheet dan tautan, serta catatan rilis yang bersih. Lakukan juga upgrade signed dari 1.6.0.
3. Build APK signed 1.7.0 kode 13. Jalankan lint, secret scan histori dan APK, smoke test Light dan Dark, lalu publikasikan rilis dan cek `releases/latest`.

## Gate rilis 1.8.0

1. Uji `/extract-schedule-image` untuk auth, tier, MIME/signature, batas ukuran, kuota bersama, dan validasi output. Deploy Worker lalu uji `/health`, `401`, `400`, `413`, dan satu ekstraksi KRS nyata ke Gemini Free.
2. Di JVM, uji parser CSV (pemisah `,` dan `;`), XLSX OpenXML, ekstraksi jadwal JSON, deduplikasi dan bentrok, serta `.ics` dan backup untuk jam selesai dan warna acara. Di emulator API 37, uji tombol `+` Jadwal, halaman Acara dan Jadwal Tetap, lima sumber impor dengan ikon vektor, pratinjau terpadu, satu tombol Pengaturan fokus, migrasi Room v5 dengan Cloud Sync, serta upgrade signed dari 1.7.0.
3. Build APK signed 1.8.0 kode 14. Jalankan lint, secret scan, smoke test Light dan Dark, lalu publikasikan rilis.

## Gate rilis 1.9.0

1. Terapkan [spesifikasi UI neumorphic](UI_NEUMORPHIC_SPEC.md) setelah owner menyetujui mockup. Tidak ada perubahan Room, Cloud Sync, atau backend. Remember Me hanya mengatur penyimpanan sesi di `SessionVault`.
2. Di JVM, uji kontras token neumorphic dan warna mata kuliah di kanvas baru. Di emulator API 37, uji tombol `+` yang kini ada di Kalender (menggantikan "Tambah acara" dan tidak ada di Jadwal), login dengan Sign In, Sign Up, Remember Me, Forgot Password?, dan Continue with Google tanpa Discord/Facebook, render neumorphic Light dan Dark (piksel kanvas, gradien, dan bayangan), serta seluruh tes regresi. Backend tetap 31/31.
3. Build APK signed 1.9.0 kode 15 dengan keystore yang sama, lalu jalankan `apksigner verify`, lint debug/release dengan 0 error, dan secret scan riwayat Git serta APK. Uji upgrade in-place dari 1.8.0 (sesi, tugas, acara, jadwal tetap, tema, dan widget), smoke test Light dan Dark dengan screenshot komposit, publikasikan rilis, lalu verifikasi Cek Update dari 1.8.0.

## Gate rilis 1.9.1

1. Terapkan polish hasil uji owner di HP: empat tab ditambah gerigi Pengaturan, Back berupa ikon biru, segmen Light/Dark/System, kolom cari ramping, panel Filter (mata kuliah, kesulitan, deadline, durasi, urutan, sub-tugas, live count, Reset), inset kartu tugas, dan highlight biru untuk aksi utama ([spesifikasi UI](UI_NEUMORPHIC_SPEC.md#polish-v191)).
2. Build debug memakai `applicationIdSuffix ".debug"` (`com.tazer.taskorganizer.debug`, label Tazer Debug, skema `tazer-debug://`, FileProvider `${applicationId}.scan`). Tes instrumented berjalan di HP fisik owner, lalu paket `.debug` dan `.debug.test` dihapus. Tes JVM mencakup multi-filter dan sort.
3. Build APK signed 1.9.1 kode 16, lalu jalankan lint, `apksigner verify`, dan secret scan. Uji upgrade in-place 1.9.0 → 1.9.1 di HP owner hanya dengan `install -r`, ambil screenshot Light dan Dark, lalu publikasikan rilis.

## Template handoff

1. **Wave [N] — Langkah [X] dari [Y] (~[Z] menit).** [Aksi atau hasil inti, model dan effort aktual.]
2. **Selesai.** [File atau simbol yang berubah dan hasil nyata.]
3. **Verifikasi.** [Perintah graph, build atau tes, serta hasil.]
4. **Terbuka.** [Kendala atau keputusan tertunda.]
5. **Aksi berikutnya.** [Satu tindakan dan satu pemilik.]
