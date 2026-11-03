package com.andeva.atelier.mobile.features.main.presentation.profile

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.andeva.atelier.mobile.BuildConfig
import com.andeva.atelier.mobile.R
import com.andeva.atelier.mobile.core.designsystem.component.AtelierButton
import com.andeva.atelier.mobile.core.designsystem.component.AtelierButtonStyle
import com.andeva.atelier.mobile.core.designsystem.component.rise
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierTheme

/** K1: quién tiene la sesión y cómo cerrarla. Apariencia e idioma llegan con sus propias historias. */
@Composable
fun ProfileScreen(viewModel: ProfileViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val atelier = AtelierTheme.colors

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Row(
            modifier = Modifier.rise(0),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = state.initials,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = state.fullName, style = MaterialTheme.typography.titleLarge)
                Text(
                    text = state.email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = atelier.inkSecondary
                )
                if (state.tenantName.isNotBlank()) {
                    Text(
                        text = state.tenantName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = atelier.muted
                    )
                }
            }
        }

        if (state.pendingCount > 0) {
            Text(
                text = pluralStringResource(R.plurals.profile_pending_count, state.pendingCount, state.pendingCount),
                style = MaterialTheme.typography.bodyMedium,
                color = atelier.wait,
                modifier = Modifier.fillMaxWidth().rise(1)
            )
        }

        AtelierButton(
            text = stringResource(R.string.profile_sign_out),
            onClick = viewModel::onSignOutClick,
            style = AtelierButtonStyle.Outlined,
            loading = state.signingOut,
            icon = Icons.AutoMirrored.Outlined.Logout,
            modifier = Modifier.fillMaxWidth().rise(2)
        )

        Text(
            text = stringResource(R.string.profile_version, BuildConfig.VERSION_NAME),
            style = MaterialTheme.typography.labelMedium,
            color = atelier.muted,
            modifier = Modifier.fillMaxWidth()
        )
    }

    if (state.confirmingSignOut) {
        AlertDialog(
            onDismissRequest = viewModel::onSignOutDismiss,
            title = { Text(stringResource(R.string.profile_sign_out_title)) },
            text = { Text(stringResource(R.string.profile_sign_out_message)) },
            dismissButton = {
                TextButton(onClick = viewModel::onSignOutDismiss) { Text(stringResource(R.string.common_cancel)) }
            },
            confirmButton = {
                TextButton(onClick = viewModel::onSignOutConfirm) { Text(stringResource(R.string.profile_sign_out)) }
            }
        )
    }
}
