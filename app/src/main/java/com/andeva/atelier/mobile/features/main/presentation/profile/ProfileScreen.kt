package com.andeva.atelier.mobile.features.main.presentation.profile

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.andeva.atelier.mobile.BuildConfig
import com.andeva.atelier.mobile.R
import com.andeva.atelier.mobile.core.designsystem.component.AtelierBottomSheet
import com.andeva.atelier.mobile.core.designsystem.component.AtelierButton
import com.andeva.atelier.mobile.core.designsystem.component.AtelierButtonStyle
import com.andeva.atelier.mobile.core.designsystem.component.AtelierFichaPanel
import com.andeva.atelier.mobile.core.designsystem.component.AtelierGroupedCard
import com.andeva.atelier.mobile.core.designsystem.component.AtelierGroupedRow
import com.andeva.atelier.mobile.core.designsystem.component.AtelierMainTopBar
import com.andeva.atelier.mobile.core.designsystem.component.AtelierSurface
import com.andeva.atelier.mobile.core.designsystem.component.SurfaceTier
import com.andeva.atelier.mobile.core.designsystem.component.floatingBarPadding
import com.andeva.atelier.mobile.core.designsystem.component.rememberAtelierHaptics
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierTheme
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierWorkshopTheme
import com.andeva.atelier.mobile.core.designsystem.theme.CardLargeShape
import com.andeva.atelier.mobile.core.designsystem.theme.HeroShape

/**
 * Perfil de la cuenta: quién es, su taller y sede, el plan y la sesión. Se abre desde "Más" con Atrás.
 * La app no conoce el nombre del rol, así que no muestra ninguno. Cerrar sesión se confirma en hoja inferior.
 */
@Composable
fun ProfileScreen(
    onBack: (() -> Unit)?,
    onSelectBranch: () -> Unit,
    onOpenSyncQueue: () -> Unit,
    onOpenNotifications: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val haptics = rememberAtelierHaptics()
    ProfileContent(
        state = state,
        onBack = onBack,
        onSelectBranch = onSelectBranch,
        onOpenSyncQueue = onOpenSyncQueue,
        onOpenNotifications = onOpenNotifications,
        onSignOutClick = viewModel::onSignOutClick,
        onSignOutDismiss = viewModel::onSignOutDismiss,
        onSignOutConfirm = {
            haptics.confirm()
            viewModel.onSignOutConfirm()
        },
        modifier = modifier
    )
}

@Composable
private fun ProfileContent(
    state: ProfileUiState,
    onBack: (() -> Unit)?,
    onSelectBranch: () -> Unit,
    onOpenSyncQueue: () -> Unit,
    onOpenNotifications: () -> Unit,
    onSignOutClick: () -> Unit,
    onSignOutDismiss: () -> Unit,
    onSignOutConfirm: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AtelierTheme.colors
    val title = state.fullName.ifBlank { stringResource(R.string.main_tab_profile) }
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            if (onBack != null) {
                AtelierFichaPanel(
                    onBack = onBack,
                    title = title,
                    subtitle = state.email.ifBlank { null }
                )
            } else {
                AtelierMainTopBar(title = stringResource(R.string.main_tab_profile))
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .floatingBarPadding()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (onBack == null) {
                AtelierSurface(
                    modifier = Modifier.fillMaxWidth(),
                    tier = SurfaceTier.HeroNavy,
                    shape = HeroShape,
                    contentPadding = PaddingValues(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(colors.onPanel.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Person,
                                contentDescription = null,
                                tint = colors.onPanel,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleLarge,
                                color = colors.onPanel,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (state.email.isNotBlank()) {
                                Text(
                                    text = state.email,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.onPanel.copy(alpha = 0.82f),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            } else {
                AtelierSurface(
                    modifier = Modifier.fillMaxWidth(),
                    tier = SurfaceTier.Elevated,
                    shape = CardLargeShape,
                    contentPadding = PaddingValues(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = stringResource(R.string.profile_account_summary),
                            style = MaterialTheme.typography.labelLarge,
                            color = colors.primaryText
                        )
                        Text(
                            text = listOfNotNull(
                                state.tenantName.takeIf { it.isNotBlank() },
                                state.branchName
                            ).joinToString(" · ").ifBlank { stringResource(R.string.profile_branch_unknown) },
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            AtelierGroupedCard(title = stringResource(R.string.profile_section_workshop)) {
                if (state.tenantName.isNotBlank()) {
                    AtelierGroupedRow(
                        title = state.tenantName,
                        subtitle = stringResource(R.string.profile_label_workshop),
                        icon = Icons.Outlined.Business,
                        showChevron = false,
                        showDivider = true
                    )
                }
                AtelierGroupedRow(
                    title = state.branchName ?: stringResource(R.string.profile_branch_unknown),
                    subtitle = stringResource(
                        if (state.canChangeBranch) R.string.profile_branch_change else R.string.profile_label_branch
                    ),
                    icon = Icons.Outlined.Storefront,
                    showChevron = state.canChangeBranch,
                    onClick = if (state.canChangeBranch) onSelectBranch else null,
                    showDivider = state.hasPlan
                )
                if (state.hasPlan) {
                    AtelierGroupedRow(
                        title = stringResource(R.string.profile_plan_format, state.planTier),
                        subtitle = planModules(state),
                        icon = Icons.Outlined.Star,
                        showChevron = false
                    )
                }
            }

            AtelierGroupedCard(title = stringResource(R.string.profile_section_ops)) {
                AtelierGroupedRow(
                    title = stringResource(R.string.profile_item_sync),
                    subtitle = if (state.pendingCount > 0) {
                        pluralStringResource(R.plurals.profile_pending_count, state.pendingCount, state.pendingCount)
                    } else {
                        stringResource(R.string.profile_sync_up_to_date)
                    },
                    icon = Icons.Outlined.Sync,
                    trailingBadgeText = state.pendingCount.takeIf { it > 0 }?.toString(),
                    trailingBadgeColor = colors.waitContainer,
                    onClick = onOpenSyncQueue,
                    showDivider = true
                )
                AtelierGroupedRow(
                    title = stringResource(R.string.profile_item_notifications),
                    subtitle = if (state.unreadNotificationsCount > 0) {
                        pluralStringResource(
                            R.plurals.profile_unread_count, state.unreadNotificationsCount, state.unreadNotificationsCount
                        )
                    } else {
                        stringResource(R.string.profile_no_unread)
                    },
                    icon = Icons.Outlined.Notifications,
                    trailingBadgeText = state.unreadNotificationsCount.takeIf { it > 0 }?.toString(),
                    trailingBadgeColor = colors.infoContainer,
                    onClick = onOpenNotifications
                )
            }

            AtelierGroupedCard(title = stringResource(R.string.profile_section_session)) {
                AtelierGroupedRow(
                    title = stringResource(R.string.profile_sign_out),
                    subtitle = stringResource(R.string.profile_sign_out_sub),
                    icon = Icons.AutoMirrored.Outlined.Logout,
                    iconContainerColor = colors.badContainer,
                    iconTint = colors.bad,
                    onClick = onSignOutClick
                )
            }

            Text(
                text = stringResource(R.string.profile_version, BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.bodySmall,
                color = colors.muted,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )
        }
    }

    if (state.confirmingSignOut) {
        AtelierBottomSheet(
            title = stringResource(R.string.profile_sign_out_title),
            onDismiss = onSignOutDismiss,
            actions = {
                AtelierButton(
                    text = stringResource(R.string.profile_sign_out),
                    onClick = onSignOutConfirm,
                    modifier = Modifier.fillMaxWidth(),
                    style = AtelierButtonStyle.DestructiveFilled
                )
                AtelierButton(
                    text = stringResource(R.string.common_cancel),
                    onClick = onSignOutDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    style = AtelierButtonStyle.Text
                )
            }
        ) {
            Text(
                text = stringResource(
                    if (state.pendingCount > 0) R.string.profile_sign_out_message else R.string.profile_sign_out_plain
                ),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/** Módulos del plan en palabras: lo que incluye, o que es el plan base. */
@Composable
private fun planModules(state: ProfileUiState): String {
    val modules = buildList {
        if (state.iotTelemetryEnabled) add(stringResource(R.string.profile_module_obd))
        if (state.aiDiagnosticsEnabled) add(stringResource(R.string.profile_module_ai))
    }
    return modules.joinToString(" · ").ifEmpty { stringResource(R.string.profile_module_none) }
}

private val ProfileUiState.hasPlan: Boolean
    get() = planTier.isNotBlank() && !planTier.equals("None", ignoreCase = true)

// region Previews

private val previewState = ProfileUiState(
    fullName = "Carlos Mendoza Rivas",
    email = "carlos@motorescusco.pe",
    tenantName = "Motores Cusco S.A.C.",
    branchId = "b1",
    branchName = "Sede Surco",
    canChangeBranch = true,
    planTier = "Pro",
    iotTelemetryEnabled = true,
    pendingCount = 2,
    unreadNotificationsCount = 3
)

@Composable
private fun PreviewContent(state: ProfileUiState) {
    ProfileContent(state, {}, {}, {}, {}, {}, {}, {})
}

@Preview(name = "Perfil · claro", widthDp = 390, heightDp = 844)
@Composable
private fun ProfilePreviewLight() = AtelierWorkshopTheme(darkTheme = false) { PreviewContent(previewState) }

@Preview(name = "Perfil · oscuro", widthDp = 390, heightDp = 844, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ProfilePreviewDark() = AtelierWorkshopTheme(darkTheme = true) {
    PreviewContent(previewState.copy(confirmingSignOut = true))
}

// endregion
