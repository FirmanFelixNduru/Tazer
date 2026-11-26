# Referensi visual · Neumorphic biru-putih

Catatan **Visual & UX Reference Analyst** (`claude-sonnet-5-5`, high) untuk redesain Tazer v1.9.0. Dokumen ini hanya spesifikasi. Kode `android/` dan `backend/` baru diubah setelah owner menyetujui mockup interaktif.

## Aset

| File | Isi | Ukuran | Git |
| --- | --- | --- | --- |
| `login-neumorphic-blue-reference.jpg` | Layar login neumorphic biru-putih dari owner. Sumber tidak diketahui dan tanpa watermark. | 941×1672, 101 KB | Ter-track. Logo Google, Discord, dan Facebook di dalamnya milik pemiliknya masing-masing dan hanya dipakai sebagai referensi tata letak. |
| `app-ui-neumorphic-mobile-reference.jpg` | Tiga layar aplikasi neumorphic dengan aksen teal. | 1600×1165, 167 KB | **Tidak ter-track** (`.gitignore`). Gambar ini pratinjau stok Depositphotos ber-watermark (Image ID 522540032) tanpa lisensi, sehingga tidak boleh disebarkan di repo publik. Salinannya hanya ada di perangkat ini. Bila tim lain membutuhkannya, minta owner mengirim ulang atau membeli lisensinya. |

Warna di bawah diukur dari piksel kedua gambar, lalu disesuaikan agar lulus WCAG 2.x. Rasio kontras dihitung dengan rumus luminansi WCAG.

## 1. Login (mengikuti `login-neumorphic-blue-reference.jpg`)

Yang dipertahankan dari referensi:

- Latar biru-es lembut dengan gradien vertikal tipis. Bentuk dekoratif lingkaran di sudut boleh dipakai, asalkan statis dan tidak menutupi konten.
- Logo di dalam tile *extruded* (timbul) persegi membulat. Tazer memakai ikon `ic_tazer` yang ada, tanpa membuat logo baru.
- Heading tebal "Welcome Back" berpasangan dengan subjudul. Subjudul referensi (#8A92A7, kontras 2,67:1) **tidak lulus**, jadi Tazer memakai `muted` #5B6478 (5,09:1).
- Input email dan kata sandi bergaya *recessed* (cekung) dengan ikon di kiri dan tombol lihat kata sandi di kanan.
- Baris **Remember Me** di kiri dan **Forgot Password?** di kanan.
- Tombol utama penuh lebar dengan gradien biru dan chip panah kanan di ujungnya.
- Pemisah "atau lanjutkan dengan" lalu tile sosial *extruded*. Tazer hanya punya **Google 1-klik**, sehingga Discord dan Facebook dihapus.

Spesifikasi tombol dari owner:

| Elemen | Spesifikasi | Kontras |
| --- | --- | --- |
| **Sign In** (utama) | Gradien horizontal `#2B7FFF → #254EDB`, label putih semibold di tengah, chip panah kanan putih 90% di ujung kanan, radius penuh, tinggi 56 dp, bayangan biru lembut di bawah (`#2B7FFF` 35%, y 8 dp, blur 20 dp). | Putih di #2B7FFF hanya **3,76:1**, sedangkan di bagian tengah (#2F6BF0) 4,69:1 dan di ujung #254EDB 6,58:1. Label wajib di tengah atau kanan gradien. Jangan geser label ke kiri atau memakai teks kecil. |
| **Sign Up** (sekunder) | Tepat di bawah Sign In, lebar dan tinggi sama. Fill putih atau sama dengan latar halaman, teks dan ikon **#254EDB** (warna fill Sign In), outline 1 dp `#254EDB` 40%, plus bayangan *extruded* ringan. | #254EDB di putih 6,58:1, di latar #E8EEF9 5,65:1. |
| Input *recessed* | Fill `#DCE4F3`, bayangan dalam gelap kanan-bawah dan terang kiri-atas, outline 1 dp `#76839F` agar batas input lulus 3:1 (WCAG 1.4.11), dan ring fokus 2 dp `#254EDB`. | Outline 3,27:1, ring fokus 5,15:1, placeholder `muted` 4,64:1. |
| Remember Me | Checkbox membulat biru `#254EDB` dengan centang putih. | — |
| Forgot Password? | Tautan teks `#254EDB` semibold dengan target sentuh minimal 48 dp. | 5,65:1 |
| Google | Tile *extruded* 64 dp dengan logo Google resmi berwarna. Label aksesibilitas "Masuk dengan Google". | — |

Keputusan untuk owner:

1. **Bahasa label.** Owner menulis "Sign In" dan "Sign Up", sedangkan seluruh aplikasi saat ini berbahasa Indonesia ("Masuk", "Daftar", "Lupa kata sandi?"). Rekomendasi: tetap Indonesia demi konsistensi, kecuali owner memang ingin label Inggris.
2. **Remember Me.** Tazer sekarang selalu menyimpan sesi terenkripsi. Bila dipertahankan, kotak ini mengatur apakah sesi disimpan setelah aplikasi ditutup (Core Dev, `SessionVault`). Opsi lainnya adalah membuang kotak ini demi `ponytail`.
3. Mode demo dan "Uji backend lokal" tetap ada di build debug saja, sebagai tombol teks kecil di bawah area sosial.

## 2. Seluruh aplikasi (mengikuti `app-ui-neumorphic-mobile-reference.jpg`, warna diganti biru-putih)

Pola yang diambil:

- **Kartu soft extruded** dengan dua bayangan: terang di kiri-atas dan biru-dingin di kanan-bawah, radius 20–24 dp, tanpa garis tepi tebal.
- **Segmented tabs**: track cekung dan segmen aktif timbul berwarna aksen. Ini dipakai untuk tab Belum Selesai/Selesai dan pilihan tema.
- **Grid kalender embossed**: hari terpilih memakai tile aksen timbul, rentang memakai pita aksen 30% yang cekung, dan tombol bulan sebelumnya/berikutnya berupa bulatan timbul.
- **Progress dan slider cekung**: track cekung dengan isi gradien biru dan knob putih timbul. Ini untuk progres subtask, akurasi estimasi, dan sisa kuota.
- **Soft toggle**: track cekung, aktif berwarna aksen, dan knob putih timbul. Ini menggantikan Switch Material pada Briefing, Seharian, dan Jam istirahat.
- **Floating bottom bar**: bar putih melayang dengan radius besar, item aktif berupa bulatan timbul berwarna aksen. Lima tab Tazer dipertahankan.
- **Tombol aksi bulat timbul** untuk edit, selesai, bagikan, hapus, dan timer.

Yang **tidak** diambil: aksen teal (diganti biru login), kartu "CARD" bergaya kartu kredit, kurva grafik dekoratif, dan teks lorem.

### Token Light (latar putih kebiruan)

| Token | Nilai | Catatan |
| --- | --- | --- |
| `bg` | `#E8EEF9` | Latar halaman, terukur #E5EDFA–#CBD9FE di referensi |
| `surface` | `#EEF3FC` | Kartu timbul |
| `recessed` | `#DCE4F3` | Input, track, segmen tidak aktif |
| `ink` | `#141A2E` | Teks utama, 14,81:1 |
| `muted` | `#5B6478` | Teks sekunder, 5,09:1 |
| `primary` | `#254EDB` | Aksen dan teks aksen, 5,65:1 |
| `primaryGradient` | `#2B7FFF → #254EDB` | Tombol utama, slider, segmen aktif |
| `outline` | `#76839F` | Batas komponen interaktif, 3,27:1 |
| `shadowLight` | `#FFFFFF` 90%, offset −6/−6 dp, blur 14 dp | Bayangan kiri-atas |
| `shadowDark` | `#A9B8D6` 70%, offset 6/6 dp, blur 16 dp | Bayangan kanan-bawah biru-dingin |
| `danger` | `#B3261E` | Deadline dan hapus, dipertahankan |

### Token Dark (hitam/charcoal)

| Token | Nilai | Catatan |
| --- | --- | --- |
| `bg` | `#16181D` | Charcoal agar bayangan timbul tetap terlihat. Hitam murni #000 dihindari karena bayangan gelap hilang. |
| `surface` | `#1C1F26` | Kartu timbul |
| `recessed` | `#121418` | Input dan track |
| `ink` | `#E6EAF2` | 14,73:1 |
| `muted` | `#9AA3B5` | 7,00:1 di bg, 6,50:1 di kartu |
| `primary` | `#7AA2FF` | Teks dan ring aksen, 7,14:1 |
| `primaryGradient` | `#3B6CF0 → #254EDB` | Label putih di #3B6CF0 4,58:1 |
| `outline` | `#5E6880` | 3,19:1 |
| `shadowLight` | `#262A33` 80%, offset −5/−5 dp | Kilau tepi |
| `shadowDark` | `#0B0C0F` 90%, offset 6/6 dp | Bayangan dalam |

Pilihan tema Light, Dark, dan System Default tetap ada. Warna per mata kuliah (v1.7.0) tetap dipakai pada chip dan titik. Palet itu sudah lulus 4,5:1 di Light dan Dark, sehingga cukup diletakkan di atas `surface` baru.

### Tipografi dan hierarki

- Font sistem (Roboto) dipertahankan agar APK tetap ramping dan Rp 0. Heading halaman 28 sp tebal, judul kartu 18 sp semibold, isi 16 sp, label 13 sp medium.
- Subjudul berspasi lebar ("S i g n  i n …") hanya dipakai sekali di login sebagai aksen, dengan warna `muted` yang lulus kontras.
- Angka timer dan statistik memakai tabular numerals (`tnum`) seperti sekarang.

### Aksesibilitas (wajib, `impeccable` craft floor)

1. Neumorphism mengandalkan bayangan berkontras rendah. Setiap komponen interaktif wajib punya batas `outline` minimal 3:1 atau isi aksen, tidak hanya bayangan.
2. Status aktif (tab, toggle, hari kalender) wajib dibedakan oleh warna aksen dan bentuk, tidak hanya oleh timbul atau cekung.
3. Target sentuh minimal 48 dp. Fokus keyboard dan TalkBack memakai ring 2 dp `primary`.
4. Mode Dark diuji terpisah. Bayangan terang dibuat sangat halus agar tidak terlihat seperti garis putih.

## 3. Perubahan navigasi

Tombol `+` beserta dropdown **Tambah Acara** dan **Jadwal Tetap** (v1.8.0) dipindahkan dari halaman **Jadwal** ke halaman **Kalender**, menggantikan tombol "Tambah acara" yang ada di sana sekarang. Halaman Jadwal kembali bersih sebagai linimasa harian. Halaman Acara dan Jadwal Tetap tidak berubah, hanya titik masuknya yang pindah. Tes instrumented `jadwalPlusOffersEventsAndFixedPages…` perlu diperbarui ke tab Kalender.

## 4. Saran per peran

| Peran | Saran konkret |
| --- | --- |
| **Android UI/UX Dev** | Satu file tema `NeumorphicTheme.kt` berisi token di atas. Pakai `Modifier.neumorphic(extruded \| recessed)` yang menggambar dua bayangan dengan `drawBehind` dan `Paint.setShadowLayer` di Compose, tanpa pustaka baru (`ponytail`). Ganti komponen satu per satu: login, kartu tugas, segmented tab, kalender, toggle, dan bottom bar. Verifikasi render Light dan Dark di emulator. |
| **Android Core Dev** | Tidak ada perubahan Room atau Cloud Sync. Keputusan "Remember Me" (lihat §1) menyentuh `SessionVault`. Pemindahan tombol `+` hanya mengubah state navigasi `WorkspaceScreen`. |
| **System & Algorithm Architect** | Tidak ada perubahan kontrak data. Setelah mockup disetujui, perbarui `DATA_AND_API_CONTRACT.md` bagian Navigasi jadwal dan acara (titik masuk Kalender) dan tambahkan token desain sebagai rujukan UI. |
| **QA & Security Reviewer** | Tambah tes kontras token (JVM), uji tangkapan layar Light/Dark untuk login dan kalender, dan pastikan aset stok ber-watermark tidak pernah ter-commit. |

## 5. Gerbang persetujuan

1. Analyst dan UI Dev menyiapkan mockup interaktif login, daftar tugas, kalender dengan tombol `+`, dan pengaturan, masing-masing dalam Light dan Dark.
2. Owner meninjau dan menyetujui atau meminta revisi.
3. Setelah disetujui, implementasi v1.9.0 berjalan dengan gate rilis biasa (tes, lint, APK signed, upgrade dari 1.8.0).
