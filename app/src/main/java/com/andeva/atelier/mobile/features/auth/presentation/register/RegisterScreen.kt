package com.andeva.atelier.mobile.features.auth.presentation.register

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.LinkOff
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.andeva.atelier.mobile.R
import com.andeva.atelier.mobile.core.designsystem.component.AtelierBanner
import com.andeva.atelier.mobile.core.designsystem.component.AtelierButton
import com.andeva.atelier.mobile.core.designsystem.component.AtelierButtonStyle
import com.andeva.atelier.mobile.core.designsystem.component.AtelierChip
import com.andeva.atelier.mobile.core.designsystem.component.AtelierPasswordField
import com.andeva.atelier.mobile.core.designsystem.component.AtelierSurface
import com.andeva.atelier.mobile.core.designsystem.component.AtelierTextField
import com.andeva.atelier.mobile.core.designsystem.component.AtelierTopBar
import com.andeva.atelier.mobile.core.designsystem.component.ChipKind
import com.andeva.atelier.mobile.core.designsystem.component.SurfaceTier
import com.andeva.atelier.mobile.core.designsystem.component.rise
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierTheme
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierWorkshopTheme
import com.andeva.atelier.mobile.core.designsystem.theme.CardLargeShape
import com.andeva.atelier.mobile.features.auth.domain.InvitationPreview
import com.andeva.atelier.mobile.features.auth.presentation.AuthHeroHeader

/** A3 Registro por invitación: enlace → formulario | vencida. Al aceptar, `AppNavHost` entra a la app. */
@Composable
fun RegisterScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RegisterViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    RegisterContent(
        state = state,
        onBack = onBack,
        onLinkChange = viewModel::onLinkChange,
        onValidateLink = viewModel::onValidateLink,
        onFirstNameChange = viewModel::onFirstNameChange,
        onLastNameChange = viewModel::onLastNameChange,
        onPasswordChange = viewModel::onPasswordChange,
        onConfirmPasswordChange = viewModel::onConfirmPasswordChange,
        onPasswordVisibleChange = viewModel::onPasswordVisibleChange,
        onConfirmVisibleChange = viewModel::onConfirmVisibleChange,
        onSubmit = viewModel::onSubmit,
        modifier = modifier
    )
}

@Composable
internal fun RegisterContent(
    state: RegisterUiState,
    onBack: () -> Unit,
    onLinkChange: (String) -> Unit,
    onValidateLink: () -> Unit,
    onFirstNameChange: (String) -> Unit,
    onLastNameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onPasswordVisibleChange: (Boolean) -> Unit,
    onConfirmVisibleChange: (Boolean) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding()
            .imePadding()
    ) {
        AtelierTopBar(title = stringResource(R.string.auth_register_title), onBack = onBack)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            when (state.step) {
                RegisterStep.Link -> LinkStep(state, onLinkChange, onValidateLink)
                RegisterStep.Form -> FormStep(
                    state, onFirstNameChange, onLastNameChange, onPasswordChange, onConfirmPasswordChange,
                    onPasswordVisibleChange, onConfirmVisibleChange, onSubmit
                )
                RegisterStep.Gone -> GoneStep(state.problem)
            }
        }

        // Pie fijo: la acción principal siempre queda al alcance del pulgar.
        HorizontalDivider(color = AtelierTheme.colors.outlineSoft)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 16.dp)
        ) {
            when (state.step) {
                RegisterStep.Link -> AtelierButton(
                    text = stringResource(R.string.auth_continue),
                    onClick = onValidateLink,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = state.canValidate,
                    loading = state.isLoading
                )
                RegisterStep.Form -> AtelierButton(
                    text = stringResource(R.string.auth_complete_registration),
                    onClick = onSubmit,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = state.canSubmit,
                    loading = state.isLoading
                )
                RegisterStep.Gone -> AtelierButton(
                    text = stringResource(R.string.auth_back_to_login),
                    onClick = onBack,
                    modifier = Modifier.fillMaxWidth(),
                    style = AtelierButtonStyle.Outlined
                )
            }
        }
    }
}

@Composable
private fun LinkStep(
    state: RegisterUiState,
    onLinkChange: (String) -> Unit,
    onValidateLink: () -> Unit
) {
    AuthHeroHeader(
        title = stringResource(R.string.auth_link_title),
        subtitle = stringResource(R.string.auth_link_body),
        modifier = Modifier.rise(0)
    )
    state.error?.let { AtelierBanner(message = it.asString(), icon = Icons.Filled.Error) }
    AtelierSurface(
        modifier = Modifier
            .fillMaxWidth()
            .rise(1),
        tier = SurfaceTier.Elevated,
        shape = CardLargeShape,
        contentPadding = PaddingValues(20.dp)
    ) {
        AtelierTextField(
            label = stringResource(R.string.auth_link_label),
            value = state.linkInput,
            onValueChange = onLinkChange,
            leadingIcon = Icons.Outlined.Link,
            placeholder = stringResource(R.string.auth_link_placeholder),
            enabled = !state.isLoading,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Go),
            keyboardActions = KeyboardActions(onGo = { onValidateLink() })
        )
    }
}

@Composable
private fun FormStep(
    state: RegisterUiState,
    onFirstNameChange: (String) -> Unit,
    onLastNameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onPasswordVisibleChange: (Boolean) -> Unit,
    onConfirmVisibleChange: (Boolean) -> Unit,
    onSubmit: () -> Unit
) {
    val invitation = state.invitation
    AuthHeroHeader(
        title = invitation?.tenantName ?: stringResource(R.string.auth_register_title),
        subtitle = if (invitation != null) stringResource(R.string.auth_invited_to) else null,
        modifier = Modifier.rise(0),
        badge = if (invitation != null && invitation.roleName.isNotBlank()) {
            {
                AtelierChip(
                    text = stringResource(R.string.auth_invited_role, invitation.roleName),
                    kind = ChipKind.Info,
                    icon = Icons.Filled.Badge
                )
            }
        } else null
    )
    state.error?.let { AtelierBanner(message = it.asString(), icon = Icons.Filled.Error) }

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
                label = stringResource(R.string.auth_first_name),
                value = state.firstName,
                onValueChange = onFirstNameChange,
                leadingIcon = Icons.Outlined.Person,
                enabled = !state.isLoading,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                autofill = ContentType.PersonFirstName
            )
            AtelierTextField(
                label = stringResource(R.string.auth_last_name),
                value = state.lastName,
                onValueChange = onLastNameChange,
                leadingIcon = Icons.Outlined.Person,
                enabled = !state.isLoading,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                autofill = ContentType.PersonLastName
            )
            AtelierPasswordField(
                label = stringResource(R.string.auth_password_label),
                value = state.password,
                onValueChange = onPasswordChange,
                visible = state.passwordVisible,
                onVisibleChange = onPasswordVisibleChange,
                enabled = !state.isLoading,
                imeAction = ImeAction.Next,
                autofill = ContentType.NewPassword
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PasswordRule(stringResource(R.string.auth_rule_length), state.hasMinLength)
                PasswordRule(stringResource(R.string.auth_rule_uppercase), state.hasUppercase)
                PasswordRule(stringResource(R.string.auth_rule_match), state.passwordsMatch)
            }
            AtelierPasswordField(
                label = stringResource(R.string.auth_confirm_password),
                value = state.confirmPassword,
                onValueChange = onConfirmPasswordChange,
                visible = state.confirmVisible,
                onVisibleChange = onConfirmVisibleChange,
                enabled = !state.isLoading,
                imeAction = ImeAction.Done,
                onImeAction = onSubmit,
                autofill = ContentType.NewPassword
            )
        }
    }
}

@Composable
private fun GoneStep(problem: InvitationProblem?) {
    val expired = problem != InvitationProblem.NotFound
    AuthHeroHeader(
        title = stringResource(if (expired) R.string.auth_invitation_expired_title else R.string.auth_invitation_gone_title),
        modifier = Modifier.rise(0)
    )
    AtelierSurface(
        modifier = Modifier
            .fillMaxWidth()
            .rise(1),
        tier = SurfaceTier.Elevated,
        shape = CardLargeShape,
        contentPadding = PaddingValues(20.dp)
    ) {
        AtelierBanner(
            title = stringResource(if (expired) R.string.auth_invitation_expired_title else R.string.auth_invitation_gone_title),
            message = stringResource(if (expired) R.string.auth_invitation_expired_body else R.string.auth_invitation_gone_body),
            icon = Icons.Outlined.LinkOff
        )
    }
}

/** Regla de contraseña: verde y rellena al cumplirse, gris y vacía mientras no. Lleva texto, no solo color. */
@Composable
internal fun PasswordRule(text: String, met: Boolean) {
    val atelier = AtelierTheme.colors
    val icon: ImageVector = if (met) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked
    val stateText = stringResource(if (met) R.string.auth_rule_met else R.string.auth_rule_pending)
    Row(
        modifier = Modifier.semantics(mergeDescendants = true) { stateDescription = stateText },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = if (met) atelier.ok else atelier.muted)
        Text(text = text, style = MaterialTheme.typography.bodyMedium, color = if (met) atelier.ok else atelier.muted)
    }
}

@Preview(name = "Registro · formulario", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
private fun RegisterFormPreview() = AtelierWorkshopTheme(darkTheme = true) {
    RegisterContent(
        state = RegisterUiState(
            step = RegisterStep.Form,
            invitation = InvitationPreview("andres@taller.pe", "Taller Sede San Isidro", "Mecánico"),
            firstName = "Andrés", lastName = "Vílchez Ramos", password = "Taller123"
        ),
        onBack = {}, onLinkChange = {}, onValidateLink = {}, onFirstNameChange = {}, onLastNameChange = {},
        onPasswordChange = {}, onConfirmPasswordChange = {}, onPasswordVisibleChange = {},
        onConfirmVisibleChange = {}, onSubmit = {}
    )
}

@Preview(name = "Registro · vencida · claro", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
private fun RegisterGonePreview() = AtelierWorkshopTheme(darkTheme = false) {
    RegisterContent(
        state = RegisterUiState(step = RegisterStep.Gone, problem = InvitationProblem.Expired),
        onBack = {}, onLinkChange = {}, onValidateLink = {}, onFirstNameChange = {}, onLastNameChange = {},
        onPasswordChange = {}, onConfirmPasswordChange = {}, onPasswordVisibleChange = {},
        onConfirmVisibleChange = {}, onSubmit = {}
    )
}
