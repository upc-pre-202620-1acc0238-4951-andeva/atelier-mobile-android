package com.andeva.atelier.mobile.features.auth.presentation.branch

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.andeva.atelier.mobile.R
import com.andeva.atelier.mobile.core.designsystem.component.AtelierButton
import com.andeva.atelier.mobile.core.designsystem.component.AtelierButtonStyle
import com.andeva.atelier.mobile.core.designsystem.component.AtelierEmptyState
import com.andeva.atelier.mobile.core.designsystem.component.AtelierErrorState
import com.andeva.atelier.mobile.core.designsystem.component.AtelierListRow
import com.andeva.atelier.mobile.core.designsystem.component.AtelierSurface
import com.andeva.atelier.mobile.core.designsystem.component.SurfaceTier
import com.andeva.atelier.mobile.core.designsystem.component.rise
import com.andeva.atelier.mobile.core.designsystem.text.UiText
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierTheme
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierWorkshopTheme
import com.andeva.atelier.mobile.core.designsystem.theme.CardLargeShape
import com.andeva.atelier.mobile.features.auth.domain.BranchOption
import com.andeva.atelier.mobile.features.auth.presentation.AuthHeroHeader

/** A5b Elegir sede. Solo la ven las cuentas que entraron sin perfil laboral (US03) o que cambian de sede (H33). */
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
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            AuthHeroHeader(
                title = stringResource(R.string.branch_title),
                subtitle = stringResource(R.string.branch_subtitle),
                modifier = Modifier.rise(0)
            )

            val loadingLabel = stringResource(R.string.branch_loading)
            AtelierSurface(
                modifier = Modifier
                    .fillMaxWidth()
                    .rise(1),
                tier = SurfaceTier.Elevated,
                shape = CardLargeShape,
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                when {
                    state.isLoading -> Column(modifier = Modifier.semantics { contentDescription = loadingLabel }) {
                        repeat(3) { AtelierListRow(title = "Sede San Isidro", subtitle = "Radio de 50 m", loading = true) }
                    }
                    state.error != null -> AtelierErrorState(
                        message = state.error.asString(),
                        modifier = Modifier.padding(horizontal = 16.dp),
                        onRetry = onRetry
                    )
                    state.branches.isEmpty() -> AtelierEmptyState(
                        text = stringResource(R.string.branch_empty_title) + " " + stringResource(R.string.branch_empty_body),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    else -> Column(modifier = Modifier.selectableGroup()) {
                        state.branches.forEachIndexed { index, branch ->
                            BranchRow(
                                branch = branch,
                                selected = branch.id == state.selectedId,
                                isAssigned = branch.id == state.assignedBranchId,
                                enabled = !state.isSaving,
                                showDivider = index < state.branches.lastIndex,
                                onClick = { onSelect(branch.id) }
                            )
                        }
                    }
                }
            }
        }

        HorizontalDivider(color = AtelierTheme.colors.outlineSoft)
        Column(
            modifier = Modifier
                .rise(2)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 16.dp),
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

/** Fila de sede: toda es el botón de radio; la elegida cambia de marca (círculo lleno) además de color. */
@Composable
private fun BranchRow(
    branch: BranchOption,
    selected: Boolean,
    isAssigned: Boolean,
    enabled: Boolean,
    showDivider: Boolean,
    onClick: () -> Unit
) {
    val line = listOfNotNull(
        stringResource(R.string.branch_assigned_badge).takeIf { isAssigned },
        branch.geofenceRadiusMeters?.let { stringResource(R.string.branch_geofence, it) }
    ).joinToString(" · ")
    AtelierListRow(
        title = branch.name,
        subtitle = line.ifEmpty { null },
        showDivider = showDivider,
        modifier = Modifier.selectable(
            selected = selected,
            enabled = enabled,
            role = Role.RadioButton,
            onClick = onClick
        ),
        trailing = {
            Icon(
                imageVector = if (selected) Icons.Filled.RadioButtonChecked else Icons.Outlined.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
            )
        }
    )
}

private val previewBranches = listOf(
    BranchOption("1", "Sede San Isidro", 50),
    BranchOption("2", "Sede Surquillo", 80),
    BranchOption("3", "Sede Ate", null)
)

@Preview(name = "Elegir sede · claro", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun BranchSelectionPreviewLight() = AtelierWorkshopTheme(darkTheme = false) {
    BranchSelectionContent(
        state = BranchSelectionUiState(
            isLoading = false, branches = previewBranches, selectedId = "2", assignedBranchId = "1"
        ),
        onSelect = {}, onContinue = {}, onSkip = {}, onRetry = {}
    )
}

@Preview(name = "Elegir sede · oscuro", widthDp = 390, heightDp = 844, showBackground = true, backgroundColor = 0xFF262626)
@Composable
private fun BranchSelectionPreviewDark() = AtelierWorkshopTheme(darkTheme = true) {
    BranchSelectionContent(
        state = BranchSelectionUiState(isLoading = false, branches = previewBranches, selectedId = "1"),
        onSelect = {}, onContinue = {}, onSkip = {}, onRetry = {}
    )
}

@Preview(name = "Elegir sede · error", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun BranchSelectionPreviewError() = AtelierWorkshopTheme(darkTheme = false) {
    BranchSelectionContent(
        state = BranchSelectionUiState(isLoading = false, error = UiText.Dynamic("No se pudieron cargar las sedes.")),
        onSelect = {}, onContinue = {}, onSkip = {}, onRetry = {}
    )
}

@Preview(name = "Elegir sede · cargando", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun BranchSelectionPreviewLoading() = AtelierWorkshopTheme(darkTheme = false) {
    BranchSelectionContent(
        state = BranchSelectionUiState(),
        onSelect = {}, onContinue = {}, onSkip = {}, onRetry = {}
    )
}
