# Kontrak data dan API

Kontrak API v1 dan skema Room v4. Perilaku perencanaan dan timer mengikuti [spesifikasi scheduler](SCHEDULER_SPEC.md).

## Room

Gunakan ID UUID string untuk tugas, blok tetap, dan sesi, serta Supabase Auth UID string untuk `ownerUid`. Waktu absolut memakai epoch milidetik `Long` di Room dan RFC 3339 UTC pada HTTP. Durasi kerja tersimpan dalam menit `Int`, durasi aktual dalam milidetik `Long`, dan jam rutin dalam menit lokal sejak tengah malam. Semua kueri, mutasi, dan callback Android dibatasi ke UID pemilik. `?` berarti nullable.

| Entity | Kolom |
| --- | --- |
| `UserProfile` | `ownerUid` (PK), `zoneId` (IANA), `activeStartMinute`, `activeEndMinute`, `busyUntilAt?` |
| `Task` | `id` (PK), `ownerUid` (FK), `title`, `course`, `description`, `deadlineAt`, `difficultyScore`, `initialEstimatedMinutes`, `remainingMinutes`, `analysisSource`, `status`, `createdAt`, `completedAt?`, `analyzerBaselineMinutes?` |
| `Subtask` | `id` (PK), `ownerUid`, `taskId` (FK komposit), `title`, `completed`, `position` |
| `FixedSchedule` | `id` (PK), `ownerUid` (FK), `label`, `kind`, `dayOfWeek` (ISO 1–7), `startMinute`, `endMinute` |
| `WorkSession` | `id` (PK), `taskId` dan `ownerUid` (FK komposit), `state`, `plannedStartAt`, `plannedEndAt`, `plannedMinutes`, `startedAt?`, `endedAt?`, `actualWorkMs?`, `timerBootId?`, `activeSinceElapsedMs?`, `cutoffElapsedMs?`, `lastObservedWorkMs?`, `revision`, `pausedRemainingMs?`, `pausedFromState?`, `workBeforeSegmentMs?`, `continuousWorkBeforeSegmentMs?`, `lastObservedAt?` |
| `CalendarEvent` | `id` (PK), `ownerUid` (FK), `title`, `startEpochDay`, `endEpochDay`, `startMinute?`, `description` |

Migrasi Room v1 ke v2 menambah tabel `CalendarEvent` dan indeks `(ownerUid,startEpochDay)`. Kedua tanggal sipil inklusif memakai `LocalDate.toEpochDay()`. `startMinute` null berarti seharian, atau menit lokal 0–1439 pada tanggal mulai. CalendarEvent disimpan per UID dan tampil bersama sesi, jadwal tetap, serta deadline tugas di kalender. Kapasitas scheduler tetap mengikuti blok tetap dan profil kerja.

Migrasi Room v2 ke v3 menambah lima kolom nullable pada `WorkSession`. Saat `PAUSED`, `pausedRemainingMs` menyimpan sisa waktu bertanda dan `pausedFromState` menyimpan status asal. `workBeforeSegmentMs` dan `continuousWorkBeforeSegmentMs` membawa kerja kumulatif dan kontinu ke segmen berikutnya. `lastObservedAt` menyimpan waktu dinding checkpoint.

Migrasi v3 ke v4 mempertahankan baris lama, menambah baseline analyzer nullable dan FK `Subtask(taskId,ownerUid)` dengan cascade dan indeks. Checklist mendukung 40 langkah per tugas, masing-masing 1–120 karakter setelah trim. Tugas baru dan perubahan checklist pada formulir disimpan dalam satu transaksi. Perubahan dari sesi fokus dipertahankan jika formulir lama belum mengedit checklist.

1. `Task.status` tersimpan sebagai `PENDING | IN_PROGRESS | DONE`. `OVERDUE` adalah status tampilan untuk tugas terbuka saat `now >= deadlineAt`, termasuk tugas yang masih punya timer berjalan. `WorkSession.state` memakai `PENDING | RUNNING | OVERTIME | EXTENDED | PAUSED | DONE | CANCELLED`. `REOPENED` adalah aksi pada Task. `FixedSchedule.kind` memakai `ROUTINE | BREAK` dan `analysisSource` memakai `OFFLINE | AI`. `SubscriptionTier` memakai `FREE | PRO | BYOK`.
2. Rentang harian dan blok rutin memakai `[start,end)` dengan `0 <= start < end <= 1440`. Pecah input lintas tengah malam menjadi dua blok rutin, dan validasi profil v1 sebagai satu rentang aktif dalam satu hari. Konversi zona waktu mengikuti spesifikasi scheduler.
3. `difficultyScore` 1–5, `initialEstimatedMinutes` 1–10080. Estimasi awal adalah nilai yang dikonfirmasi user saat membuat tugas. Setelah dibuat, penyesuaian mengubah `remainingMinutes` dan mempertahankan baseline. `remainingMinutes` 0 hanya untuk `DONE`; keputusan `Belum Selesai` dan reopen menerima sisa yang diperkirakan user, 1–10080. Transisi selesai mengisi `completedAt`, membatalkan rencana mendatang untuk tugas itu, dan mencatat sesi aktual bila ada.
4. Indeks `Task(ownerUid,status,completedAt)` dan unique `Task(id,ownerUid)` untuk FK komposit; `FixedSchedule(ownerUid,dayOfWeek)` dan `WorkSession(ownerUid,state,plannedStartAt)`, serta indeks `WorkSession(taskId,ownerUid)` untuk FK. Hapus arsip hanya jika `DONE` dan cascade ke sesi. Pakai transaksi Room untuk perubahan tugas/sesi/profil terkait dan pemeriksaan maksimal satu sesi aktif per UID. `actualWorkMs` menyimpan durasi final untuk sesi `DONE` yang telah dimulai. `revision` berubah jika status atau batas waktu yang terkait callback berubah.
5. Tab **Belum Selesai** urut sesuai ranking, lalu tampilkan judul, matkul, difficulty, baseline, sisa, deadline, dan awal sesi `PENDING` berikutnya jika ada. Tab **Selesai** urut `completedAt DESC, id ASC`, tampilkan baseline, jumlah sesi `DONE` yang telah dimulai, dan total `actualWorkMs` termasuk riwayat sebelum reopen. Reopen mengosongkan `completedAt`, menerima estimasi sisa, mempertahankan histori, dan memicu replan. Untuk perbandingan, bulatkan total aktual setelah penjumlahan.


## Autentikasi dan endpoint HTTP

Supabase Free menyediakan Auth dan PostgreSQL. Android memakai email/password dengan verifikasi dan reset email, serta Google OAuth melalui Supabase untuk integrasi Wave 2. Backend memakai Supabase Auth `GET /auth/v1/user` dengan access token Bearer dan project publishable key. Ambil UID dari respons Supabase yang sah dan tolak akun tanpa email terverifikasi. Pada `401`, Android mencoba refresh satu kali. Room selalu difilter UID aktif. Sign-out menutup akses UID lama dan pada Wave 3 menghentikan timer serta alarm akun lama. Debug Wave 1 memakai akun demo lokal dan mode HTTP mock yang hanya bind loopback. Release memakai HTTPS dan konfigurasi Supabase nyata. [Supabase Auth](https://supabase.com/docs/guides/auth) · [Supabase Free](https://supabase.com/pricing)

| Endpoint | Request | Respons sukses |
| --- | --- | --- |
| `GET /auth/session` | Authorization: Bearer | `200 { "uid": "u1" }` |
| `GET /subscription` | Authorization: Bearer | `200 { "tier": "PRO", "monthlyLimit": 20, "remaining": 19, "resetsAt": "2026-12-01T00:00:00Z" }` |
| `POST /analyze-task` | Bearer dan JSON input | `200 { "difficultyScore": 4, "estimatedMinutes": 150 }` |
| `GET /health` | Tanpa autentikasi | `200` jika binding siap atau `503` jika belum lengkap. Respons hanya memuat `ready` dan boolean konfigurasi publik `SUPABASE_URL`, `SUPABASE_PUBLISHABLE_KEY`, `GEMINI_MODEL`. Pemeriksaan ini membaca konfigurasi tanpa memanggil layanan eksternal. |

### Entitlement akun dan kuota bulanan

1. `FREE` menggunakan analisis offline. `PRO` memakai AI server, default 20 permintaan per UID per bulan UTC. Operator menetapkan PRO gratis melalui Supabase. `BYOK` memakai provider langsung dari Android dengan key milik user, tanpa konsumsi kuota server. Default untuk akun tanpa row adalah FREE. FREE/BYOK mendapat `monthlyLimit: 0` dan `remaining: 0`.
2. Simpan tier di `account_entitlements(user_id, tier)` dan penggunaan di `analysis_usage(user_id, month_start, used)`. Hanya role server memiliki akses. Server menentukan tier dan mengambil slot kuota lewat RPC PostgreSQL transaksional. Konsumsi satu slot per permintaan valid yang diterima, termasuk jika provider kemudian gagal. Fallback Gemini ke Groq tidak mengurangi slot tambahan. Waktu reset adalah awal bulan UTC berikutnya.
3. Backend memverifikasi sesi dan entitlement untuk setiap analisis sebelum provider dipanggil. Jika verifikasi Auth atau database tidak tersedia, kembalikan `503`. Jika tier bukan PRO, kembalikan `403`. Jika kuota PRO habis, kembalikan `429`. Mock lokal meniru tier dan kuota dengan penyimpanan in-memory.
4. Kunci project khusus server berada di env/secret store. Aplikasi memuat konfigurasi publik Supabase saja. Untuk BYOK Wave 2, simpan ciphertext AES-GCM dengan kunci dari Android Keystore per akun dan eksklusi backup. Kirim key hanya melalui HTTPS langsung ke provider yang dipilih user. User dapat menghapusnya. Jelaskan penggunaan konten oleh free tier sebelum pengiriman.
5. Server Node.js + TypeScript memakai Web Fetch API untuk handler. Bootstrap mendukung Node lokal. Cloudflare Workers Free menjadi target adapter deploy, setelah pengujian staging. Supabase, Cloudflare, Google AI Studio, dan Groq dipakai dengan kuota gratis dan tanpa aktivasi billing atau kartu kredit.

### Sinkronisasi cloud

1. Room adalah sumber pembacaan UI. Trigger SQLite mencatat perubahan dan tombstone per UID riil dalam transaksi yang sama dengan mutasi. Migrasi mengantrekan data lama untuk sinkronisasi awal. Profil default tanpa riwayat menunggu pull pertama agar profil cloud lebih dahulu diadopsi. Demo dan akun mock tetap lokal.
2. Supabase Auth memberi JWT untuk `POST /rest/v1/rpc/tazer_sync_push` dengan array `p_rows`. Setiap item memiliki `kind`, `row_id`, `version`, `writer`, dan `payload`. `POST /rest/v1/rpc/tazer_sync_pull` menerima `{"p_after":0,"p_limit":24}`. Push mengembalikan jumlah perubahan yang menang. Pull mengembalikan baris dalam urutan `change_id`, termasuk `deleted_at` untuk tombstone dan `payload:null`. ID UUID dan isi payload mengikuti field Room tanpa ID atau UID di dalam payload. Enam jenis memakai `profile`, `tasks`, `subtasks`, `fixed_schedules`, `calendar_events`, `work_sessions`.
3. Baris menang berdasarkan pasangan tertinggi `(version, writer)`. Versi lokal naik dari revisi yang sudah diketahui dan writer UUID persisten per database memecahkan seri. Server menyusun feed per akun dalam transaksi dan menolak permintaan tanpa UID. RLS tiap tabel membatasi `auth.uid() = user_id`; authenticated membaca baris sendiri dan menulis lewat RPC. Role anon tidak memperoleh akses.
4. Urutan sync meliputi pull, kirim batch antrean, lalu pull hasil terbaru. Retry idempoten. ACK hanya menghapus status dirty untuk pasangan versi/writer yang dikirim. Impor dan cursor disimpan atomik. Anggota aplikasi dapat memicu sinkronisasi di Pengaturan, dan antrean dicoba lagi setelah koneksi tervalidasi kembali atau saat aplikasi dibuka lagi. Akun tidak perlu tier PRO untuk sinkronisasi.
5. `PENDING`, `DONE`, dan `CANCELLED` mereplikasi rencana dan riwayat durasi aktual. Timer aktif/dijeda, revisi notifikasi, dan checkpoint monotonic dikelola perangkat yang memulainya. Perubahan cloud yang akan menimpa sesi itu atau tugasnya diambil setelah sesi ditutup. Profil, tugas, dan blok rutin yang diimpor memicu perencanaan ulang di perangkat.

### Impor iCalendar

1. Kalender dan Jadwal Tetap membuka dokumen melalui Android Storage Access Framework. Parsing dan pratinjau memakai file lokal. Simpan setelah konfirmasi per akun.
2. Baca `VEVENT` beserta UID, judul, awal, akhir, lokasi dan deskripsi. Buka lipatan baris dan dekode escape teks iCalendar. Normalisasi waktu UTC atau `TZID` ke zona profil. Tanggal akhir all-day iCalendar eksklusif dan dikonversi ke tanggal akhir inklusif Room. Jam satu kali mengikuti tanggal sipil profil.
3. Pola mingguan tak berbatas yang dapat direpresentasikan oleh hari dan menit dalam satu pekan masuk Jadwal Tetap dan berlaku setelah diimpor. Pola dengan `COUNT` atau `UNTIL` diperluas menjadi acara Kalender jika masih dalam batas 512 kemunculan. Kalender menyimpan lokasi dan deskripsi di catatan acara. Jadwal Tetap menyimpan judul dan panjang blok. Pratinjau memisahkan kedua tujuan dan memperlihatkan acara yang dilewati, termasuk aturan pengecualian dan zona yang memerlukan definisi khusus atau punya offset relatif berubah.
4. Identitas impor diturunkan dari pemilik, UID dan kemunculan. Impor ulang melewati baris yang sudah ada dan mempertahankan edit lokal. Komit melalui tabel Room yang sama agar jurnal Cloud Sync meneruskan perubahan sesuai RLS. Skema Room tetap v4.

### Analyzer

Input memiliki `title` dan `course` 1–120 karakter setelah trim, `description` 0–4000 karakter, serta `deadlineAt` RFC 3339 UTC. Batas body 32 KiB. Deadline lampau boleh dianalisis.

```json
{
  "title": "Implementasi sorting",
  "course": "Algoritma",
  "description": "Coding kelompok dan presentasi hasil",
  "deadlineAt": "2026-12-02T10:00:00Z"
}
```

Gemini API Free melalui Google AI Studio menjadi primary. Groq Free menjadi fallback bila dikonfigurasi. Model diatur melalui env sesuai ketersediaan free tier. Gunakan structured output, validasi ulang hasil, kendalikan timeout, dan tampilkan opsi offline jika AI gagal. Batasi data ke input tugas saja. Jangan log isi tugas, bearer, atau kunci. Tugas hanya dikirim saat user meminta analisis AI. [Gemini structured output](https://ai.google.dev/gemini-api/docs/structured-output) · [Groq structured outputs](https://console.groq.com/docs/structured-outputs)

```json
{
  "type": "object",
  "additionalProperties": false,
  "required": ["difficultyScore", "estimatedMinutes"],
  "properties": {
    "difficultyScore": { "type": "integer", "minimum": 1, "maximum": 5 },
    "estimatedMinutes": { "type": "integer", "minimum": 1, "maximum": 10080 },
    "subtasks": { "type": "array", "minItems": 3, "maxItems": 5, "items": { "type": "string", "minLength": 1, "maxLength": 120 } }
  }
}
```

Header `X-Tazer-Analysis-Subtasks: 1` meminta properti tambahan `subtasks` jika model mengembalikan daftar valid. Klien lama tetap mendapat dua field lama. Saran AI/BYOK/offline tampil sebagai langkah yang bisa diterapkan oleh user. Edit manual tetap tersedia. [Aturan analyzer offline dan kalibrasi](SCHEDULER_SPEC.md#analisis-offline)

### Envelope error

`{ "error": { "code": "PRO_REQUIRED", "message": "Analisis melalui server memerlukan PRO." } }`. Android bercabang berdasarkan kode. Respons memakai `Cache-Control: no-store`.

| HTTP | Kode |
| --- | --- |
| `400` / `413` | `VALIDATION_ERROR` / `PAYLOAD_TOO_LARGE` |
| `401` / `403` | `INVALID_AUTH` / `EMAIL_UNVERIFIED` atau `PRO_REQUIRED` |
| `429` | `QUOTA_EXHAUSTED` dengan `Retry-After` |
| `502` | `AI_UNAVAILABLE` |
| `503` | `VERIFICATION_UNAVAILABLE` |
