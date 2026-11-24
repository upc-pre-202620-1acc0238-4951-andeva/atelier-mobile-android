package com.andeva.atelier.mobile.features.diagnostics.presentation

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.annotation.StringRes
import androidx.core.content.FileProvider
import com.andeva.atelier.mobile.R
import com.andeva.atelier.mobile.core.designsystem.component.ChipKind
import com.andeva.atelier.mobile.core.designsystem.text.UiText
import com.andeva.atelier.mobile.core.designsystem.text.toUiText
import com.andeva.atelier.mobile.core.domain.BusinessTime
import com.andeva.atelier.mobile.core.presentation.DocumentIntents
import com.andeva.atelier.mobile.features.diagnostics.domain.BluetoothDisabledException
import com.andeva.atelier.mobile.features.diagnostics.domain.BluetoothPermissionDeniedException
import com.andeva.atelier.mobile.features.diagnostics.domain.DeviceStatus
import com.andeva.atelier.mobile.features.diagnostics.domain.FaultSeverity
import com.andeva.atelier.mobile.features.diagnostics.domain.InsufficientTelemetryException
import com.andeva.atelier.mobile.features.diagnostics.domain.InvalidDiagnosticsInputException
import com.andeva.atelier.mobile.features.diagnostics.domain.ObdAdapterUnreachableException
import com.andeva.atelier.mobile.features.diagnostics.domain.ObdConnectionLostException
import com.andeva.atelier.mobile.features.diagnostics.domain.ObdLinkState
import com.andeva.atelier.mobile.features.diagnostics.domain.ObdNoResponseException
import com.andeva.atelier.mobile.features.diagnostics.domain.ObdNotConnectedException
import com.andeva.atelier.mobile.features.diagnostics.domain.PredictiveAlertStatus
import com.andeva.atelier.mobile.features.diagnostics.domain.PredictiveAlertType
import java.io.File
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DateTimeFormatterLima = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.forLanguageTag("es-PE"))
private val TimeFormatterLima = DateTimeFormatter.ofPattern("HH:mm:ss", Locale.forLanguageTag("es-PE"))

internal fun Instant.formatLimaDateTime(): String = DateTimeFormatterLima.format(atZone(BusinessTime.ZONE))

internal fun Instant.formatLimaTime(): String = TimeFormatterLima.format(atZone(BusinessTime.ZONE))

internal fun Throwable.toDiagnosticsUiText(): UiText = when (this) {
    is BluetoothPermissionDeniedException -> UiText.Resource(R.string.diag_bt_permission_needed)
    is BluetoothDisabledException -> UiText.Resource(R.string.diag_bt_disabled)
    is ObdAdapterUnreachableException -> UiText.Resource(R.string.diag_err_adapter_unreachable)
    is ObdNoResponseException -> UiText.Resource(R.string.diag_err_ecu_no_response)
    is ObdConnectionLostException -> UiText.Resource(R.string.diag_err_connection_lost)
    is ObdNotConnectedException -> UiText.Resource(R.string.diag_err_not_connected)
    is InsufficientTelemetryException -> UiText.Resource(R.string.diag_report_insufficient_telemetry, message)
    is InvalidDiagnosticsInputException -> when (field) {
        "connectionType" -> UiText.Resource(R.string.diag_device_invalid_connection_type)
        "deviceIdentifier" -> UiText.Resource(R.string.diag_device_identifier_required)
        else -> UiText.Resource(R.string.diag_installation_odometer_invalid)
    }
    else -> toUiText()
}

@StringRes
internal fun ObdLinkState.labelRes(): Int = when (this) {
    ObdLinkState.DISCONNECTED -> R.string.diag_bt_status_disconnected
    ObdLinkState.CONNECTING -> R.string.diag_bt_status_connecting
    ObdLinkState.CONNECTED -> R.string.diag_bt_status_connected
    ObdLinkState.STREAMING -> R.string.diag_bt_status_streaming
    ObdLinkState.RECONNECTING -> R.string.diag_bt_status_reconnecting
    ObdLinkState.LOST -> R.string.diag_bt_status_lost
}

internal fun ObdLinkState.chipKind(): ChipKind = when (this) {
    ObdLinkState.DISCONNECTED -> ChipKind.Idle
    ObdLinkState.CONNECTING, ObdLinkState.RECONNECTING -> ChipKind.Wait
    ObdLinkState.CONNECTED -> ChipKind.Ok
    ObdLinkState.STREAMING -> ChipKind.Info
    ObdLinkState.LOST -> ChipKind.Bad
}

@StringRes
internal fun DeviceStatus.labelRes(): Int = when (this) {
    DeviceStatus.ACTIVE -> R.string.obd_status_active
    DeviceStatus.INACTIVE -> R.string.obd_status_inactive
    DeviceStatus.LOST -> R.string.obd_status_lost
    DeviceStatus.BROKEN -> R.string.obd_status_broken
}

internal fun DeviceStatus.chipKind(): ChipKind = when (this) {
    DeviceStatus.ACTIVE -> ChipKind.Ok
    DeviceStatus.INACTIVE -> ChipKind.Idle
    DeviceStatus.LOST, DeviceStatus.BROKEN -> ChipKind.Bad
}

@StringRes
internal fun FaultSeverity.labelRes(): Int = when (this) {
    FaultSeverity.CRITICAL -> R.string.diag_faults_severity_critical
    FaultSeverity.MEDIUM -> R.string.diag_faults_severity_medium
    FaultSeverity.LOW -> R.string.diag_faults_severity_low
}

internal fun FaultSeverity.chipKind(): ChipKind = when (this) {
    FaultSeverity.CRITICAL -> ChipKind.Bad
    FaultSeverity.MEDIUM -> ChipKind.Warn
    FaultSeverity.LOW -> ChipKind.Idle
}

@StringRes
internal fun PredictiveAlertType?.labelRes(): Int = when (this) {
    PredictiveAlertType.ENGINE_OVERHEATING_RISK -> R.string.diag_alert_type_overheating
    PredictiveAlertType.BATTERY_FAILURE_RISK -> R.string.diag_alert_type_battery
    PredictiveAlertType.CATALYTIC_SYSTEM_DEGRADATION -> R.string.diag_alert_type_catalytic
    PredictiveAlertType.CYLINDER_MISFIRE_HAZARD -> R.string.diag_alert_type_misfire
    PredictiveAlertType.BRAKE_WEAR -> R.string.diag_alert_type_brake
    PredictiveAlertType.OIL_LIFE_DEPLETION -> R.string.diag_alert_type_oil
    PredictiveAlertType.EMISSIONS_FAILURE -> R.string.diag_alert_type_emissions
    null -> R.string.diag_alert_type_generic
}

@StringRes
internal fun PredictiveAlertStatus.labelRes(): Int = when (this) {
    PredictiveAlertStatus.DISPATCHED -> R.string.diag_alert_status_dispatched
    PredictiveAlertStatus.ACKNOWLEDGED -> R.string.diag_alert_status_acknowledged
    PredictiveAlertStatus.RESOLVED -> R.string.diag_alert_status_resolved
    PredictiveAlertStatus.DISMISSED -> R.string.diag_alert_status_dismissed
}

internal fun PredictiveAlertStatus.chipKind(): ChipKind = when (this) {
    PredictiveAlertStatus.DISPATCHED -> ChipKind.Wait
    PredictiveAlertStatus.ACKNOWLEDGED, PredictiveAlertStatus.RESOLVED -> ChipKind.Ok
    PredictiveAlertStatus.DISMISSED -> ChipKind.Idle
}

/** Abre o comparte el PDF descargado en `cacheDir/pdfs` mediante [DocumentIntents]. */
internal fun shareOrOpenPdf(context: Context, pdfFile: File, chooserTitle: String): Boolean =
    DocumentIntents.openPdf(context, pdfFile, chooserTitle)
