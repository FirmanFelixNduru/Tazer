# Tazer (Task Organizer)

## Aturan global

1. Gunakan [kontrak data dan API](docs/DATA_AND_API_CONTRACT.md) untuk skema, status, dan protokol. Gunakan [spesifikasi scheduler](docs/SCHEDULER_SPEC.md) untuk alokasi dan transisi. Gunakan [rencana eksekusi](docs/WAVE_PLAN.md) untuk kepemilikan dan urutan kerja. `docs/` adalah Single Source of Truth. Ubah kontrak bersama pemilik terkait sebelum mengubah perilaku lintas batas.
2. Terapkan `graphify`, `ponytail`, dan `i-have-adhd` di awal setiap penugasan. Skill wajib keempat, `impeccable`, berlaku untuk desain, redesign, hierarki, tipografi, warna, dan polish UI Android Compose. Baca `C:/Users/HP/.gemini/config/skills/impeccable/SKILL.md` sebelum mengerjakan UI dan verifikasi hasil render pada perangkat atau emulator. Ikuti perintah dan format handoff di bawah.
3. Jaga batas modul. Android mengelola tugas, jadwal, dan timer lokal per UID. Backend mengelola verifikasi identitas, tier dan kuota Supabase, dan proxy AI. Android memakai Kotlin, Jetpack Compose Material 3, ViewModel, dan Room dalam satu Gradle module `:app`.
4. Jaga biaya Rp 0 hingga rilis. Pakai layanan free tier tanpa kartu kredit dan distribusikan APK sideload. Simpan kunci server Gemini/Groq dan secret Supabase hanya di server. Backend memverifikasi sesi Supabase serta tier dan kuota sebelum AI. Kunci BYOK milik user dienkripsi di perangkat dan dipakai langsung ke provider.
5. Tutup tugas setelah pemeriksaan yang relevan lulus dan hasilnya tercatat. Eskalasi ke `claude-opus-5-5` hanya setelah dua percobaan `claude-sonnet-5-5` `high` gagal pada bug atau tes yang sama, saat ada kasus scheduler/lifecycle atau race kompleks, atau saat audit keamanan kritis menjelang rilis. Mulai dengan `high`. Pakai `xhigh` hanya bila `high` masih buntu. Ikuti dispatch dan bukti yang diminta di [rencana eksekusi](docs/WAVE_PLAN.md).

## Tim Orkestra Claude 5.5

Sejak 26 November 2026 seluruh peran dikerjakan Tim Orkestra Claude. Orchestrator mencatat model dan effort aktual pada setiap dispatch dan handoff.

| Peran | Model dan effort |
| --- | --- |
| Orchestrator (Tech Lead / PM) | `claude-sonnet-5-5` medium; high untuk handover dan gate rilis |
| System & Algorithm Architect | `claude-sonnet-5-5` high |
| Backend & AI Integration Dev | `claude-sonnet-5-5` medium; high untuk Auth, RLS, tier/kuota, dan keamanan Workers |
| Android Core & Scheduler Dev | `claude-sonnet-5-5` high |
| Android UI/UX (Compose) Dev | `claude-haiku-5-5` medium untuk komponen ringan; `claude-sonnet-5-5` medium untuk layar kompleks dan polish `impeccable` |
| QA & Security Reviewer | `claude-sonnet-5-5` high |
| Visual & UX Reference Analyst | `claude-sonnet-5-5` high; `claude-opus-5-5` high hanya untuk audit konflik design system atau alur interaksi kompleks |
| Eskalasi | `claude-opus-5-5` high, `xhigh` hanya saat buntu total; pemicu sesuai aturan 5 |

Visual & UX Reference Analyst menganalisis foto, screenshot, atau video referensi UI dan alur kerja dari owner. Analyst menyimpan aset dengan nama deskriptif di `docs/references/`, memeriksa ukuran, lisensi, dan watermark sebelum aset ikut Git, lalu menyusun spesifikasi token visual: palet Light/Dark, shadow dan elevasi, tipografi, hierarki, kontras WCAG, serta pemetaan komponen. Saran konkret diteruskan kepada Android UI/UX Dev, Android Core Dev, dan System Architect sesuai `impeccable` dan `ponytail`. Kode UI baru diubah setelah owner menyetujui mockup.

AI di dalam aplikasi tetap memakai Gemini Free Tier melalui Cloudflare Workers dan Supabase Free. Model Claude hanya dipakai untuk pengembangan.

## Batas modul dan struktur target

```text
Tazer/
├── AGENTS.md
├── docs/
│   ├── DATA_AND_API_CONTRACT.md
│   ├── SCHEDULER_SPEC.md
│   ├── WAVE_PLAN.md
│   └── references/   # aset referensi UI dan spesifikasi token visual
├── graphify-out/
│   ├── graph.json
│   └── GRAPH_REPORT.md
├── android/
│   └── app/src/
│       ├── main/java/<namespace>/
│       │   ├── ui/       # Compose, ViewModel, UiState
│       │   ├── domain/   # ranking, scheduler, analyzer offline, transisi murni
│       │   ├── data/     # Room, repository, Supabase Auth, HTTP
│       │   └── timer/    # foreground service, alarm, receiver, notifikasi
│       ├── test/
│       └── androidTest/
└── backend/
    ├── src/              # HTTP, auth, entitlement, analyze
    └── test/
```

1. Arah dependensi Android `ui → data/domain`, `timer → data/domain`, `data → domain`. Domain murni Kotlin dan menerima waktu sebagai parameter. Repository menjadi jalur bersama UI, service, dan receiver untuk mutasi Room.
2. Pakai Node.js + TypeScript dan Supabase Free (Auth + PostgreSQL). Simpan tier dan kuota pada database cloud. Room melayani UI dan mutasi offline, lalu mereplikasi data akun sesuai kontrak sinkronisasi. Namespace dan package ID Android adalah `com.tazer.taskorganizer`. Distribusikan signed APK lewat sideload/GitHub Releases gratis.
3. Terapkan `ponytail`. Gunakan helper yang ada, lalu API platform atau pustaka yang sudah dipakai. Tambah dependensi atau abstraksi saat ada kebutuhan konkret. Jaga validasi batas kepercayaan, aksesibilitas, migrasi, dan penanganan kegagalan.

## Workflow `graphify`

1. Mulai dari root. Jalankan `graphify query`, `graphify path`, atau `graphify explain` dengan entitas dan batas yang relevan sebelum membuka atau mengedit file lintas modul. Gunakan hasilnya untuk memilih kontrak dan caller yang diperiksa. Simpan perintah dan temuan di handoff.
2. Indeks `docs/`, `android/`, dan `backend/` sebagai graph persisten di `graphify-out/`. Boleh sertakan `AGENTS.md` sebagai peta modul. Kecualikan secret, kredensial, data pengguna, dependensi vendor, build output, dan graph yang dihasilkan.
3. Jalankan `/graphify . --update` setiap selesai perubahan struktur atau kontrak. Untuk CLI yang memakai subperintah, jalankan `graphify update .` untuk AST dan selesaikan ekstraksi semantik Markdown jika `graphify check-update .` melaporkan pekerjaan tertunda. Jalankan query ulang untuk memeriksa simbol atau hubungan yang berubah.
4. Saat bootstrap tanpa graph, rekam hasil query awal, buat graph dari corpus pertama, lalu gunakan graph untuk pekerjaan berikutnya. Di kerja paralel, Orchestrator menerbitkan graph gabungan setelah menerima perubahan dari pemilik sumber.

## Pemeriksaan sebelum handoff

1. Catat perintah dan hasil build, lint, dan tes yang berlaku untuk area perubahan. Core wajib menguji prioritas, cap, redistribusi, jeda, batas deadline, konflik fixed, dan replan deterministik. Perubahan data wajib menguji transaksi Room dan migrasi bila skema berubah.
2. UI wajib memeriksa jalur dua tab, edit estimasi, reopen, hapus arsip, dialog keputusan timer, serta label dan fokus aksesibilitas. Timer wajib menguji alarm/aksi duplikat, process death, reboot, Doze, serta izin exact alarm dan notifikasi diizinkan maupun ditolak.
3. Backend wajib menguji token salah/kedaluwarsa/tercabut, tier FREE/PRO/BYOK, kuota habis dan persaingan request, serta parsing output AI dengan mock. Pastikan cabang tanpa otorisasi menghasilkan nol panggilan AI. Gate rilis mencakup inspeksi APK dan secret scan.
4. Catat pemeriksaan yang belum dapat dijalankan beserta penyebabnya. Gate terkait tetap terbuka sampai ada bukti pada lingkungan yang sesuai.

## Handoff `i-have-adhd`

1. **Wave N — Langkah X dari Y (~Z menit).** Nyatakan aksi atau hasil inti pada baris pertama.
2. **Selesai.** Beri lokasi perubahan dan hasil yang dapat diperiksa.
3. **Verifikasi.** Tulis perintah graph/test dan hasilnya.
4. **Terbuka.** Sebutkan keputusan dan kendala yang masih perlu ditangani.
5. **Aksi berikutnya.** Beri satu tindakan yang bisa langsung dimulai, beserta pemiliknya.
