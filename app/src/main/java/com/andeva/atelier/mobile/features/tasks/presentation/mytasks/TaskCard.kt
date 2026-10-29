package com.andeva.atelier.mobile.features.tasks.presentation.mytasks

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.andeva.atelier.mobile.R
import com.andeva.atelier.mobile.core.designsystem.component.AtelierChip
import com.andeva.atelier.mobile.core.designsystem.component.ChipKind
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierTheme
import com.andeva.atelier.mobile.core.designsystem.theme.CardShape
import com.andeva.atelier.mobile.features.tasks.domain.Task
import com.andeva.atelier.mobile.features.tasks.presentation.taskStatusChipKind
import com.andeva.atelier.mobile.features.tasks.presentation.taskStatusLabelRes

/** Tarjeta de una faena: placa, bahía, orden, horas estimadas y descripción. Toda ella es el área táctil (guantes). */
@Composable
internal fun TaskCard(task: Task, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(AtelierTheme.colors.card)
            .clickable(onClick = onClick)
            .heightIn(min = 72.dp)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AtelierChip(
                text = stringResource(taskStatusLabelRes(task.status, task.holdReason)),
                kind = taskStatusChipKind(task.status, task.holdReason)
            )
            if (task.hasPendingSuspension) {
                AtelierChip(text = stringResource(R.string.task_suspension_pending), kind = ChipKind.Wait)
            }
        }
        Text(
            text = task.plate?.let { stringResource(R.string.task_card_plate, it) }
                ?: stringResource(R.string.task_card_no_plate),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = task.description,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            task.bayName?.let { Meta(stringResource(R.string.task_card_bay, it)) }
            task.workOrderNumber?.let { Meta(stringResource(R.string.task_card_work_order, it)) }
            task.estimatedHours?.let {
                Meta(stringResource(R.string.task_card_estimated, it.stripTrailingZeros().toPlainString()))
            }
        }
    }
}

@Composable
private fun Meta(text: String) = Text(
    text = text,
    style = MaterialTheme.typography.bodyMedium,
    color = AtelierTheme.colors.inkSecondary
)
