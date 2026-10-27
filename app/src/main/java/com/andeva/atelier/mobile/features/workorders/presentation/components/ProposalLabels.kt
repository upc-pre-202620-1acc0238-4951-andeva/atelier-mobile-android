package com.andeva.atelier.mobile.features.workorders.presentation.components

import com.andeva.atelier.mobile.R
import com.andeva.atelier.mobile.core.designsystem.component.ChipKind
import com.andeva.atelier.mobile.features.workorders.domain.ProposalSeverity
import com.andeva.atelier.mobile.features.workorders.domain.ProposalStatus

/** Severidades que se pueden enviar: el backend rechaza `HIGH` con 400 (docs/api/README.md §8). */
val SubmittableSeverities: List<ProposalSeverity> = ProposalSeverity.entries.filter { it.isSubmittable }

fun ProposalSeverity.labelRes(): Int = when (this) {
    ProposalSeverity.LOW -> R.string.severity_low
    ProposalSeverity.MEDIUM -> R.string.severity_medium
    ProposalSeverity.HIGH -> R.string.severity_high
    ProposalSeverity.CRITICAL -> R.string.severity_critical
}

fun ProposalSeverity.chipKind(): ChipKind = when (this) {
    ProposalSeverity.LOW -> ChipKind.Info
    ProposalSeverity.MEDIUM -> ChipKind.Wait
    ProposalSeverity.HIGH, ProposalSeverity.CRITICAL -> ChipKind.Bad
}

fun ProposalStatus.labelRes(): Int = when (this) {
    ProposalStatus.PENDING_REVIEW -> R.string.proposal_status_pending
    ProposalStatus.APPROVED -> R.string.proposal_status_approved
    ProposalStatus.REJECTED -> R.string.proposal_status_rejected
}

fun ProposalStatus.chipKind(): ChipKind = when (this) {
    ProposalStatus.PENDING_REVIEW -> ChipKind.Wait
    ProposalStatus.APPROVED -> ChipKind.Ok
    ProposalStatus.REJECTED -> ChipKind.Idle
}
