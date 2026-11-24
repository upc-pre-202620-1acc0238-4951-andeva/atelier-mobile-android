package com.andeva.atelier.mobile.features.reception.presentation.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.andeva.atelier.mobile.R
import com.andeva.atelier.mobile.core.designsystem.component.AtelierBanner
import com.andeva.atelier.mobile.core.designsystem.component.AtelierBottomSheet
import com.andeva.atelier.mobile.core.designsystem.component.AtelierButton
import com.andeva.atelier.mobile.core.designsystem.component.AtelierButtonStyle
import com.andeva.atelier.mobile.core.designsystem.component.AtelierListRow
import com.andeva.atelier.mobile.core.designsystem.component.AtelierPlate
import com.andeva.atelier.mobile.core.designsystem.component.AtelierRequiresConnection
import com.andeva.atelier.mobile.core.designsystem.component.AtelierSelectionCard
import com.andeva.atelier.mobile.core.designsystem.component.AtelierTextField
import com.andeva.atelier.mobile.core.designsystem.component.PlateSize
import com.andeva.atelier.mobile.core.designsystem.component.bone
import com.andeva.atelier.mobile.core.designsystem.text.UiText
import com.andeva.atelier.mobile.core.designsystem.theme.AtelierTheme
import com.andeva.atelier.mobile.core.designsystem.theme.ChipShape
import com.andeva.atelier.mobile.core.designsystem.theme.Spacing
import com.andeva.atelier.mobile.core.domain.BusinessTime
import com.andeva.atelier.mobile.features.reception.domain.Appointment
import com.andeva.atelier.mobile.features.reception.domain.AppointmentStatus
import com.andeva.atelier.mobile.features.reception.domain.Customer
import java.time.format.DateTimeFormatter

private val SheetTimeFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm").withZone(BusinessTime.ZONE)
private val SheetDateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(BusinessTime.ZONE)

private enum class AppointmentSheetMode { ACTIONS, RESCHEDULE, CANCEL }

/**
 * Hoja inferior de gestión de cita (US11 + agenda completa):
 * Confirmar (`PENDING`), Registrar llegada (`H10`), Reprogramar (fecha/hora) y Cancelar (motivo).
 */
@Composable
internal fun AppointmentActionsSheet(
    appointment: Appointment,
    state: VehicleSearchUiState,
    onConfirmAppointment: () -> Unit,
    onMarkArrived: () -> Unit,
    onReschedule: (dateText: String, timeText: String) -> Unit,
    onCancelAppointment: (reason: String) -> Unit,
    onDismiss: () -> Unit
) {
    var mode by remember(appointment.id) { mutableStateOf(AppointmentSheetMode.ACTIONS) }
    var rescheduleDate by remember(appointment.id) { mutableStateOf(SheetDateFormat.format(appointment.scheduledAt)) }
    var rescheduleTime by remember(appointment.id) { mutableStateOf(SheetTimeFormat.format(appointment.scheduledAt)) }
    var cancelReason by remember(appointment.id) { mutableStateOf("") }

    val busy = state.arrivingId != null || state.updatingAppointmentId != null
    val isPending = appointment.status == AppointmentStatus.PENDING
    val canMutate = state.canMarkArrival && state.isOnline && !busy

    AtelierBottomSheet(
        title = stringResource(
            when (mode) {
                AppointmentSheetMode.ACTIONS -> R.string.reception_appointment_actions_title
                AppointmentSheetMode.RESCHEDULE -> R.string.reception_appointment_reschedule_btn
                AppointmentSheetMode.CANCEL -> R.string.reception_appointment_cancel_btn
            }
        ),
        onDismiss = { if (!busy) onDismiss() },
        actions = {
            if (!state.isOnline) AtelierRequiresConnection()
            when (mode) {
                AppointmentSheetMode.ACTIONS -> {
                    if (isPending) {
                        AtelierButton(
                            text = stringResource(R.string.reception_appointment_confirm_btn),
                            onClick = onConfirmAppointment,
                            modifier = Modifier.fillMaxWidth(),
                            style = AtelierButtonStyle.Tonal,
                            enabled = canMutate,
                            loading = state.updatingAppointmentId == appointment.id,
                            icon = Icons.Outlined.CheckCircle
                        )
                    }
                    AtelierButton(
                        text = stringResource(R.string.reception_arrive_confirm),
                        onClick = onMarkArrived,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = canMutate,
                        loading = state.arrivingId == appointment.id
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                    ) {
                        AtelierButton(
                            text = stringResource(R.string.reception_appointment_reschedule_btn),
                            onClick = { mode = AppointmentSheetMode.RESCHEDULE },
                            modifier = Modifier.weight(1f),
                            style = AtelierButtonStyle.Outlined,
                            enabled = canMutate,
                            icon = Icons.Outlined.Schedule
                        )
                        AtelierButton(
                            text = stringResource(R.string.reception_appointment_cancel_btn),
                            onClick = { mode = AppointmentSheetMode.CANCEL },
                            modifier = Modifier.weight(1f),
                            style = AtelierButtonStyle.Destructive,
                            enabled = canMutate
                        )
                    }
                    AtelierButton(
                        text = stringResource(R.string.common_cancel),
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth(),
                        style = AtelierButtonStyle.Text,
                        enabled = !busy
                    )
                }
                AppointmentSheetMode.RESCHEDULE -> {
                    AtelierButton(
                        text = stringResource(R.string.reception_appointment_reschedule_confirm),
                        onClick = { onReschedule(rescheduleDate, rescheduleTime) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = canMutate && rescheduleDate.isNotBlank() && rescheduleTime.isNotBlank(),
                        loading = state.updatingAppointmentId == appointment.id
                    )
                    AtelierButton(
                        text = stringResource(R.string.common_back),
                        onClick = { mode = AppointmentSheetMode.ACTIONS },
                        modifier = Modifier.fillMaxWidth(),
                        style = AtelierButtonStyle.Text,
                        enabled = !busy
                    )
                }
                AppointmentSheetMode.CANCEL -> {
                    AtelierButton(
                        text = stringResource(R.string.reception_appointment_cancel_confirm),
                        onClick = { onCancelAppointment(cancelReason) },
                        modifier = Modifier.fillMaxWidth(),
                        style = AtelierButtonStyle.DestructiveFilled,
                        enabled = canMutate && cancelReason.isNotBlank(),
                        loading = state.updatingAppointmentId == appointment.id
                    )
                    AtelierButton(
                        text = stringResource(R.string.common_back),
                        onClick = { mode = AppointmentSheetMode.ACTIONS },
                        modifier = Modifier.fillMaxWidth(),
                        style = AtelierButtonStyle.Text,
                        enabled = !busy
                    )
                }
            }
        }
    ) {
        appointment.plate?.let { AtelierPlate(it, size = PlateSize.L) }
        Text(
            text = appointment.customerName ?: stringResource(R.string.reception_appointment_no_customer),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = listOfNotNull(
                stringResource(R.string.reception_arrive_time, SheetTimeFormat.format(appointment.scheduledAt)),
                appointment.reason
            ).joinToString(" · "),
            style = MaterialTheme.typography.bodyMedium,
            color = AtelierTheme.colors.inkSecondary
        )

        when (mode) {
            AppointmentSheetMode.ACTIONS -> Unit
            AppointmentSheetMode.RESCHEDULE -> {
                DateTimeQuickPicker(
                    dateText = rescheduleDate,
                    timeText = rescheduleTime,
                    enabled = !busy,
                    isError = false,
                    onDateChange = { rescheduleDate = it },
                    onTimeChange = { rescheduleTime = it }
                )
            }
            AppointmentSheetMode.CANCEL -> {
                AtelierTextField(
                    label = stringResource(R.string.reception_appointment_cancel_reason_label),
                    value = cancelReason,
                    onValueChange = { cancelReason = it },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !busy,
                    placeholder = stringResource(R.string.reception_appointment_cancel_reason_hint),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
                )
            }
        }

        state.error?.let {
            AtelierBanner(message = it.asString(), icon = Icons.Outlined.ErrorOutline)
        }
    }
}

/** Hoja inferior para agendar una nueva cita (`POST /appointments`, `APPOINTMENTS_CREATE`). */
@Composable
internal fun CreateAppointmentSheet(
    form: CreateAppointmentForm,
    customers: List<Customer>,
    isOnline: Boolean,
    error: UiText?,
    onCustomerQueryChange: (String) -> Unit,
    onSelectCustomer: (Customer) -> Unit,
    onClearCustomer: () -> Unit,
    onSelectVehicle: (String) -> Unit,
    onQuickDate: (Long) -> Unit,
    onDateChange: (String) -> Unit,
    onTimeChange: (String) -> Unit,
    onDurationChange: (String) -> Unit,
    onReasonChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val filteredCustomers = remember(form.customerQuery, customers) {
        val q = form.customerQuery.trim().lowercase()
        val list = if (q.isEmpty()) customers else customers.filter {
            it.displayName.lowercase().contains(q) || it.taxId?.lowercase()?.contains(q) == true
        }
        list.take(5)
    }

    AtelierBottomSheet(
        title = stringResource(R.string.reception_create_appointment_title),
        onDismiss = { if (!form.isSaving) onDismiss() },
        actions = {
            if (!isOnline) AtelierRequiresConnection()
            AtelierButton(
                text = stringResource(R.string.reception_appointment_schedule_btn),
                onClick = onConfirm,
                modifier = Modifier.fillMaxWidth(),
                enabled = isOnline && !form.isSaving &&
                    form.selectedCustomer != null && form.selectedVehicleId != null &&
                    form.dateText.isNotBlank() && form.timeText.isNotBlank(),
                loading = form.isSaving
            )
            AtelierButton(
                text = stringResource(R.string.common_cancel),
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                style = AtelierButtonStyle.Text,
                enabled = !form.isSaving
            )
        }
    ) {
        Text(
            text = stringResource(R.string.reception_appointment_customer_label),
            style = MaterialTheme.typography.titleSmall,
            color = AtelierTheme.colors.inkSecondary
        )
        if (form.selectedCustomer == null) {
            AtelierTextField(
                label = stringResource(R.string.reception_appointment_select_customer),
                value = form.customerQuery,
                onValueChange = onCustomerQueryChange,
                modifier = Modifier.fillMaxWidth(),
                enabled = !form.isSaving,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
            )
            filteredCustomers.forEach { customer ->
                AtelierSelectionCard(
                    title = customer.displayName,
                    subtitle = customer.taxId,
                    selected = false,
                    enabled = !form.isSaving,
                    onClick = { onSelectCustomer(customer) }
                )
            }
        } else {
            AtelierSelectionCard(
                title = form.selectedCustomer.displayName,
                subtitle = form.selectedCustomer.taxId,
                selected = true,
                enabled = !form.isSaving,
                onClick = onClearCustomer
            )
            Text(
                text = stringResource(R.string.reception_appointment_vehicle_label),
                style = MaterialTheme.typography.titleSmall,
                color = AtelierTheme.colors.inkSecondary
            )
            when {
                form.isLoadingVehicles -> Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    AtelierListRow(title = "Vehículo", subtitle = "Cargando", plate = "BKL482", loading = true)
                }
                form.vehicles.isEmpty() -> Text(
                    text = stringResource(R.string.reception_appointment_no_vehicles),
                    style = MaterialTheme.typography.bodyMedium,
                    color = AtelierTheme.colors.inkSecondary
                )
                else -> form.vehicles.forEach { vehicle ->
                    AtelierSelectionCard(
                        title = listOfNotNull(vehicle.plate, vehicle.brand, vehicle.model).joinToString(" · "),
                        subtitle = vehicle.year?.toString(),
                        selected = vehicle.id == form.selectedVehicleId,
                        enabled = !form.isSaving,
                        onClick = { onSelectVehicle(vehicle.id) }
                    )
                }
            }
        }

        DateTimeQuickPicker(
            dateText = form.dateText,
            timeText = form.timeText,
            enabled = !form.isSaving,
            isError = form.invalidField == "scheduledAt",
            onDateChange = onDateChange,
            onTimeChange = onTimeChange,
            onQuickDate = onQuickDate
        )

        AtelierTextField(
            label = stringResource(R.string.reception_appointment_duration_label),
            value = form.durationText,
            onValueChange = onDurationChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = !form.isSaving,
            isError = form.invalidField == "estimatedDurationMinutes",
            errorText = if (form.invalidField == "estimatedDurationMinutes") {
                stringResource(R.string.reception_appointment_invalid_duration)
            } else null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next)
        )

        AtelierTextField(
            label = stringResource(R.string.reception_appointment_reason_label),
            value = form.reason,
            onValueChange = onReasonChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = !form.isSaving,
            placeholder = stringResource(R.string.reception_appointment_reason_hint),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
        )

        error?.let { AtelierBanner(message = it.asString(), icon = Icons.Outlined.ErrorOutline) }
    }
}

@Composable
private fun DateTimeQuickPicker(
    dateText: String,
    timeText: String,
    enabled: Boolean,
    isError: Boolean,
    onDateChange: (String) -> Unit,
    onTimeChange: (String) -> Unit,
    onQuickDate: ((Long) -> Unit)? = null
) {
    if (onQuickDate != null) {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            FilterChip(
                selected = false,
                onClick = { onQuickDate(0) },
                enabled = enabled,
                shape = ChipShape,
                label = { Text(stringResource(R.string.reception_appointment_today_chip)) },
                leadingIcon = { Icon(Icons.Outlined.Check, contentDescription = null) }
            )
            FilterChip(
                selected = false,
                onClick = { onQuickDate(1) },
                enabled = enabled,
                shape = ChipShape,
                label = { Text(stringResource(R.string.reception_appointment_tomorrow_chip)) }
            )
        }
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        AtelierTextField(
            label = stringResource(R.string.reception_appointment_date_label),
            value = dateText,
            onValueChange = onDateChange,
            modifier = Modifier.weight(1.2f),
            placeholder = "2026-11-23",
            isError = isError,
            enabled = enabled,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii, imeAction = ImeAction.Next)
        )
        AtelierTextField(
            label = stringResource(R.string.reception_appointment_time_label),
            value = timeText,
            onValueChange = onTimeChange,
            modifier = Modifier.weight(0.8f),
            placeholder = "10:00",
            isError = isError,
            enabled = enabled,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii, imeAction = ImeAction.Next)
        )
    }
}

/** Hoja inferior para editar correo y teléfono del cliente (`PUT /customers/{id}/contact`). */
@Composable
internal fun EditCustomerContactSheet(
    form: EditContactForm,
    isOnline: Boolean,
    error: UiText?,
    onEmailChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AtelierBottomSheet(
        title = stringResource(R.string.reception_edit_contact_title),
        onDismiss = { if (!form.isSaving) onDismiss() },
        actions = {
            if (!isOnline) AtelierRequiresConnection()
            AtelierButton(
                text = stringResource(R.string.reception_edit_contact_save),
                onClick = onConfirm,
                modifier = Modifier.fillMaxWidth(),
                enabled = isOnline && !form.isSaving,
                loading = form.isSaving
            )
            AtelierButton(
                text = stringResource(R.string.common_cancel),
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                style = AtelierButtonStyle.Text,
                enabled = !form.isSaving
            )
        }
    ) {
        Text(
            text = form.customerName,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        AtelierTextField(
            label = stringResource(R.string.reception_edit_contact_email_label),
            value = form.email,
            onValueChange = onEmailChange,
            modifier = Modifier
                .fillMaxWidth()
                .bone(loading = false),
            leadingIcon = Icons.Outlined.Email,
            enabled = !form.isSaving,
            isError = form.invalidField == "email",
            errorText = if (form.invalidField == "email") stringResource(R.string.reception_field_email) else null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next)
        )
        AtelierTextField(
            label = stringResource(R.string.reception_edit_contact_phone_label),
            value = form.phone,
            onValueChange = onPhoneChange,
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = Icons.Outlined.Phone,
            enabled = !form.isSaving,
            isError = form.invalidField == "phone",
            errorText = if (form.invalidField == "phone") stringResource(R.string.reception_field_phone) else null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Done)
        )
        error?.let { AtelierBanner(message = it.asString(), icon = Icons.Outlined.ErrorOutline) }
    }
}
