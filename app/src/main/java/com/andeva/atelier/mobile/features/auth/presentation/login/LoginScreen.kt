package com.andeva.atelier.mobile.features.auth.presentation.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
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
import com.andeva.atelier.mobile.core.designsystem.component.AtelierLogo
import com.andeva.atelier.mobile.core.designsystem.component.AtelierPasswordField
import com.andeva.atelier.mobile.core.designsystem.component.AtelierTextField
import com.andeva.atelier.mobile.core.designsystem.component.BannerKind
import com.andeva.atelier.mobile.core.designsystem.component.rise
import com.andeva.atelier.mobile.core.designsystem.text.UiText
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierTheme
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierWorkshopTheme

/** A5 Iniciar sesión. Con sesión guardada `AppNavHost` cambia de grafo solo. */
@Composable
fun LoginScreen(
    onHaveInvitation: () -> Unit,
    onVerifyEmail: (email: String) -> Unit,
    modifier: Modifier = Modifier,
    onForgotPassword: (() -> Unit)? = null,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.unverifiedEmail) {
        state.unverifiedEmail?.let { email ->
            onVerifyEmail(email)
            viewModel.onVerificationHandled()
        }
    }

    LoginContent(
        state = state,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onPasswordVisibleChange = viewModel::onPasswordVisibleChange,
        onSubmit = viewModel::onSubmit,
        onHaveInvitation = onHaveInvitation,
        onForgotPassword = onForgotPassword,
        modifier = modifier
    )
}

@Composable
internal fun LoginContent(
    state: LoginUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onPasswordVisibleChange: (Boolean) -> Unit,
    onSubmit: () -> Unit,
    onHaveInvitation: () -> Unit,
    onForgotPassword: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val atelier = AtelierTheme.colors
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .heightIn(min = maxHeight)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            AtelierLogo(modifier = Modifier.padding(top = 24.dp).height(36.dp))
            Spacer(Modifier.height(28.dp))

            Column(modifier = Modifier.rise(0), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.auth_login_title),
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = stringResource(R.string.auth_login_subtitle),
                    style = MaterialTheme.typography.bodyLarge,
                    color = atelier.inkSecondary
                )
            }

            if (state.emailVerified) {
                AtelierBanner(
                    message = stringResource(R.string.auth_email_verified),
                    icon = Icons.Filled.CheckCircle,
                    kind = BannerKind.Ok
                )
            }
            state.error?.let { error ->
                AtelierBanner(message = error.asString(), icon = Icons.Filled.Error, kind = BannerKind.Bad)
            }

            Column(modifier = Modifier.rise(1), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                AtelierTextField(
                    label = stringResource(R.string.auth_email_label),
                    value = state.email,
                    onValueChange = onEmailChange,
                    leadingIcon = Icons.Outlined.Email,
                    enabled = !state.isLoading,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                    autofill = ContentType.EmailAddress + ContentType.Username
                )
                AtelierPasswordField(
                    label = stringResource(R.string.auth_password_label),
                    value = state.password,
                    onValueChange = onPasswordChange,
                    visible = state.passwordVisible,
                    onVisibleChange = onPasswordVisibleChange,
                    enabled = !state.isLoading,
                    imeAction = ImeAction.Done,
                    onImeAction = onSubmit
                )
                if (onForgotPassword != null) {
                    AtelierButton(
                        text = stringResource(R.string.auth_forgot_password),
                        onClick = onForgotPassword,
                        style = AtelierButtonStyle.Text,
                        modifier = Modifier.offset(x = (-12).dp)
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            Column(
                modifier = Modifier
                    .rise(2)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AtelierButton(
                    text = stringResource(R.string.auth_login_action),
                    onClick = onSubmit,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = state.canSubmit,
                    loading = state.isLoading
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
                    Text(
                        text = stringResource(R.string.auth_or),
                        style = MaterialTheme.typography.labelMedium,
                        color = atelier.muted
                    )
                    HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
                }
                AtelierButton(
                    text = stringResource(R.string.auth_have_invitation),
                    onClick = onHaveInvitation,
                    modifier = Modifier.fillMaxWidth(),
                    style = AtelierButtonStyle.Outlined,
                    enabled = !state.isLoading,
                    icon = Icons.Outlined.Link
                )
            }
        }
    }
}

@Preview(name = "Login · oscuro", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
private fun LoginPreviewDark() = AtelierWorkshopTheme(darkTheme = true) {
    LoginContent(
        state = LoginUiState(email = "andres.vilchez@taller.pe", password = "contraseña123"),
        onEmailChange = {}, onPasswordChange = {}, onPasswordVisibleChange = {},
        onSubmit = {}, onHaveInvitation = {}, onForgotPassword = {}
    )
}

@Preview(name = "Login · error · claro", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
private fun LoginPreviewErrorLight() = AtelierWorkshopTheme(darkTheme = false) {
    LoginContent(
        state = LoginUiState(
            email = "andres.vilchez@taller.pe",
            password = "contraseña123",
            error = UiText.Dynamic("Correo o contraseña incorrectos. Te quedan 3 intentos antes de bloquear el acceso por 15 minutos.")
        ),
        onEmailChange = {}, onPasswordChange = {}, onPasswordVisibleChange = {},
        onSubmit = {}, onHaveInvitation = {}, onForgotPassword = {}
    )
}
