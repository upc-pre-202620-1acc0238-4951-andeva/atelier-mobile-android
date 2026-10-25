package com.andeva.atelier.mobile.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierTheme
import com.andeva.atelier.mobile.core.designsystem.theme.CardShape

enum class BannerKind { Bad, Info, Ok, Wait }

/** Aviso en línea. Los de error se anuncian solos al lector de pantalla. */
@Composable
fun AtelierBanner(
    message: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    kind: BannerKind = BannerKind.Bad,
    title: String? = null
) {
    val colors = AtelierTheme.colors
    val (container, accent) = when (kind) {
        BannerKind.Bad -> colors.badContainer to colors.bad
        BannerKind.Info -> colors.infoContainer to colors.info
        BannerKind.Ok -> colors.okContainer to colors.ok
        BannerKind.Wait -> colors.waitContainer to colors.wait
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(container)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .semantics {
                liveRegion = if (kind == BannerKind.Bad) LiveRegionMode.Assertive else LiveRegionMode.Polite
            },
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(icon, contentDescription = null, tint = accent)
        Text(
            text = buildAnnotatedString {
                if (title != null) withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("$title ") }
                append(message)
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
