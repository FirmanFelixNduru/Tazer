# Spesifikasi UI neumorphic biru-putih (v1.9.0)

Dokumen ini menjadi acuan visual Tazer sejak v1.9.0. Owner menyetujui mockup interaktif lima layar (Light dan Dark) pada 27 November 2026. Analisis piksel dan saran awal ada di [references/README.md](references/README.md). Bila angkanya berbeda, nilai di dokumen ini yang berlaku karena mengikuti token final dari owner.

Implementasinya ada di `android/app/src/main/java/com/tazer/taskorganizer/ui/Neumorphic.kt`, tanpa pustaka baru.

## Token

| Token | Light | Dark | Pemakaian |
| --- | --- | --- | --- |
| `canvas` | `#EDF2FB` | `#141923` | Latar semua layar, `background` dan `surface` Material, dan warna fade spinner tanggal |
| `canvasTop → canvasBottom` | `#F3F7FF → #E7EEFB` | `#19202C → #10141C` | Gradien vertikal di layar login |
| `surface` | `#F4F7FD` | `#1A2130` | Kartu timbul, bottom bar, dan tombol ikon |
| `recessed` | `#E1E8F5` | `#0E1219` | Input, track segmented, dan kolom cari |
| `highlight` | `#FFFFFF` 95% | `#222B3D` | Bayangan terang di kiri atas |
| `shade` | `#A9B9D8` 72% | `#090C12` | Bayangan gelap di kanan bawah |
| `ink` | `#141A2E` | `#E6EAF2` | Teks utama |
| `muted` | `#535E77` | `#9AA3B5` | Teks sekunder dan placeholder |
| `outline` | `#76839F` | `#64708A` | Batas komponen interaktif |
| `outlineVariant` | `#7A86A1` | `#616D88` | Batas tombol outlined Material dan pemisah |
| `accent` | `#2254E6` | `#4F94FF` | Royal blue: tab aktif, tombol `+`, FAB, dan teks tautan |
| `onAccent` | `#FFFFFF` | `#08142E` | Label di atas `accent` (checkbox Remember Me) |
| `fill` (v1.9.1) | `#2254E6` | `#2D66EB` | Fill biru royal untuk aksi utama, segmen aktif, tab aktif, gerigi, Back, `+`, dan FAB, selalu dengan teks/ikon putih (6,04:1 dan 4,99:1) |
| Gradien Sign In | `#2B82FF → #2254E6 → #3B38D6` | sama | Tombol utama login, dengan label putih di tengah |

Warna mata kuliah (10 slot, v1.7.0) tidak berubah. `TaskSearchAndColorsTest` membuktikan titik warna tetap 3:1 dan label chip 4,5:1 di atas `canvas` dan `surface` kedua tema.

## Kontras WCAG

`NeumorphicTokensTest` (JVM) menegakkan aturan berikut di `canvas`, kedua ujung gradien, dan `surface` untuk kedua tema:

1. `ink`, `muted`, dan `accent` sebagai teks minimal 4,5:1. Hasil terendah: `muted` Light di `canvasBottom` 5,57:1, `accent` Dark di `surface` 5,38:1, dan `muted` di `recessed` 5,27:1.
2. `outline` minimal 3:1 sebagai batas komponen (WCAG 1.4.11). Hasil terendah adalah 3,24:1 di `surface` Dark.
3. `onAccent` di `accent` minimal 4,5:1: 6,04:1 (Light) dan 6,1:1 (Dark). Label Sign In berada di stop `#2254E6` (6,04:1). Stop `#2B82FF` hanya 3,66:1, sehingga tidak boleh dipakai di bawah teks.

Bayangan hanya dekorasi. Setiap kontrol tetap punya garis `outline`, isi `accent`, atau keduanya, dan status aktif dibedakan oleh warna serta bentuk.

## Modifier

1. `Modifier.neumorphicRaised(neu, radius, depth = 5.dp, fill)` memakai `drawWithCache` dan `onDrawBehind`, lalu menggambar dua rounded rect dengan `Paint.setShadowLayer`: terang ke kiri atas (blur 1,8×depth) dan gelap ke kanan bawah (blur 2,2×depth). Paint dibuat sekali per ukuran, bukan per frame.
2. `Modifier.neumorphicInset(neu, radius, depth = 3.dp, fill)` mengisi bentuk dengan `recessed`, memotong ke bentuk yang sama, lalu menggambar stroke tepi dengan bayangan gelap ke kanan bawah dan terang ke kiri atas. Hasilnya bayangan masuk ke dalam.
3. Bayangan berada di luar batas layout, sehingga kontainer memberi jarak 7–12 dp di sekitar kartu. Sebelum API 28, `setShadowLayer` diabaikan pada canvas akselerasi hardware, sehingga tampilan jatuh ke isi datar plus garis tepi yang tetap lulus kontras.

Komponen bersama: `NeuCard` (kartu timbul radius 20 dp, garis `outline` 35%), `NeuSegmented` (track cekung dengan segmen aktif `accent`, semantik `Role.Tab` dan `selectableGroup`), `NeuIconAction` (tombol ikon bulat 40 dp dengan target 48 dp dan garis `outline` penuh), serta `NeuAccentSquircle` (squircle royal blue).

## Pemetaan layar

| Layar | Perubahan |
| --- | --- |
| Login | Latar gradien dengan tiga orb statis dan toggle tema (bulan/matahari) di kanan atas. Badge logo squircle timbul dengan ikon petir bergradien biru. Judul **Welcome Back** dan subjudul **Sign in to continue**. Input Email dan Password berbentuk pill cekung dengan ikon dan tombol lihat kata sandi. Baris **Remember Me** dan **Forgot Password?**, diikuti **Sign In** (gradien dan chip panah) dan **Sign Up** (pill putih timbul, teks dan garis `#2254E6`). Pemisah **OR CONTINUE WITH** lalu **Continue with Google**. Discord dan Facebook tidak ada. Demo dan uji backend lokal hanya muncul di build debug sebagai tombol teks kecil. |
| Tugas | Segmented **Belum Selesai \| Selesai**, kolom cari berbentuk pill cekung, kartu tugas timbul, tombol edit, selesai, bagikan, dan hapus bergaris `outline`, serta FAB squircle royal blue. |
| Jadwal | Header bulan dan **Hari Ini** bergaris, spinner tanggal dengan sel bergaris `outline` dan fade tepi `canvas`, kartu timeline timbul, dan tombol jam bergaris. Tombol `+` tidak ada lagi di sini. |
| Kalender | Header bulan, **Hari Ini**, dan panah bulat bergaris. Grid sel kotak bergaris `outline`. Squircle `+` (52 dp, deskripsi "Tambah ke jadwal") menggantikan tombol "Tambah acara" lama di samping tanggal terpilih. Dropdown-nya berisi tepat **Tambah Acara** (halaman Acara) dan **Jadwal Tetap** (halaman Jadwal Tetap dengan lima sumber). |
| Acara dan Jadwal Tetap | Kartu formulir dan kartu tersimpan memakai `NeuCard`, sedangkan tombol edit dan hapus memakai `NeuIconAction`. |
| Timer fokus dan Floating Timer Deck | Kartu waktu timbul radius 26 dp (tersier saat dijeda atau lembur). Deck melayang timbul dengan garis `outline`. |
| Statistik | Navigasi minggu memakai `NeuIconAction`. Fokus harian dan ringkasan akurasi berada di kartu timbul. |
| Pengaturan | Setiap grup menjadi `NeuCard` dengan urutan yang sama seperti sebelumnya. Pilihan tema memakai segmented Light, Dark, dan System Default. |
| Bottom bar | Pill timbul melayang dengan item aktif berisi `accent`. |
| Widget | `focus_widget.xml` memakai `canvas`, `ink`, dan `muted` kedua tema. Override warna dinamis API 31+ dihapus, dan hanya radius sudut sistem yang dipakai. |

## Polish v1.9.1

1. **Navigasi.** Bottom bar berisi empat tab: Jadwal, Tugas, Kalender, Statistik. Tugas tetap layar pembuka. Pengaturan dibuka dari tombol gerigi `fill` 48 dp di kanan atas top bar (`contentDescription` "Pengaturan"), dan kembali ke tab asal lewat tombol Back. Bar punya inset horizontal 10 dp agar pill tab tepi tidak menyentuh lengkung bar.
2. **Back.** Tombol teks "Kembali" diganti `NeuFillIconButton` 48 dp: fill biru, panah putih yang otomatis dicerminkan saat RTL, `contentDescription` "Kembali". Ini berlaku di Pengaturan, Acara, Jadwal Tetap, Timer fokus, dan formulir tugas. Panah digambar sebagai glyph `TazerGlyph.Back` tanpa pustaka ikon baru.
3. **Tema.** Segmen menjadi Light | Dark | System. Pill aktif radius 22 dp, konsentris dengan track 26 dp yang berinset 4 dp.
4. **Tugas.** Kolom cari berupa pill cekung 44 dp dengan placeholder `bodyMedium`, sedangkan teks yang diketik tetap `bodyLarge`. Chip kesulitan dan mata kuliah pindah ke panel **Filter** ([referensi §5](references/README.md)). Kartu tugas mendapat inset bawah 16 dp di bawah baris ikon aksi.
5. **Highlight biru.** `PrimaryButton` (fill biru, teks putih, raised) menggantikan semua `Button` Material. Tanggal dan Jam Deadline, Impor Paket Tugas, Scan Foto / Screenshot, Cek Update, dan Sinkronkan sekarang ikut naik menjadi aksi utama.

## Remember Me

Kotak **Remember Me** (default aktif) disimpan di SharedPreferences `tazer_login` dengan kunci `remember_session`. Saat aktif, perilakunya sama seperti v1.8.0: sesi Supabase terenkripsi di `SessionVault`. Saat nonaktif, `AndroidIntegration.persist` menghapus sesi tersimpan, sehingga sesi hanya hidup selama proses berjalan dan cold start berikutnya kembali ke login. Mematikan kotak saat masuk langsung menghapus salinan di disk. Token tidak pernah keluar dari perangkat.
