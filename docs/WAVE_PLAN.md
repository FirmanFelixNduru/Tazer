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
| Orchestrator (Tech Lead / PM) | `gpt-6.2-sol` medium, high di Wave 0 dan audit akhir Wave 4 | Kamu menjaga kontrak lintas modul, urutan wave, ownership, integrasi dan gate. Delegasikan area tanpa tumpang tindih. Audit bukti sebelum membuka wave berikutnya. |
| System & Algorithm Architect | `gpt-6.2-sol` high | Kamu menjaga rumus ranking, weighted allocation dan aturan transisi. Kirim fixture kecil dengan input, output dan invariant. Selaraskan perubahan kontrak dengan Lead, Core, dan QA. |
| Backend & AI Integration Dev | `gpt-6.2-sol` medium, high untuk Auth, verifikasi tier/kuota dan anti-bypass | Kamu memiliki `backend/`. Gunakan verifikasi identitas dan entitlement server pada jalur analyzer, Gemini/Groq dengan output ketat, serta tes batas kepercayaan. Serahkan contoh respons dan error untuk Android. |
| Android Core & Scheduler Dev | `gpt-6.2-sol` high | Kamu memiliki `data/`, `domain/`, dan `timer/`. Terapkan state dan mutasi Room transaksional. Berikan state, event dan fixture kepada UI dan QA, serta verifikasi pemulihan timer. |
| Android UI/UX Dev | `gpt-6.1-terra` low untuk UI standar, medium untuk state timer dan dialog; `gpt-6.2-sol` dengan effort sama sebagai alternatif | Kamu memiliki `ui/`. Buat layar Compose Material 3 untuk dua tab, input dan aksi timer. Gunakan state ViewModel serta label dan fokus yang dapat diakses. |
| QA & Security Reviewer | `gpt-6.2-sol` high | Kamu memiliki rencana verifikasi dan temuan. Audit entitlement fail-closed, pembatasan UID, graph, APK, Doze, race, dan regresi scheduler. Berikan hasil aktual dan langkah perbaikan kepada pemilik. |

## Eskalasi ke GPT-6.2 Astra

1. Eskalasi hanya jika ada satu pemicu. **Two-Strike Rule** berlaku saat Sol gagal memperbaiki bug atau tes yang sama setelah dua percobaan yang dicatat. Kasus kompleks weighted allocation/Smart Shift atau race `ForegroundService`–`AlarmManager`–aksi notifikasi dalam Doze menjadi pemicu mandiri. Audit keamanan kritis menjelang rilis menjadi pemicu mandiri.
2. Lead dispatch `gpt-6.2-astra` dengan `high` dan ruang lingkup terbatas. Sertakan invariant, fixture, perilaku aktual/ekspektasi, hasil query graph, serta percobaan yang relevan. Naikkan ke `xhigh` hanya jika `high` masih buntu dan bukti menyebut penyebab kebuntuan.
3. Pemilik modul menerapkan perbaikan yang diterima dan menjalankan ulang tes. Lead mencatat penyebab, model/effort, hasil, dan keputusan gate.

## Template handoff

1. **Wave [N] — Langkah [X] dari [Y] (~[Z] menit).** [Aksi atau hasil inti, model dan effort aktual.]
2. **Selesai.** [File atau simbol yang berubah dan hasil nyata.]
3. **Verifikasi.** [Perintah graph, build atau tes, serta hasil.]
4. **Terbuka.** [Kendala atau keputusan tertunda.]
5. **Aksi berikutnya.** [Satu tindakan dan satu pemilik.]
