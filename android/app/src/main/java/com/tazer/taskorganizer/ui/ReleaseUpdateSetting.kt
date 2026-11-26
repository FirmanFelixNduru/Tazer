package com.tazer.taskorganizer.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.text.format.Formatter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.tazer.taskorganizer.BuildConfig
import com.tazer.taskorganizer.domain.NoteBlock
import com.tazer.taskorganizer.domain.NoteSpan
import com.tazer.taskorganizer.domain.ReleaseNotes
import com.tazer.taskorganizer.integration.IntegrationException
import com.tazer.taskorganizer.integration.RELEASES_API
import com.tazer.taskorganizer.integration.ReleaseUpdate
import com.tazer.taskorganizer.integration.call
import com.tazer.taskorganizer.integration.parseReleaseUpdate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun ReleaseUpdateSetting() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var update by remember { mutableStateOf<ReleaseUpdate?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Pembaruan aplikasi", style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
        Text("Versi terpasang ${BuildConfig.VERSION_NAME}", color = MaterialTheme.colorScheme.onSurfaceVariant)
        PrimaryButton(enabled = !busy, onClick = {
            busy = true
            update = null
            message = null
            scope.launch {
                try {
                    val response = withContext(Dispatchers.IO) {
                        call(RELEASES_API, headers = mapOf("Accept" to "application/vnd.github+json", "User-Agent" to "Tazer-Android"))
                    }
                    when (response.status) {
                        200 -> {
                            update = parseReleaseUpdate(response.json(), BuildConfig.VERSION_NAME)
                            if (update == null) message = "Aplikasi sudah versi terbaru."
                        }
                        404 -> message = "Rilis publik belum tersedia."
                        403, 429 -> message = "Batas permintaan GitHub tercapai. Coba lagi nanti."
                        else -> message = "Pembaruan belum bisa diperiksa. Coba lagi."
                    }
                } catch (_: IntegrationException) {
                    message = "Pembaruan belum bisa diperiksa. Periksa koneksi dan coba lagi."
                } finally {
                    busy = false
                }
            }
        }) { Text(if (busy) "Memeriksa…" else "Cek Update") }
        message?.let { Text(it, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
        update?.let { latest ->
            Text("Versi ${latest.version} tersedia", style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
            if (latest.notes.isNotEmpty()) ReleaseNotesView(latest.notes)
            latest.sizeBytes?.let { Text("Ukuran APK ${Formatter.formatFileSize(context, it)}") }
            latest.sha256?.let { Text("SHA-256 $it", style = MaterialTheme.typography.bodySmall) }
            val download = latest.downloadUrl
            if (download == null) {
                Text("APK belum tersedia untuk rilis ini.")
            } else {
                OutlinedButton(onClick = {
                    try {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(download)).addCategory(Intent.CATEGORY_BROWSABLE))
                    } catch (_: ActivityNotFoundException) {
                        message = "Browser tidak tersedia untuk mengunduh APK."
                    }
                }) { Text("Unduh Update") }
            }
        }
    }
}

/** Release notes from GitHub with headings, bullets, and bold text rendered instead of raw Markdown. */
@Composable
internal fun ReleaseNotesView(markdown: String) {
    val blocks = remember(markdown) { ReleaseNotes.parse(markdown) }
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        blocks.forEach { block ->
            when (block) {
                is NoteBlock.Heading -> Text(block.text, style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.semantics { heading() })
                is NoteBlock.Bullet -> Row {
                    Text("•", Modifier.width(18.dp), color = muted, style = MaterialTheme.typography.bodyMedium)
                    Text(styled(block.spans), style = MaterialTheme.typography.bodyMedium)
                }
                is NoteBlock.Paragraph -> Text(styled(block.spans), style = MaterialTheme.typography.bodyMedium, color = muted)
            }
        }
    }
}

private fun styled(spans: List<NoteSpan>) = buildAnnotatedString {
    spans.forEach { span ->
        if (span.bold) withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(span.text) } else append(span.text)
    }
}
