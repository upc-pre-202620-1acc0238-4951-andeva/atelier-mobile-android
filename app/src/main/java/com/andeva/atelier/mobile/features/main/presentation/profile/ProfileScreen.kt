package com.andeva.atelier.mobile.features.main.presentation.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.andeva.atelier.mobile.BuildConfig
import com.andeva.atelier.mobile.R
import com.andeva.atelier.mobile.core.designsystem.component.AtelierButton
import com.andeva.atelier.mobile.core.designsystem.component.AtelierGroupedCard
import com.andeva.atelier.mobile.core.designsystem.component.AtelierGroupedRow
import com.andeva.atelier.mobile.core.designsystem.component.AtelierMainTopBar
import com.andeva.atelier.mobile.core.designsystem.component.AtelierTopBar
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierTheme

/**
 * Pantalla de perfil de usuario rediseñada al estilo limpio y espacioso de Lemon Cash.
 *
 * Estructurada en:
 * 1. Hero Card: Avatar con iniciales, Nombre, Correo, Tag de rol y Taller.
 * 2. Tarjeta Agrupada "Mi cuenta y taller" (Datos personales, Sede y turno, Plan).
 * 3. Tarjeta Agrupada "Operaciones y sincronización" (Cola offline, Alertas, Permisos).
 * 4. Tarjeta Agrupada "Seguridad y sesión" (Cerrar sesión y versión).
 */
@Composable
fun ProfileScreen(
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    onOpenMenu: (() -> Unit)? = null,
    onSelectBranch: (() -> Unit)? = null,
    onOpenSyncQueue: (() -> Unit)? = null,
    onOpenNotifications: (() -> Unit)? = null,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scheme = MaterialTheme.colorScheme
    val atelier = AtelierTheme.colors

    var showPersonalDataDialog by remember { mutableStateOf(false) }
    var showPlanDialog by remember { mutableStateOf(false) }
    var showPermissionsDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(scheme.background)
            .safeDrawingPadding()
    ) {
        if (onBack != null) {
            AtelierTopBar(title = stringResource(R.string.main_tab_profile), onBack = onBack)
        } else if (onOpenMenu != null) {
            AtelierMainTopBar(
                title = stringResource(R.string.main_tab_profile),
                onOpenMenu = onOpenMenu,
                onOpenNotifications = onOpenNotifications,
                unreadNotificationsCount = state.unreadNotificationsCount,
                onOpenSync = onOpenSyncQueue,
                pendingSyncCount = state.pendingCount
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Hero Card (Lemon Cash style)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(elevation = 2.dp, shape = RoundedCornerShape(20.dp), ambientColor = scheme.primary.copy(alpha = 0.05f)),
                shape = RoundedCornerShape(20.dp),
                color = atelier.card,
                border = BorderStroke(1.dp, atelier.outlineSoft.copy(alpha = 0.6f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Surface(
                        modifier = Modifier.size(62.dp),
                        shape = CircleShape,
                        color = scheme.primaryContainer
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = state.initials.ifBlank { "AT" },
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = scheme.onPrimaryContainer
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = state.fullName.ifBlank { stringResource(R.string.main_tab_profile) },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = scheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = state.email,
                            style = MaterialTheme.typography.bodySmall,
                            color = atelier.inkSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (state.tenantName.isNotBlank()) {
                            Text(
                                text = state.tenantName,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = scheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // 2. Grupo 1: Mi cuenta y taller
            AtelierGroupedCard(title = stringResource(R.string.profile_section_account)) {
                AtelierGroupedRow(
                    title = stringResource(R.string.profile_item_personal_data),
                    subtitle = state.roleTitle,
                    icon = Icons.Outlined.Person,
                    onClick = { showPersonalDataDialog = true },
                    showDivider = true
                )
                AtelierGroupedRow(
                    title = stringResource(R.string.profile_item_branch_shift),
                    subtitle = state.branchName ?: stringResource(R.string.more_item_branch_sub),
                    icon = Icons.Outlined.Storefront,
                    onClick = onSelectBranch,
                    showDivider = true
                )
                AtelierGroupedRow(
                    title = stringResource(R.string.profile_item_plan),
                    subtitle = buildString {
                        append(stringResource(R.string.profile_plan_format, state.planName))
                        if (state.iotTelemetryEnabled) append(" · OBD-II")
                        if (state.aiDiagnosticsEnabled) append(" · IA")
                    },
                    icon = Icons.Outlined.Star,
                    onClick = { showPlanDialog = true },
                    showDivider = false
                )
            }

            // 3. Grupo 2: Operaciones y sincronización
            AtelierGroupedCard(title = stringResource(R.string.profile_section_ops)) {
                val syncSubtitle = if (state.pendingCount > 0) {
                    pluralStringResource(R.plurals.profile_pending_count, state.pendingCount, state.pendingCount)
                } else {
                    stringResource(R.string.profile_sync_up_to_date)
                }
                AtelierGroupedRow(
                    title = stringResource(R.string.profile_item_sync),
                    subtitle = syncSubtitle,
                    icon = Icons.Outlined.Sync,
                    trailingBadgeText = if (state.pendingCount > 0) "${state.pendingCount}" else null,
                    trailingBadgeColor = if (state.pendingCount > 0) atelier.wait.copy(alpha = 0.2f) else null,
                    onClick = onOpenSyncQueue,
                    showDivider = true
                )
                AtelierGroupedRow(
                    title = stringResource(R.string.profile_item_notifications),
                    subtitle = if (state.unreadNotificationsCount > 0) {
                        "${state.unreadNotificationsCount} alertas no leídas"
                    } else {
                        "Sin alertas nuevas"
                    },
                    icon = Icons.Outlined.Notifications,
                    trailingBadgeText = if (state.unreadNotificationsCount > 0) "${state.unreadNotificationsCount}" else null,
                    trailingBadgeColor = if (state.unreadNotificationsCount > 0) scheme.error.copy(alpha = 0.2f) else null,
                    onClick = onOpenNotifications,
                    showDivider = true
                )
                AtelierGroupedRow(
                    title = stringResource(R.string.profile_item_permissions),
                    subtitle = stringResource(R.string.profile_permissions_count, state.permissionsCount),
                    icon = Icons.Outlined.Security,
                    onClick = { showPermissionsDialog = true },
                    showDivider = false
                )
            }

            // 4. Grupo 3: Seguridad y sesión
            AtelierGroupedCard(title = stringResource(R.string.profile_section_session)) {
                AtelierGroupedRow(
                    title = stringResource(R.string.profile_sign_out),
                    subtitle = if (state.pendingCount > 0) {
                        stringResource(R.string.profile_sign_out_message)
                    } else {
                        "Finalizar jornada en este teléfono"
                    },
                    icon = Icons.AutoMirrored.Outlined.Logout,
                    iconContainerColor = scheme.errorContainer.copy(alpha = 0.4f),
                    iconTint = scheme.error,
                    onClick = viewModel::onSignOutClick,
                    showDivider = false
                )
            }

            // 5. Versión de la app
            Text(
                text = stringResource(R.string.profile_version, BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.bodySmall,
                color = atelier.muted,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            )
        }
    }

    if (showPersonalDataDialog) {
        AlertDialog(
            onDismissRequest = { showPersonalDataDialog = false },
            title = { Text(stringResource(R.string.profile_item_personal_data)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ProfileDetailItem(label = "Nombre completo", value = state.fullName)
                    ProfileDetailItem(label = "Correo corporativo", value = state.email)
                    ProfileDetailItem(label = "Rol operativo", value = state.roleTitle)
                    ProfileDetailItem(label = "Taller / Razón social", value = state.tenantName)
                    state.membershipId?.let {
                        ProfileDetailItem(label = "ID de membresía", value = it)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPersonalDataDialog = false }) {
                    Text("Cerrar")
                }
            }
        )
    }

    if (showPlanDialog) {
        AlertDialog(
            onDismissRequest = { showPlanDialog = false },
            title = { Text(stringResource(R.string.profile_item_plan)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ProfileDetailItem(label = "Nivel de suscripción", value = "Plan ${state.planName}")
                    ProfileDetailItem(
                        label = "Telemetría IoT OBD-II",
                        value = if (state.iotTelemetryEnabled) "Habilitado" else "No incluido en plan"
                    )
                    ProfileDetailItem(
                        label = "Diagnóstico predictivo IA",
                        value = if (state.aiDiagnosticsEnabled) "Habilitado" else "No incluido en plan"
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showPlanDialog = false }) {
                    Text("Cerrar")
                }
            }
        )
    }

    if (showPermissionsDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionsDialog = false },
            title = { Text("Permisos asignados (${state.permissionsCount})") },
            text = {
                Column(
                    modifier = Modifier
                        .heightIn(max = 380.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (state.permissions.isEmpty()) {
                        Text(
                            text = "Sin permisos especiales asignados",
                            style = MaterialTheme.typography.bodyMedium,
                            color = atelier.inkSecondary
                        )
                    } else {
                        state.permissions.forEach { perm ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = scheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.CheckCircle,
                                        contentDescription = null,
                                        tint = atelier.ok,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = perm,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                        color = scheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPermissionsDialog = false }) {
                    Text("Cerrar")
                }
            }
        )
    }

    if (state.confirmingSignOut) {
        AlertDialog(
            onDismissRequest = viewModel::onSignOutDismiss,
            title = { Text(stringResource(R.string.profile_sign_out_title)) },
            text = { Text(stringResource(R.string.profile_sign_out_message)) },
            confirmButton = {
                TextButton(onClick = viewModel::onSignOutConfirm) {
                    Text(stringResource(R.string.profile_sign_out), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::onSignOutDismiss) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }
}

@Composable
private fun ProfileDetailItem(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = AtelierTheme.colors.inkSecondary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
