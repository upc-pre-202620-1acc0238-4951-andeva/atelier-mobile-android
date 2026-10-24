package com.andeva.atelier.mobile.features.workorders.domain

import java.math.BigDecimal
import java.time.Instant

/**
 * `ProposalSeverity` de docs/api/README.md §8. El enum del backend tiene `HIGH`, pero la propuesta solo se puede
 * enviar con `LOW`, `MEDIUM` o `CRITICAL` ([isSubmittable]); `HIGH` solo puede llegar al leer.
 */
enum class ProposalSeverity {
    LOW, MEDIUM, HIGH, CRITICAL;

    val isSubmittable: Boolean get() = this != HIGH
}

/** `ProposalStatus` de docs/api/README.md §8. */
enum class ProposalStatus { PENDING_REVIEW, APPROVED, REJECTED }

/** Propuesta de tarea adicional por hallazgo en foso (`TaskProposalResource`, US14–US15). */
data class Proposal(
    val id: String,
    val workOrderId: String,
    val taskId: String?,
    val serviceId: String?,
    val mechanicId: String?,
    val description: String,
    val severity: ProposalSeverity?,
    val imageUrl: String?,
    val status: ProposalStatus?,
    val customerNotes: String?,
    val createdAt: Instant?
)

/** Aprobación del cliente (US15 E1): `ApproveTaskProposalResource`; crea la tarea. [mechanicId] = membershipId. */
data class ProposalApproval(
    val serviceId: String,
    val finalPrice: BigDecimal,
    val laborHours: BigDecimal,
    val mechanicId: String? = null,
    val notes: String? = null
)
