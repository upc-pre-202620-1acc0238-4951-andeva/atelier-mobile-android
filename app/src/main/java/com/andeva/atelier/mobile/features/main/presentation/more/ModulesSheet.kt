package com.andeva.atelier.mobile.features.main.presentation.more

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Garage
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.andeva.atelier.mobile.R
import com.andeva.atelier.mobile.core.designsystem.component.AtelierBottomSheet
import com.andeva.atelier.mobile.core.designsystem.component.AtelierGroupedCard
import com.andeva.atelier.mobile.core.designsystem.component.AtelierGroupedRow
import com.andeva.atelier.mobile.core.designsystem.component.pressScale
import com.andeva.atelier.mobile.core.designsystem.icon.bluetooth_searching
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierTheme
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierWorkshopTheme
import com.andeva.atelier.mobile.core.designsystem.theme.ControlShape
import com.andeva.atelier.mobile.core.domain.Permission
import com.andeva.atelier.mobile.core.domain.Session
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Adónde puede ir quien abre "Módulos". El grafo de navegación decide la ruta de cada uno. */
enum class MoreDestination {
    BRANCH, RECEPTION, BAYS, DAILY_CASH, FISCAL_SERIES, REPORTS,
    PARTS, STOCK_ALERTS, PURCHASE_ORDERS, VALUATION, OBD, NOTIFICATIONS, SYNC_QUEUE, PROFILE
}

private const val TILE_COLUMNS = 4

/**
 * Hoja de módulos, la misma de quina-webapp: sube desde abajo, se arrastra para cerrar y muestra baldosas de cuatro
 * columnas agrupadas por tarea. Cada baldosa aparece solo si la sesión tiene el permiso del endpoint que la alimenta
 * (docs/02), nunca por rol; un grupo sin baldosas no se dibuja. Al pie, la cuenta.
 */
@Composable
fun ModulesSheet(
    sessionFlow: StateFlow<Session?>,
    pendingSyncCountFlow: StateFlow<Int>,
    onDismiss: () -> Unit,
    onOpen: (MoreDestination) -> Unit
) {
    val session by sessionFlow.collectAsStateWithLifecycle()
    val pendingSync by pendingSyncCountFlow.collectAsStateWithLifecycle()
    ModulesSheetContent(session = session, pendingSync = pendingSync, onDismiss = onDismiss, onOpen = onOpen)
}

@Composable
private fun ModulesSheetContent(
    session: Session?,
    pendingSync: Int,
    onDismiss: () -> Unit,
    onOpen: (MoreDestination) -> Unit
) {
    val rows = session?.let(::moreRows).orEmpty()
    AtelierBottomSheet(title = stringResource(R.string.modules_title), onDismiss = onDismiss) {
        ModulesBody(rows = rows, email = session?.email, pendingSync = pendingSync, onOpen = onOpen)
    }
}

@Composable
private fun ModulesBody(rows: List<MoreRow>, email: String?, pendingSync: Int, onOpen: (MoreDestination) -> Unit) {
    MoreGroup.entries.filter { it != MoreGroup.ACCOUNT }.forEach { group ->
        val items = rows.filter { it.group == group }
        if (items.isEmpty()) return@forEach
        Text(
            text = stringResource(group.title),
            style = MaterialTheme.typography.labelLarge,
            color = AtelierTheme.colors.inkSecondary,
            modifier = Modifier.padding(top = 4.dp)
        )
        items.chunked(TILE_COLUMNS).forEach { chunk ->
            Row(
                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                chunk.forEach { row ->
                    ModuleTile(row, onClick = { onOpen(row.destination) }, modifier = Modifier.weight(1f).fillMaxHeight())
                }
                repeat(TILE_COLUMNS - chunk.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
    val account = rows.filter { it.group == MoreGroup.ACCOUNT }
    if (account.isNotEmpty()) {
        AtelierGroupedCard(title = stringResource(MoreGroup.ACCOUNT.title), modifier = Modifier.padding(top = 4.dp)) {
            account.forEachIndexed { index, row ->
                AtelierGroupedRow(
                    title = stringResource(row.title),
                    subtitle = if (row.destination == MoreDestination.PROFILE) {
                        email ?: stringResource(row.subtitle)
                    } else {
                        stringResource(row.subtitle)
                    },
                    icon = row.icon,
                    trailingBadgeText = if (row.destination == MoreDestination.SYNC_QUEUE && pendingSync > 0) {
                        pendingSync.toString()
                    } else null,
                    showDivider = index < account.lastIndex,
                    onClick = { onOpen(row.destination) }
                )
            }
        }
    }
}

@Composable
private fun ModuleTile(row: MoreRow, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    Column(
        modifier = modifier
            .heightIn(min = 80.dp)
            .pressScale(interaction)
            .clip(ControlShape)
            .background(MaterialTheme.colorScheme.background)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, ControlShape)
            .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically)
    ) {
        Icon(row.icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(24.dp))
        Text(
            text = stringResource(row.title),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private enum class MoreGroup(val title: Int) {
    WORKSHOP(R.string.more_h_group_workshop),
    CASHIER(R.string.more_h_group_cashier),
    STOCK(R.string.more_h_group_stock),
    ACCOUNT(R.string.more_h_group_account)
}

private data class MoreRow(
    val group: MoreGroup,
    val destination: MoreDestination,
    val icon: ImageVector,
    val title: Int,
    val subtitle: Int
)

/** Filas que la sesión puede abrir, en el orden en que se dibujan. */
private fun moreRows(s: Session): List<MoreRow> = buildList {
    fun add(group: MoreGroup, destination: MoreDestination, icon: ImageVector, title: Int, sub: Int) =
        add(MoreRow(group, destination, icon, title, sub))

    if (s.has(Permission.BRANCHES_READ)) {
        add(MoreGroup.WORKSHOP, MoreDestination.BRANCH, Icons.Outlined.Storefront, R.string.more_h_branch, R.string.more_h_branch_sub)
    }
    if (s.has(Permission.CUSTOMERS_READ) || s.has(Permission.VEHICLES_READ) || s.has(Permission.WORK_ORDERS_CREATE)) {
        add(MoreGroup.WORKSHOP, MoreDestination.RECEPTION, Icons.Outlined.DirectionsCar, R.string.more_h_reception, R.string.more_h_reception_sub)
    }
    if (s.has(Permission.BAYS_READ)) {
        add(MoreGroup.WORKSHOP, MoreDestination.BAYS, Icons.Outlined.Garage, R.string.more_h_bays, R.string.more_h_bays_sub)
    }
    if (s.has(Permission.DEVICES_READ) || s.has(Permission.DEVICES_REGISTER)) {
        add(MoreGroup.WORKSHOP, MoreDestination.OBD, bluetooth_searching, R.string.more_h_obd, R.string.more_h_obd_sub)
    }
    if (s.has(Permission.INVOICES_READ)) {
        add(MoreGroup.CASHIER, MoreDestination.DAILY_CASH, Icons.Outlined.Payments, R.string.more_h_daily_cash, R.string.more_h_daily_cash_sub)
    }
    if (s.has(Permission.CASHFLOW_EXPORT_PDF) || s.canReadAiReports()) {
        add(MoreGroup.CASHIER, MoreDestination.REPORTS, Icons.Outlined.Assessment, R.string.more_h_reports, R.string.more_h_reports_sub)
    }
    if (s.has(Permission.FISCAL_CONFIG_MANAGE)) {
        add(MoreGroup.CASHIER, MoreDestination.FISCAL_SERIES, Icons.Outlined.ReceiptLong, R.string.more_h_series, R.string.more_h_series_sub)
    }
    if (s.has(Permission.PARTS_READ)) {
        add(MoreGroup.STOCK, MoreDestination.PARTS, Icons.Outlined.Inventory2, R.string.more_h_parts, R.string.more_h_parts_sub)
    }
    if (s.has(Permission.ALERTS_READ)) {
        add(MoreGroup.STOCK, MoreDestination.STOCK_ALERTS, Icons.Outlined.WarningAmber, R.string.more_h_alerts, R.string.more_h_alerts_sub)
    }
    if (s.has(Permission.PURCHASE_ORDERS_READ)) {
        add(MoreGroup.STOCK, MoreDestination.PURCHASE_ORDERS, Icons.Outlined.ShoppingCart, R.string.more_h_purchases, R.string.more_h_purchases_sub)
    }
    // El endpoint de valorización exige `inventory:batches:read` (docs/02); no existe un permiso de valorización.
    if (s.has(Permission.BATCHES_READ)) {
        add(MoreGroup.STOCK, MoreDestination.VALUATION, Icons.Outlined.AccountBalanceWallet, R.string.more_h_valuation, R.string.more_h_valuation_sub)
    }
    add(MoreGroup.ACCOUNT, MoreDestination.NOTIFICATIONS, Icons.Outlined.Notifications, R.string.more_h_notifications, R.string.more_h_notifications_sub)
    add(MoreGroup.ACCOUNT, MoreDestination.SYNC_QUEUE, Icons.Outlined.Sync, R.string.more_h_sync, R.string.more_h_sync_sub)
    add(MoreGroup.ACCOUNT, MoreDestination.PROFILE, Icons.Outlined.Person, R.string.more_h_profile, R.string.more_h_profile_sub)
}

// region Previews

private fun previewSession(permissions: Set<String>) = Session(
    userId = "u1", email = "pedro@tallersuarez.pe", fullName = "Pedro Suárez", tenantId = "t1",
    tenantName = "Taller Suárez Automotriz", membershipId = "m1", permissions = permissions, plan = null, branchId = "b1"
)

private val previewPermissions = setOf(
    "iam:branches:read", "crm:customers:read", "operations:bays:read", "invoicing:invoices:read",
    "invoicing:cashflow:export_pdf", "invoicing:fiscal_config:manage", "inventory:parts:read",
    "inventory:alerts:read", "inventory:purchase_orders:read", "inventory:batches:read", "iot:devices:read"
)

@Composable
private fun PreviewBody() {
    val session = previewSession(previewPermissions)
    Column(
        modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainer).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ModulesBody(rows = moreRows(session), email = session.email, pendingSync = 2, onOpen = {})
    }
}

@Preview(name = "Módulos · claro", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun ModulesPreviewLight() = AtelierWorkshopTheme(darkTheme = false) { PreviewBody() }

@Preview(
    name = "Módulos · oscuro", widthDp = 390, heightDp = 844, showBackground = true,
    backgroundColor = 0xFF262626, uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun ModulesPreviewDark() = AtelierWorkshopTheme(darkTheme = true) { PreviewBody() }

// endregion
