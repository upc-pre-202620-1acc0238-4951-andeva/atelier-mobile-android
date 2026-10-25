package com.andeva.atelier.mobile.features.auth.presentation.verifyemail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.andeva.atelier.mobile.R
import com.andeva.atelier.mobile.core.designsystem.component.AtelierBanner
import com.andeva.atelier.mobile.core.designsystem.component.AtelierButton
import com.andeva.atelier.mobile.core.designsystem.component.AtelierButtonStyle
import com.andeva.atelier.mobile.core.designsystem.component.AtelierOtpField
import com.andeva.atelier.mobile.core.designsystem.component.AtelierTopBar
import com.andeva.atelier.mobile.core.designsystem.component.BannerKind
import com.andeva.atelier.mobile.core.designsystem.component.rise
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierTheme
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierWorkshopTheme
import com.andeva.atelier.mobile.core.designsystem.theme.JetBrainsMono
import java.util.Locale

/** A4 Verificar correo · código de 6 dígitos. */
@Composable
fun VerifyEmailScreen(
    onBack: () -> Unit,
    onVerified: (email: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: VerifyEmailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.verified) {
        if (state.verified) onVerified(state.email)
    }

    VerifyEmailContent(
        state = state,
        onBack = onBack,
        onCodeChange = viewModel::onCodeChange,
        onVerify = viewModel::onVerify,
        onResend = viewModel::onResend,
        modifier = modifier
    )
}

@Composable
internal fun VerifyEmailContent(
    state: VerifyEmailUiState,
    onBack: () -> Unit,
    onCodeChange: (String) -> Unit,
    onVerify: () -> Unit,
    onResend: () -> Unit,
    modifier: Modifier = Modifier
) {
    val atelier = AtelierTheme.colors
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
    ) {
        AtelierTopBar(title = stringResource(R.string.auth_verify_title), onBack = onBack)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(modifier = Modifier.rise(0), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.auth_otp_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                val sentTo = stringResource(R.string.auth_otp_sent_to, state.maskedEmail)
                val onBackground = MaterialTheme.colorScheme.onBackground
                Text(
                    text = buildAnnotatedString {
                        append(sentTo)
                        val start = sentTo.indexOf(state.maskedEmail)
                        if (start >= 0) {
                            addStyle(
                                SpanStyle(fontWeight = FontWeight.Bold, color = onBackground),
                                start,
                                start + state.maskedEmail.length
                            )
                        }
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = atelier.inkSecondary
                )
            }

            state.info?.let {
                AtelierBanner(message = it.asString(), icon = Icons.Filled.Info, kind = BannerKind.Info)
            }

            AtelierOtpField(
                value = state.code,
                onValueChange = onCodeChange,
                contentDescription = stringResource(R.string.auth_otp_label, 6),
                modifier = Modifier.rise(1),
                isError = state.error != null,
                enabled = !state.isVerifying,
                onDone = onVerify
            )

            state.error?.let { error ->
                Row(
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive },
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Error, contentDescription = null, modifier = Modifier.size(18.dp), tint = atelier.bad)
                    Text(text = error.asString(), style = MaterialTheme.typography.bodyMedium, color = atelier.bad)
                }
            }

            Row(
                modifier = Modifier
                    .rise(2)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ExpiryLabel(state.expiresSecondsLeft)
                AtelierButton(
                    text = if (state.canResend || state.isResending) {
                        stringResource(R.string.auth_resend)
                    } else {
                        stringResource(R.string.auth_resend_in, formatClock(state.resendSecondsLeft, pad = false))
                    },
                    onClick = onResend,
                    style = AtelierButtonStyle.Text,
                    enabled = state.canResend,
                    loading = state.isResending
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp)
        ) {
            AtelierButton(
                text = stringResource(R.string.auth_verify_action),
                onClick = onVerify,
                modifier = Modifier.fillMaxWidth(),
                enabled = state.canSubmit,
                loading = state.isVerifying
            )
        }
    }
}

/** "Vence en 09:42" en mono; solo aparece cuando la app envió el código y sabe cuándo vence. */
@Composable
private fun ExpiryLabel(secondsLeft: Int?) {
    val atelier = AtelierTheme.colors
    if (secondsLeft == null) {
        Box(Modifier.size(0.dp))
        return
    }
    if (secondsLeft == 0) {
        Text(
            text = stringResource(R.string.auth_otp_expired),
            style = MaterialTheme.typography.bodyMedium,
            color = atelier.bad
        )
        return
    }
    val clock = formatClock(secondsLeft, pad = true)
    val text = stringResource(R.string.auth_otp_expires_in, clock)
    val onBackground = MaterialTheme.colorScheme.onBackground
    Text(
        text = buildAnnotatedString {
            append(text)
            val start = text.indexOf(clock)
            if (start >= 0) {
                addStyle(
                    SpanStyle(fontFamily = JetBrainsMono, fontWeight = FontWeight.Medium, color = onBackground),
                    start,
                    start + clock.length
                )
            }
        },
        style = MaterialTheme.typography.bodyMedium,
        color = atelier.muted
    )
}

private fun formatClock(totalSeconds: Int, pad: Boolean): String =
    String.format(Locale.ROOT, if (pad) "%02d:%02d" else "%d:%02d", totalSeconds / 60, totalSeconds % 60)

@Preview(name = "Verificar · código", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
private fun VerifyEmailPreview() = AtelierWorkshopTheme(darkTheme = true) {
    VerifyEmailContent(
        state = VerifyEmailUiState(email = "andres@taller.pe", code = "482", resendSecondsLeft = 42, expiresSecondsLeft = 582),
        onBack = {}, onCodeChange = {}, onVerify = {}, onResend = {}
    )
}

@Preview(name = "Verificar · error · claro", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
private fun VerifyEmailErrorPreview() = AtelierWorkshopTheme(darkTheme = false) {
    VerifyEmailContent(
        state = VerifyEmailUiState(
            email = "andres@taller.pe", code = "482170",
            error = com.andeva.atelier.mobile.core.designsystem.text.UiText.Dynamic("El código es incorrecto o venció."),
            resendSecondsLeft = 0
        ),
        onBack = {}, onCodeChange = {}, onVerify = {}, onResend = {}
    )
}
