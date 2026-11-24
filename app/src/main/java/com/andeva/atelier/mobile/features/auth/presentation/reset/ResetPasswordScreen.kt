package com.andeva.atelier.mobile.features.auth.presentation.reset

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
import com.andeva.atelier.mobile.core.designsystem.component.AtelierPasswordField
import com.andeva.atelier.mobile.core.designsystem.component.AtelierSurface
import com.andeva.atelier.mobile.core.designsystem.component.AtelierTextField
import com.andeva.atelier.mobile.core.designsystem.component.AtelierTopBar
import com.andeva.atelier.mobile.core.designsystem.component.SurfaceTier
import com.andeva.atelier.mobile.core.designsystem.component.rise
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierTheme
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierWorkshopTheme
import com.andeva.atelier.mobile.core.designsystem.theme.CardLargeShape
import com.andeva.atelier.mobile.features.auth.presentation.AuthHeroHeader
import com.andeva.atelier.mobile.features.auth.presentation.register.PasswordRule

/** A6 Recuperar contraseña · paso 2: pegar el enlace o código y elegir la contraseña nueva. */
@Composable
fun ResetPasswordScreen(
    onBack: () -> Unit,
    onRequestNewLink: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ResetPasswordViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.done) {
        if (state.done) onDone()
    }

    ResetPasswordContent(
        state = state,
        onBack = onBack,
        onTokenChange = viewModel::onTokenChange,
        onPasswordChange = viewModel::onPasswordChange,
        onConfirmationChange = viewModel::onConfirmationChange,
        onPasswordVisibleChange = viewModel::onPasswordVisibleChange,
        onConfirmationVisibleChange = viewModel::onConfirmationVisibleChange,
        onSubmit = viewModel::onSubmit,
        onRequestNewLink = onRequestNewLink,
        modifier = modifier
    )
}

@Composable
internal fun ResetPasswordContent(
    state: ResetPasswordUiState,
    onBack: () -> Unit,
    onTokenChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmationChange: (String) -> Unit,
    onPasswordVisibleChange: (Boolean) -> Unit,
    onConfirmationVisibleChange: (Boolean) -> Unit,
    onSubmit: () -> Unit,
    onRequestNewLink: () -> Unit,
    modifier: Modifier = Modifier
) {
    val atelier = AtelierTheme.colors
    var showDiscardDialog by rememberSaveable { mutableStateOf(false) }
    val handleBack = {
        if (state.hasUnsavedChanges) {
            showDiscardDialog = true
        } else {
            onBack()
        }
    }
    BackHandler(enabled = state.hasUnsavedChanges) {
        showDiscardDialog = true
    }
    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text(stringResource(R.string.common_discard_changes_title)) },
            text = { Text(stringResource(R.string.common_discard_changes_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscardDialog = false
                        onBack()
                    }
                ) {
                    Text(stringResource(R.string.common_discard_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text(stringResource(R.string.common_keep_editing))
                }
            }
        )
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding()
            .imePadding()
    ) {
        AtelierTopBar(title = stringResource(R.string.auth_reset_title), onBack = handleBack)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            AuthHeroHeader(
                title = stringResource(R.string.auth_reset_title),
                subtitle = stringResource(R.string.auth_reset_body),
                modifier = Modifier.rise(0)
            )

            if (state.tokenInvalid) {
                AtelierBanner(
                    message = stringResource(R.string.auth_reset_invalid),
                    icon = Icons.Filled.Error,
                    modifier = Modifier.rise(0)
                )
                AtelierButton(
                    text = stringResource(R.string.auth_reset_request_new),
                    onClick = onRequestNewLink,
                    style = AtelierButtonStyle.Outlined,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            state.error?.let { error ->
                AtelierBanner(message = error.asString(), icon = Icons.Filled.Error)
            }

            AtelierSurface(
                modifier = Modifier
                    .fillMaxWidth()
                    .rise(1),
                tier = SurfaceTier.Elevated,
                shape = CardLargeShape,
                contentPadding = PaddingValues(20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    AtelierTextField(
                        label = stringResource(R.string.auth_reset_token_label),
                        value = state.token,
                        onValueChange = onTokenChange,
                        leadingIcon = Icons.Outlined.Link,
                        isError = state.tokenInvalid,
                        enabled = !state.isLoading,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next)
                    )
                    AtelierPasswordField(
                        label = stringResource(R.string.auth_password_label),
                        value = state.password,
                        onValueChange = onPasswordChange,
                        visible = state.passwordVisible,
                        onVisibleChange = onPasswordVisibleChange,
                        enabled = !state.isLoading,
                        imeAction = ImeAction.Next,
                        autofill = null
                    )
                    AtelierPasswordField(
                        label = stringResource(R.string.auth_confirm_password),
                        value = state.confirmation,
                        onValueChange = onConfirmationChange,
                        visible = state.confirmationVisible,
                        onVisibleChange = onConfirmationVisibleChange,
                        enabled = !state.isLoading,
                        imeAction = ImeAction.Done,
                        onImeAction = onSubmit,
                        autofill = null
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        PasswordRule(stringResource(R.string.auth_rule_length), state.hasMinLength)
                        PasswordRule(stringResource(R.string.auth_rule_uppercase), state.hasUppercase)
                        PasswordRule(stringResource(R.string.auth_rule_match), state.passwordsMatch)
                        Text(
                            text = stringResource(R.string.auth_reset_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = atelier.muted,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = atelier.outlineSoft)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 16.dp)
        ) {
            AtelierButton(
                text = stringResource(R.string.auth_reset_action),
                onClick = onSubmit,
                modifier = Modifier.fillMaxWidth(),
                enabled = state.canSubmit,
                loading = state.isLoading
            )
        }
    }
}

@Preview(name = "Nueva contraseña", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
private fun ResetPasswordPreview() = AtelierWorkshopTheme(darkTheme = true) {
    ResetPasswordContent(
        state = ResetPasswordUiState(token = "https://atelier.pe/reset?token=abc", password = "Taller123", confirmation = "Taller12"),
        onBack = {}, onTokenChange = {}, onPasswordChange = {}, onConfirmationChange = {},
        onPasswordVisibleChange = {}, onConfirmationVisibleChange = {}, onSubmit = {}, onRequestNewLink = {}
    )
}

@Preview(name = "Nueva contraseña · enlace vencido · claro", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
private fun ResetPasswordInvalidPreview() = AtelierWorkshopTheme(darkTheme = false) {
    ResetPasswordContent(
        state = ResetPasswordUiState(token = "abc", password = "Taller123", confirmation = "Taller123", tokenInvalid = true),
        onBack = {}, onTokenChange = {}, onPasswordChange = {}, onConfirmationChange = {},
        onPasswordVisibleChange = {}, onConfirmationVisibleChange = {}, onSubmit = {}, onRequestNewLink = {}
    )
}
