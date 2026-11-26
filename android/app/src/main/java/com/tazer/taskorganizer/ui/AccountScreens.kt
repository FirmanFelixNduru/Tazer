package com.tazer.taskorganizer.ui

import android.content.Intent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
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
internal fun LoginScreen(
    model: TazerViewModel, state: TazerState, launch: (Intent) -> Unit, dark: Boolean, toggleTheme: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var rememberMe by rememberSaveable { mutableStateOf(model.rememberSession) }
    val neu = neu()
    val canSubmit = !state.busy && email.isNotBlank() && password.isNotEmpty()
    Box(modifier.fillMaxSize().imePadding()) {
        AmbientOrbs(neu)
        Column(
            Modifier.align(Alignment.TopCenter).widthIn(max = 440.dp).fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                NeuIconButton(if (dark) "Ganti ke tema terang" else "Ganti ke tema gelap", toggleTheme) {
                    TazerIcon(if (dark) TazerGlyph.Sun else TazerGlyph.Moon)
                }
            }
            Spacer(Modifier.height(8.dp))
            LogoBadge(neu)
            Spacer(Modifier.height(22.dp))
            Text(
                if (state.recovery) "Set New Password" else "Welcome Back",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = neu.ink, modifier = Modifier.semantics { heading() },
            )
            Spacer(Modifier.height(6.dp))
            Text(
                if (state.recovery) "Choose a new password for your account" else "Sign in to continue",
                style = MaterialTheme.typography.bodyLarge, color = neu.muted, textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(28.dp))
            if (state.recovery) {
                NeuField(password, { password = it }, "New Password", TazerGlyph.Lock, !state.busy, secret = true)
                Spacer(Modifier.height(20.dp))
                SignInButton("Save Password", !state.busy && password.length >= 6) { model.updatePassword(password) }
                Spacer(Modifier.height(14.dp))
                SignUpButton("Cancel", !state.busy, model::signOut)
                return@Column
            }
            if (model.authAvailable) {
                NeuField(email, { email = it }, "Email", TazerGlyph.Mail, !state.busy, keyboard = KeyboardType.Email)
                Spacer(Modifier.height(16.dp))
                NeuField(password, { password = it }, "Password", TazerGlyph.Lock, !state.busy, secret = true)
                Spacer(Modifier.height(6.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Row(
                        Modifier.weight(1f).heightIn(min = 48.dp).toggleable(rememberMe, enabled = !state.busy, role = Role.Checkbox) {
                            rememberMe = it
                            model.setRememberSession(it)
                        },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(rememberMe, onCheckedChange = null, colors = CheckboxDefaults.colors(
                            checkedColor = neu.accent, checkmarkColor = neu.onAccent, uncheckedColor = neu.outline))
                        Spacer(Modifier.width(10.dp))
                        Text("Remember Me", style = MaterialTheme.typography.bodyMedium, color = neu.ink)
                    }
                    TextButton(onClick = {
                        if (email.isBlank()) model.showMessage("Isi email dulu untuk menerima tautan reset kata sandi.")
                        else model.resetPassword(email)
                    }, enabled = !state.busy) {
                        Text("Forgot Password?", color = neu.accent, fontWeight = FontWeight.SemiBold)
                    }
                }
                Spacer(Modifier.height(18.dp))
                SignInButton("Sign In", canSubmit) { model.signIn(email, password) }
                Spacer(Modifier.height(14.dp))
                SignUpButton("Sign Up", !state.busy) {
                    if (email.isBlank() || password.length < 6) {
                        model.showMessage("Isi email dan kata sandi minimal 6 karakter untuk mendaftar.")
                    } else {
                        model.signUp(email, password)
                    }
                }
                Spacer(Modifier.height(26.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    HorizontalDivider(Modifier.weight(1f), color = neu.outline.copy(alpha = .5f))
                    Text(
                        "OR CONTINUE WITH", Modifier.padding(horizontal = 12.dp), color = neu.muted,
                        style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.2.sp),
                    )
                    HorizontalDivider(Modifier.weight(1f), color = neu.outline.copy(alpha = .5f))
                }
                Spacer(Modifier.height(18.dp))
                NeuCard(
                    Modifier.fillMaxWidth().heightIn(min = 56.dp), radius = 28.dp, border = neu.outline,
                    onClick = { if (!state.busy) model.startGoogle(launch) },
                ) {
                    Row(
                        Modifier.padding(horizontal = 20.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        TazerIcon(TazerGlyph.Google)
                        Spacer(Modifier.width(12.dp))
                        Text("Continue with Google", color = neu.ink, style = MaterialTheme.typography.titleSmall)
                    }
                }
                if (state.googlePending) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Selesaikan login di browser atau batalkan untuk mencoba lagi.", color = neu.muted,
                        textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium,
                    )
                    TextButton(onClick = model::cancelGoogle, enabled = !state.busy) {
                        Text("Batalkan login Google", color = neu.accent)
                    }
                }
            } else if (!model.demoAvailable) {
                Text("Hubungkan pengaturan akun untuk menggunakan build ini.", color = neu.ink)
            }
            if (model.demoAvailable || model.mockAvailable) {
                // Debug-only shortcuts stay as quiet text actions below the main hierarchy.
                Spacer(Modifier.height(20.dp))
                FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    if (model.demoAvailable) TextButton(onClick = model::openDemo, enabled = !state.busy) {
                        Text("Masuk sebagai demo", color = neu.accent)
                    }
                    if (model.mockAvailable) SubscriptionTier.entries.forEach { tier ->
                        TextButton(onClick = { model.signInMock(tier) }, enabled = !state.busy) {
                            Text(tier.name, color = neu.muted)
                        }
                    }
                }
            }
        }
    }
}

/** Static decorative glow; it sits behind the content and never takes focus. */
@Composable
private fun AmbientOrbs(neu: Neu) {
    val dark = LocalTazerDark.current
    Canvas(Modifier.fillMaxSize()) {
        fun orb(color: Color, x: Float, y: Float, radius: Float) {
            val center = Offset(size.width * x, size.height * y)
            val r = size.width * radius
            drawCircle(Brush.radialGradient(listOf(color, Color.Transparent), center = center, radius = r), r, center)
        }
        orb(if (dark) Color(0x402254E6) else Color(0x4D2B82FF), .98f, .06f, .62f)
        orb(if (dark) Color(0x2E3B38D6) else Color(0x333B38D6), .02f, .66f, .58f)
        orb(neu.highlight.copy(alpha = if (dark) .55f else .8f), .18f, .14f, .42f)
    }
}

@Composable
private fun LogoBadge(neu: Neu) {
    val shape = RoundedCornerShape(28.dp)
    Box(
        Modifier.size(92.dp).neumorphicRaised(neu, 28.dp, 7.dp)
            .background(neu.surface, shape).border(1.dp, neu.outline.copy(alpha = .3f), shape)
            .semantics { contentDescription = "Logo Tazer" },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(46.dp)) {
            val w = size.width
            val h = size.height
            drawPath(
                Path().apply {
                    moveTo(w * .62f, h * .02f); lineTo(w * .16f, h * .56f); lineTo(w * .46f, h * .56f)
                    lineTo(w * .36f, h * .98f); lineTo(w * .84f, h * .40f); lineTo(w * .54f, h * .40f); close()
                },
                Brush.linearGradient(
                    listOf(Color(0xFF2B82FF), Color(0xFF2254E6), Color(0xFF3B38D6)),
                    start = Offset(w * .5f, 0f), end = Offset(w * .5f, h),
                ),
            )
        }
    }
}

@Composable
private fun NeuField(
    value: String, onChange: (String) -> Unit, label: String, glyph: TazerGlyph, enabled: Boolean,
    secret: Boolean = false, keyboard: KeyboardType = KeyboardType.Text,
) {
    val neu = neu()
    var shown by rememberSaveable { mutableStateOf(false) }
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(28.dp)
    TextField(
        value, onChange, label = { Text(label) }, singleLine = true, enabled = enabled, shape = shape,
        leadingIcon = { TazerIcon(glyph) },
        trailingIcon = if (!secret) null else {
            {
                IconButton(onClick = { shown = !shown }) {
                    TazerIcon(
                        if (shown) TazerGlyph.EyeOff else TazerGlyph.Eye,
                        if (shown) "Sembunyikan kata sandi" else "Tampilkan kata sandi",
                    )
                }
            }
        },
        visualTransformation = if (secret && !shown) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = if (secret) KeyboardType.Password else keyboard),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent, focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent, disabledIndicatorColor = Color.Transparent,
            focusedLeadingIconColor = neu.accent, unfocusedLeadingIconColor = neu.muted,
            focusedTrailingIconColor = neu.muted, unfocusedTrailingIconColor = neu.muted,
            focusedLabelColor = neu.accent, unfocusedLabelColor = neu.muted, cursorColor = neu.accent,
            focusedTextColor = neu.ink, unfocusedTextColor = neu.ink,
        ),
        modifier = Modifier.fillMaxWidth().neumorphicInset(neu, 28.dp)
            .border(if (focused) 2.dp else 1.dp, if (focused) neu.accent else neu.outline, shape)
            .onFocusChanged { focused = it.isFocused },
    )
}

/** Gradient pill; the centred white label sits on the #2254E6 stop (6.0:1). */
@Composable
private fun SignInButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    val neu = neu()
    val shape = RoundedCornerShape(30.dp)
    Box(
        Modifier.fillMaxWidth().height(58.dp)
            .then(if (enabled) Modifier.neumorphicRaised(neu, 30.dp, 5.dp, Color(0xFF2254E6)) else Modifier)
            .alpha(if (enabled) 1f else .5f).clip(shape).background(Neu.SignInGradient, shape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = Color.White, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
        Box(
            Modifier.align(Alignment.CenterEnd).padding(end = 7.dp).size(44.dp).clip(CircleShape)
                .background(Color.White.copy(alpha = .92f)),
            contentAlignment = Alignment.Center,
        ) {
            CompositionLocalProvider(LocalContentColor provides Color(0xFF2E45DE)) { TazerIcon(TazerGlyph.ArrowRight) }
        }
    }
}

/** White raised pill (canvas-matched in Dark) with the Sign In blue for text and border. */
@Composable
private fun SignUpButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    val neu = neu()
    val dark = LocalTazerDark.current
    val fill = if (dark) neu.surface else Color.White
    val ink = if (dark) neu.accent else Color(0xFF2254E6)
    NeuCard(
        Modifier.fillMaxWidth().height(58.dp).alpha(if (enabled) 1f else .5f), radius = 30.dp, fill = fill,
        border = ink.copy(alpha = .6f), onClick = { if (enabled) onClick() },
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(label, color = ink, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
        }
    }
}

@Composable
private fun NeuIconButton(description: String, onClick: () -> Unit, icon: @Composable () -> Unit) {
    val neu = neu()
    Box(
        Modifier.size(48.dp).neumorphicRaised(neu, 24.dp).background(neu.surface, CircleShape)
            .border(1.dp, neu.outline, CircleShape).clip(CircleShape)
            .clickable(role = Role.Button, onClick = onClick).semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) { CompositionLocalProvider(LocalContentColor provides neu.ink) { icon() } }
}

@Composable
internal fun AccountSettings(state: TazerState, model: TazerViewModel, modifier: Modifier = Modifier) {
    val subscription = state.subscription
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
