package com.andeva.atelier.mobile.features.auth.presentation.branch

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.andeva.atelier.mobile.R
import com.andeva.atelier.mobile.core.designsystem.component.AtelierBanner
import com.andeva.atelier.mobile.core.designsystem.component.AtelierButton
import com.andeva.atelier.mobile.core.designsystem.component.AtelierButtonStyle
import com.andeva.atelier.mobile.core.designsystem.component.pressScale
import com.andeva.atelier.mobile.core.designsystem.component.rise
import com.andeva.atelier.mobile.core.designsystem.text.UiText
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierTheme
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierWorkshopTheme
import com.andeva.atelier.mobile.core.designsystem.theme.CardShape
import com.andeva.atelier.mobile.features.auth.domain.BranchOption

/** A5b Elegir sede. Solo la ven las cuentas que entraron sin perfil laboral (US03). */
@Composable
fun BranchSelectionScreen(
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BranchSelectionViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.done) {
        if (state.done) onDone()
    }

    BranchSelectionContent(
        state = state,
        onSelect = viewModel::onSelect,
        onContinue = viewModel::onContinue,
        onSkip = viewModel::onSkip,
        onRetry = viewModel::load,
        modifier = modifier
    )
}

@Composable
internal fun BranchSelectionContent(
    state: BranchSelectionUiState,
    onSelect: (String) -> Unit,
    onContinue: () -> Unit,
    onSkip: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val atelier = AtelierTheme.colors
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 48.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(modifier = Modifier.rise(0), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.branch_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = stringResource(R.string.branch_subtitle),
                    style = MaterialTheme.typography.bodyLarge,
                    color = atelier.inkSecondary
                )
            }

            when {
                state.isLoading -> BranchSkeleton()
                state.error != null -> {
                    AtelierBanner(message = state.error.asString(), icon = Icons.Filled.Error)
                    AtelierButton(
                        text = stringResource(R.string.branch_retry),
                        onClick = onRetry,
                        modifier = Modifier.fillMaxWidth(),
                        style = AtelierButtonStyle.Outlined
                    )
                }
                state.branches.isEmpty() -> AtelierBanner(
                    title = stringResource(R.string.branch_empty_title),
                    message = stringResource(R.string.branch_empty_body),
                    icon = Icons.Outlined.Storefront,
                    kind = com.andeva.atelier.mobile.core.designsystem.component.BannerKind.Wait
                )
                else -> Column(
                    modifier = Modifier
                        .rise(1)
                        .selectableGroup(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    state.branches.forEach { branch ->
                        BranchCard(
                            branch = branch,
                            selected = branch.id == state.selectedId,
                            enabled = !state.isSaving,
                            onClick = { onSelect(branch.id) }
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .rise(2)
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val selected = state.selected
            AtelierButton(
                text = if (selected != null) stringResource(R.string.branch_continue_in, selected.name)
                else stringResource(R.string.branch_continue),
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth(),
                enabled = state.canContinue,
                loading = state.isSaving
            )
            AtelierButton(
                text = stringResource(R.string.branch_later),
                onClick = onSkip,
                modifier = Modifier.fillMaxWidth(),
                style = AtelierButtonStyle.Text,
                enabled = !state.isSaving
            )
        }
    }
}

/** Tarjeta de sede: toda es el botón de radio (área táctil completa) y la elegida lleva borde azul de 2 dp. */
@Composable
private fun BranchCard(
    branch: BranchOption,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val atelier = AtelierTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val isDark = scheme.background.luminance() < 0.5f
    val restingBorder = if (isDark) Color.Transparent else atelier.outlineSoft
    val border by animateColorAsState(
        targetValue = if (selected) scheme.primary else restingBorder,
        animationSpec = tween(160),
        label = "branchBorder"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pressScale(interaction, pressed = 0.985f)
            .clip(CardShape)
            .background(atelier.card)
            .border(2.dp, border, CardShape)
            .selectable(
                selected = selected,
                enabled = enabled,
                role = Role.RadioButton,
                interactionSource = interaction,
                indication = androidx.compose.material3.ripple(),
                onClick = onClick
            )
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(scheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.Storefront, contentDescription = null, tint = scheme.onPrimaryContainer)
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = branch.name,
                style = MaterialTheme.typography.titleMedium,
                color = scheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            branch.geofenceRadiusMeters?.let { radius ->
                Text(
                    text = stringResource(R.string.branch_geofence, radius),
                    style = MaterialTheme.typography.bodyMedium,
                    color = atelier.muted
                )
            }
        }
        RadioMark(selected)
    }
}

/** Marca de radio de 24 dp de la galería (el estado lo anuncia `selectable`, no este dibujo). */
@Composable
private fun RadioMark(selected: Boolean) {
    val scheme = MaterialTheme.colorScheme
    val color by animateColorAsState(
        targetValue = if (selected) scheme.primary else scheme.outline,
        animationSpec = tween(160),
        label = "radio"
    )
    Box(
        modifier = Modifier
            .size(24.dp)
            .border(2.dp, color, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (selected) Box(modifier = Modifier.size(12.dp).background(scheme.primary, CircleShape))
    }
}

@Composable
private fun BranchSkeleton() {
    val loading = stringResource(R.string.branch_loading)
    Column(
        modifier = Modifier.semantics { contentDescription = loading },
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        repeat(3) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .clip(CardShape)
                    .background(AtelierTheme.colors.card)
            )
        }
    }
}

private val previewBranches = listOf(
    BranchOption("1", "Sede San Isidro", 50),
    BranchOption("2", "Sede Surquillo", 80),
    BranchOption("3", "Sede Ate", null)
)

@Preview(name = "Elegir sede · oscuro", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
private fun BranchSelectionPreviewDark() = AtelierWorkshopTheme(darkTheme = true) {
    BranchSelectionContent(
        state = BranchSelectionUiState(isLoading = false, branches = previewBranches, selectedId = "1"),
        onSelect = {}, onContinue = {}, onSkip = {}, onRetry = {}
    )
}

@Preview(name = "Elegir sede · error · claro", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
private fun BranchSelectionPreviewError() = AtelierWorkshopTheme(darkTheme = false) {
    BranchSelectionContent(
        state = BranchSelectionUiState(isLoading = false, error = UiText.Dynamic("No se pudieron cargar las sedes.")),
        onSelect = {}, onContinue = {}, onSkip = {}, onRetry = {}
    )
}
