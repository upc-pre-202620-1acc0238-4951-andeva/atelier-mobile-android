package com.andeva.atelier.mobile.features.auth.presentation.forgot

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.andeva.atelier.mobile.R
import com.andeva.atelier.mobile.core.designsystem.component.AtelierBanner
import com.andeva.atelier.mobile.core.designsystem.component.AtelierButton
import com.andeva.atelier.mobile.core.designsystem.component.AtelierButtonStyle
import com.andeva.atelier.mobile.core.designsystem.component.AtelierTextField
import com.andeva.atelier.mobile.core.designsystem.component.AtelierTopBar
import com.andeva.atelier.mobile.core.designsystem.component.BannerKind
import com.andeva.atelier.mobile.core.designsystem.component.rise
import com.andeva.atelier.mobile.core.designsystem.text.UiText
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierTheme
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierWorkshopTheme

/** A6 Recuperar contraseña · paso 1: pedir el enlace por correo. */
@Composable
fun ForgotPasswordScreen(
    onBack: () -> Unit,
    onHaveToken: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ForgotPasswordViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ForgotPasswordContent(
        state = state,
        onBack = onBack,
        onEmailChange = viewModel::onEmailChange,
        onSubmit = viewModel::onSubmit,
        onHaveToken = onHaveToken,
        onRequestAgain = viewModel::onRequestAgain,
        modifier = modifier
    )
}

@Composable
internal fun ForgotPasswordContent(
    state: ForgotPasswordUiState,
    onBack: () -> Unit,
    onEmailChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onHaveToken: () -> Unit,
    onRequestAgain: () -> Unit,
    modifier: Modifier = Modifier
) {
    val atelier = AtelierTheme.colors
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
    ) {
        AtelierTopBar(title = stringResource(R.string.auth_forgot_title), onBack = onBack)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (state.sent) {
                Text(
                    text = stringResource(R.string.auth_forgot_sent_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.rise(0)
                )
                AtelierBanner(
                    message = stringResource(R.string.auth_forgot_sent),
                    icon = Icons.Filled.CheckCircle,
                    kind = BannerKind.Ok,
                    modifier = Modifier.rise(1)
                )
            } else {
                Text(
                    text = stringResource(R.string.auth_forgot_body),
                    style = MaterialTheme.typography.bodyLarge,
                    color = atelier.inkSecondary,
                    modifier = Modifier.rise(0)
                )
                state.error?.let { error ->
                    AtelierBanner(message = error.asString(), icon = Icons.Filled.Error, kind = BannerKind.Bad)
                }
                AtelierTextField(
                    label = stringResource(R.string.auth_email_label),
                    value = state.email,
                    onValueChange = onEmailChange,
                    modifier = Modifier.rise(1),
                    leadingIcon = Icons.Outlined.Email,
                    enabled = !state.isLoading,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { onSubmit() }),
                    autofill = ContentType.EmailAddress
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (state.sent) {
                AtelierButton(
                    text = stringResource(R.string.auth_have_token),
                    onClick = onHaveToken,
                    modifier = Modifier.fillMaxWidth()
                )
                AtelierButton(
                    text = stringResource(R.string.auth_request_again),
                    onClick = onRequestAgain,
                    modifier = Modifier.fillMaxWidth(),
                    style = AtelierButtonStyle.Text
                )
            } else {
                AtelierButton(
                    text = stringResource(R.string.auth_forgot_action),
                    onClick = onSubmit,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = state.canSubmit,
                    loading = state.isLoading
                )
                AtelierButton(
                    text = stringResource(R.string.auth_have_token),
                    onClick = onHaveToken,
                    modifier = Modifier.fillMaxWidth(),
                    style = AtelierButtonStyle.Text,
                    enabled = !state.isLoading
                )
            }
        }
    }
}

@Preview(name = "Recuperar · correo", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
private fun ForgotPasswordPreview() = AtelierWorkshopTheme(darkTheme = true) {
    ForgotPasswordContent(
        state = ForgotPasswordUiState(email = "andres@taller.pe"),
        onBack = {}, onEmailChange = {}, onSubmit = {}, onHaveToken = {}, onRequestAgain = {}
    )
}

@Preview(name = "Recuperar · error · claro", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
private fun ForgotPasswordErrorPreview() = AtelierWorkshopTheme(darkTheme = false) {
    ForgotPasswordContent(
        state = ForgotPasswordUiState(email = "andres@taller", error = UiText.Dynamic("El correo no es válido.")),
        onBack = {}, onEmailChange = {}, onSubmit = {}, onHaveToken = {}, onRequestAgain = {}
    )
}

@Preview(name = "Recuperar · enviado", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
private fun ForgotPasswordSentPreview() = AtelierWorkshopTheme(darkTheme = true) {
    ForgotPasswordContent(
        state = ForgotPasswordUiState(email = "andres@taller.pe", sent = true),
        onBack = {}, onEmailChange = {}, onSubmit = {}, onHaveToken = {}, onRequestAgain = {}
    )
}
