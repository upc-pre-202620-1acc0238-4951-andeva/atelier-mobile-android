package com.andeva.atelier.mobile.features.main.presentation.more

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Garage
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.andeva.atelier.mobile.R
import com.andeva.atelier.mobile.core.designsystem.component.AtelierChip
import com.andeva.atelier.mobile.core.designsystem.component.AtelierTopBar
import com.andeva.atelier.mobile.core.designsystem.component.ChipKind
import com.andeva.atelier.mobile.core.designsystem.icon.bluetooth_searching
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierTheme
import com.andeva.atelier.mobile.core.designsystem.theme.CardShape
import com.andeva.atelier.mobile.core.domain.Permission
import com.andeva.atelier.mobile.core.domain.Session
import kotlinx.coroutines.flow.StateFlow

@Composable
fun MoreHubScreen(
    sessionFlow: StateFlow<Session?>,
    pendingSyncCountFlow: StateFlow<Int>,
    onBack: () -> Unit,
    onSelectBranch: () -> Unit,
    onOpenReception: () -> Unit,
    onOpenBays: () -> Unit,
    onOpenObdInventory: () -> Unit,
    onOpenAttendance: () -> Unit,
    onOpenStockCatalog: () -> Unit,
    onOpenStockAlerts: () -> Unit,
    onOpenPurchaseOrders: () -> Unit,
    onOpenValuation: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenSyncQueue: () -> Unit,
    onOpenProfile: () -> Unit,
    modifier: Modifier = Modifier
) {
    val session by sessionFlow.collectAsStateWithLifecycle()
    val pendingSync by pendingSyncCountFlow.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
    ) {
        AtelierTopBar(title = stringResource(R.string.more_hub_title), onBack = onBack)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.more_hub_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = AtelierTheme.colors.inkSecondary
            )

        SectionGroupTitle(stringResource(R.string.more_section_operations))
        HubItemCard(
            icon = Icons.Outlined.Storefront,
            title = stringResource(R.string.more_item_branch_title),
            subtitle = stringResource(R.string.more_item_branch_sub),
            onClick = onSelectBranch
        )
        if (session?.has(Permission.CUSTOMERS_READ) == true || session?.has(Permission.VEHICLES_READ) == true) {
            HubItemCard(
                icon = Icons.Outlined.DirectionsCar,
                title = stringResource(R.string.more_item_reception_title),
                subtitle = stringResource(R.string.more_item_reception_sub),
                onClick = onOpenReception
            )
        }
        if (session?.has(Permission.BAYS_READ) == true) {
            HubItemCard(
                icon = Icons.Outlined.Garage,
                title = stringResource(R.string.bays_title),
                subtitle = stringResource(R.string.more_item_bays_sub),
                onClick = onOpenBays
            )
        }
        if (session?.has(Permission.OBD_DEVICES_READ) == true || session?.has(Permission.OBD_DEVICES_REGISTER) == true) {
            HubItemCard(
                icon = bluetooth_searching,
                title = stringResource(R.string.main_obd_title),
                subtitle = stringResource(R.string.more_item_obd_sub),
                onClick = onOpenObdInventory
            )
        }

        SectionGroupTitle(stringResource(R.string.more_section_inventory_hr))
        if (session?.has(Permission.PARTS_READ) == true) {
            HubItemCard(
                icon = Icons.Outlined.Inventory2,
                title = stringResource(R.string.more_item_catalog_title),
                subtitle = stringResource(R.string.more_item_catalog_sub),
                onClick = onOpenStockCatalog
            )
        }
        if (session?.has(Permission.INVENTORY_ALERTS_READ) == true || session?.has(Permission.PARTS_READ) == true) {
            HubItemCard(
                icon = Icons.Outlined.Notifications,
                title = stringResource(R.string.more_item_stock_alerts_title),
                subtitle = stringResource(R.string.more_item_stock_alerts_sub),
                onClick = onOpenStockAlerts
            )
        }
        if (session?.has(Permission.PURCHASE_ORDERS_READ) == true) {
            HubItemCard(
                icon = Icons.Outlined.ShoppingCart,
                title = stringResource(R.string.more_item_purchases_title),
                subtitle = stringResource(R.string.more_item_purchases_sub),
                onClick = onOpenPurchaseOrders
            )
        }
        if (session?.has(Permission.VALUATION_READ) == true) {
            HubItemCard(
                icon = Icons.Outlined.AccountBalanceWallet,
                title = stringResource(R.string.more_item_valuation_title),
                subtitle = stringResource(R.string.more_item_valuation_sub),
                onClick = onOpenValuation
            )
        }
        HubItemCard(
            icon = Icons.Outlined.Badge,
            title = stringResource(R.string.admin_attendance_title),
            subtitle = stringResource(R.string.more_item_attendance_sub),
            onClick = onOpenAttendance
        )

        SectionGroupTitle(stringResource(R.string.more_section_system))
        HubItemCard(
            icon = Icons.Outlined.Notifications,
            title = stringResource(R.string.notifications_title),
            subtitle = stringResource(R.string.more_item_notifications_sub),
            onClick = onOpenNotifications
        )
        HubItemCard(
            icon = Icons.Outlined.Sync,
            title = stringResource(R.string.sync_queue_title),
            subtitle = stringResource(R.string.more_item_sync_sub),
            badge = if (pendingSync > 0) "$pendingSync" else null,
            onClick = onOpenSyncQueue
        )
        HubItemCard(
            icon = Icons.Outlined.Person,
            title = stringResource(R.string.main_tab_profile),
            subtitle = session?.email ?: stringResource(R.string.more_item_profile_sub),
            onClick = onOpenProfile
        )
        }
    }
}

@Composable
private fun SectionGroupTitle(text: String) = Text(
    text = text,
    style = MaterialTheme.typography.titleMedium,
    color = MaterialTheme.colorScheme.onBackground,
    modifier = Modifier.padding(top = 4.dp)
)

@Composable
private fun HubItemCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    badge: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(AtelierTheme.colors.card)
            .clickable(onClick = onClick)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = AtelierTheme.colors.inkSecondary
            )
        }
        if (badge != null) {
            AtelierChip(text = badge, kind = ChipKind.Wait)
        }
    }
}
