package com.tazer.taskorganizer.ui

import androidx.compose.material3.LocalContentColor
import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.tazer.taskorganizer.integration.IntegrationError
import java.io.File

/** Friendly copy for scan failures that need no sign-out; null lets the shared handler decide. */
internal fun scanFailureMessage(code: IntegrationError): String? = when (code) {
    IntegrationError.NETWORK_UNAVAILABLE ->
        "Kamu sedang offline. Scan foto butuh internet, jadi isi form secara manual atau coba lagi nanti."
    IntegrationError.QUOTA_EXHAUSTED ->
        "Kuota AI bulan ini sudah habis. Isi form secara manual; kuota pulih di awal bulan."
    IntegrationError.PRO_REQUIRED, IntegrationError.SCAN_REQUIRES_AI, IntegrationError.BYOK_REQUIRED ->
        IntegrationError.SCAN_REQUIRES_AI.message
    IntegrationError.AI_UNAVAILABLE, IntegrationError.INVALID_RESPONSE ->
        "AI belum bisa membaca gambar ini. Coba foto yang lebih terang dan tegak, atau isi form secara manual."
    IntegrationError.IMAGE_UNREADABLE, IntegrationError.PAYLOAD_TOO_LARGE, IntegrationError.BYOK_GEMINI_REQUIRED,
    IntegrationError.BYOK_KEY_MISSING, IntegrationError.VALIDATION_ERROR -> code.message
    else -> null
}

private fun captureUri(context: Context): Uri {
    val directory = File(context.cacheDir, "scan").apply { mkdirs() }
    return FileProvider.getUriForFile(context, "${context.packageName}.scan", File(directory, "capture.jpg"))
}

/** Gallery (Photo Picker, no storage permission) and camera (FileProvider, permission asked on use) launchers. */
internal class ImageSource(val pickGallery: () -> Unit, val takePhoto: () -> Unit)

@Composable
internal fun rememberImageSource(onImage: (Uri) -> Unit, onMessage: (String) -> Unit): ImageSource {
    val context = LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let(onImage)
    }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        if (saved) onImage(captureUri(context))
    }
    fun openCamera() = try {
        camera.launch(captureUri(context))
    } catch (_: ActivityNotFoundException) {
        onMessage("Aplikasi kamera tidak tersedia. Pilih gambar dari Galeri / Screenshot.")
    }
    val cameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) openCamera() else onMessage("Izin kamera ditolak. Kamu tetap bisa memilih gambar dari Galeri / Screenshot.")
    }
    return ImageSource(
        pickGallery = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
        takePhoto = {
            val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
            if (granted) openCamera() else cameraPermission.launch(Manifest.permission.CAMERA)
        },
    )
}

@Composable
internal fun ScanImageButton(
    available: Boolean,
    enabled: Boolean,
    scanning: Boolean,
    onImage: (Uri) -> Unit,
    onMessage: (String) -> Unit,
) {
    var choosing by rememberSaveable { mutableStateOf(false) }
    val source = rememberImageSource(onImage, onMessage)

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box {
            PrimaryButton(
                onClick = { if (available) choosing = true else onMessage(IntegrationError.SCAN_REQUIRES_AI.message) },
                enabled = enabled && !scanning,
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            ) {
                if (scanning) {
                    CircularProgressIndicator(Modifier.size(18.dp), color = LocalContentColor.current, strokeWidth = 2.dp)
                    Text("Membaca gambar…", Modifier.padding(start = 12.dp))
                } else {
                    TazerIcon(TazerGlyph.Camera)
                    Text("Scan Foto / Screenshot", Modifier.padding(start = 12.dp), style = MaterialTheme.typography.titleSmall)
                }
            }
            DropdownMenu(expanded = choosing, onDismissRequest = { choosing = false }) {
                DropdownMenuItem(
                    text = { Text("Galeri / Screenshot") },
                    leadingIcon = { TazerIcon(TazerGlyph.Image) },
                    onClick = {
                        choosing = false
                        source.pickGallery()
                    },
                )
                DropdownMenuItem(
                    text = { Text("Ambil Foto") },
                    leadingIcon = { TazerIcon(TazerGlyph.Camera) },
                    onClick = {
                        choosing = false
                        source.takePhoto()
                    },
                )
            }
        }
        Text(
            if (available) "AI membaca tugas dari foto atau screenshot lalu mengisi form. Periksa hasilnya sebelum menyimpan."
            else "Tersedia untuk akun PRO atau BYOK Gemini. Gambar hanya diproses saat kamu memindainya.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Review notes shown after a scan so guessed or missing fields are checked before saving. */
@Composable
internal fun ScanReview(notes: List<String>) {
    val colors = MaterialTheme.colorScheme
    val attention = notes.isNotEmpty()
    Surface(
        color = if (attention) colors.tertiaryContainer else colors.secondaryContainer,
        contentColor = if (attention) colors.onTertiaryContainer else colors.onSecondaryContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TazerIcon(TazerGlyph.Check)
                Text("Form terisi dari gambar", Modifier.padding(start = 8.dp).semantics { heading() },
                    style = MaterialTheme.typography.titleSmall)
            }
            notes.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium) }
            Text("Periksa setiap isian, lalu ketuk Simpan tugas.", style = MaterialTheme.typography.bodyMedium)
        }
    }
}
