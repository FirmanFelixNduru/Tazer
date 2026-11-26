package com.tazer.taskorganizer.ui

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.tazer.taskorganizer.integration.ByokProvider
import com.tazer.taskorganizer.integration.SubscriptionTier
import kotlinx.coroutines.CancellationException

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun LoginScreen(model: TazerViewModel, state: TazerState, launch: (Intent) -> Unit, modifier: Modifier = Modifier) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val canSubmit = !state.busy && email.isNotBlank() && password.isNotEmpty()
    Box(modifier.fillMaxSize().imePadding(), contentAlignment = Alignment.Center) {
        Column(
            Modifier.widthIn(max = 440.dp).fillMaxWidth().verticalScroll(rememberScrollState()).padding(28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer) {
                    Box(Modifier.padding(9.dp)) { TazerIcon(TazerGlyph.Tasks) }
                }
                Text("Tazer", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            }
            Text(
                if (state.recovery) "Atur kata sandi baru" else "Masuk",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.semantics { heading() },
            )
            if (state.recovery) {
                SecretField(password, { password = it }, "Kata sandi baru", !state.busy, Modifier.fillMaxWidth())
                Button(onClick = { model.updatePassword(password) }, enabled = !state.busy && password.length >= 6,
                    modifier = Modifier.fillMaxWidth()) { Text("Simpan kata sandi") }
                OutlinedButton(onClick = model::signOut, enabled = !state.busy) { Text("Batal") }
                return@Column
            }
            if (model.authAvailable) {
                OutlinedTextField(email, { email = it }, label = { Text("Email") }, singleLine = true, enabled = !state.busy,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), modifier = Modifier.fillMaxWidth())
                SecretField(password, { password = it }, "Kata sandi", !state.busy, Modifier.fillMaxWidth())
                Button(onClick = { model.signIn(email, password) }, enabled = canSubmit, modifier = Modifier.fillMaxWidth()) {
                    Text("Masuk dengan email")
                }
                OutlinedButton(onClick = { model.startGoogle(launch) }, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) {
                    Text("Masuk dengan Google")
                }
                if (state.googlePending) {
                    Text("Selesaikan login di browser atau batalkan untuk mencoba lagi.")
                    OutlinedButton(onClick = model::cancelGoogle, enabled = !state.busy) {
                        Text("Batalkan login Google")
                    }
                }
                FlowRow {
                    OutlinedButton(onClick = { model.signUp(email, password) }, enabled = canSubmit) { Text("Daftar") }
                    OutlinedButton(onClick = { model.resetPassword(email) }, enabled = !state.busy && email.isNotBlank()) {
                        Text("Lupa kata sandi?")
                    }
                }
            } else if (!model.demoAvailable) {
                Text("Hubungkan pengaturan akun untuk menggunakan build ini.")
            }
            if (model.demoAvailable) {
                if (model.authAvailable) HorizontalDivider()
                val demoClick = { model.openDemo(); Unit }
                if (model.authAvailable) {
                    OutlinedButton(demoClick, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) { Text("Masuk sebagai demo") }
                } else {
                    Button(demoClick, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) { Text("Masuk sebagai demo") }
                }
            }
            if (model.mockAvailable) {
                Text("Uji backend lokal", style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    SubscriptionTier.entries.forEach { tier ->
                        OutlinedButton(onClick = { model.signInMock(tier) }, enabled = !state.busy) { Text(tier.name) }
                    }
                }
            }
        }
    }
}

@Composable
internal fun AccountSettings(state: TazerState, model: TazerViewModel, modifier: Modifier = Modifier) {
    val subscription = state.subscription
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        HorizontalDivider()
        Text("Akun", style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
        when {
            state.demo -> Text("Demo lokal · Analisis offline")
            subscription == null -> Text("Tier sedang diperiksa.")
            subscription.tier == SubscriptionTier.PRO -> Text("PRO · " + subscription.remaining + "/" + subscription.monthlyLimit + " analisis tersisa")
            else -> Text(subscription.tier.name)
        }
        if (subscription != null) {
            OutlinedButton(onClick = model::refreshTier, enabled = !state.busy) { Text("Perbarui tier") }
        }
        if (subscription?.tier == SubscriptionTier.BYOK) {
            ByokForm(state, model)
        }
        OutlinedButton(onClick = model::signOut, enabled = !state.busy) { Text("Keluar") }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ByokForm(state: TazerState, model: TazerViewModel) {
    var providerName by rememberSaveable(state.uid) { mutableStateOf(ByokProvider.entries.first().name) }
    var modelName by rememberSaveable(state.uid) { mutableStateOf("") }
    var key by remember(state.uid) { mutableStateOf("") }
    var hasKey by remember(state.uid) { mutableStateOf(false) }
    LaunchedEffect(state.uid) {
        try {
            model.readByokSettings()?.let { saved ->
                providerName = saved.provider.name
                modelName = saved.model
                hasKey = true
            }
        } catch (failure: CancellationException) {
            throw failure
        } catch (_: Exception) {
            model.showMessage("Pengaturan kunci BYOK belum dapat dibaca.")
        }
    }
    Text("Kunci AI pribadi", style = MaterialTheme.typography.titleMedium)
    Text("Gunakan kunci dari paket gratis. Saat memilih BYOK, provider menerima isi tugas sesuai kebijakan datanya.")
    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        ByokProvider.entries.forEach { candidate ->
            if (providerName == candidate.name) {
                Button(onClick = {}, enabled = false) { Text(candidate.name + " dipilih") }
            } else {
                OutlinedButton(onClick = { providerName = candidate.name }, enabled = !state.busy) { Text(candidate.name) }
            }
        }
    }
    OutlinedTextField(modelName, { modelName = it }, label = { Text("Model free tier") }, singleLine = true,
        enabled = !state.busy, modifier = Modifier.fillMaxWidth())
    SecretField(key, { key = it }, "Kunci API", !state.busy, Modifier.fillMaxWidth())
    if (hasKey) Text("Kunci tersimpan di perangkat.")
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = {
            model.saveByok(ByokProvider.valueOf(providerName), modelName, key) { key = ""; hasKey = true }
        }, enabled = !state.busy && modelName.isNotBlank() && key.isNotBlank()) { Text("Simpan kunci") }
        if (hasKey) OutlinedButton(onClick = {
            model.deleteByok { key = ""; hasKey = false }
        }, enabled = !state.busy) { Text("Hapus kunci") }
    }
}

@Composable
private fun SecretField(value: String, onChange: (String) -> Unit, label: String, enabled: Boolean, modifier: Modifier) {
    OutlinedTextField(
        value, onChange, label = { Text(label) }, singleLine = true, enabled = enabled, modifier = modifier,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
    )
}
