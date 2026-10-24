package com.andeva.atelier.mobile.features.workorders.domain

import com.andeva.atelier.mobile.core.domain.Money
import java.math.BigDecimal

/** Servicio del catálogo del taller (`ServiceResource`): da nombre a las tareas y propuestas (H11). */
data class ServiceOption(
    val id: String,
    val name: String,
    val basePrice: Money?,
    val estimatedMinutes: Int?
)

/**
 * Colaborador de la sede para asignar una tarea (`EmployeeProfileResource`). No hay endpoint con nombres (H11):
 * se identifica por [jobTitle]. [membershipId] es el `mechanicId` que espera el backend.
 */
data class MechanicOption(
    val membershipId: String,
    val jobTitle: String?,
    val employmentStatus: String?
)

/** Nueva tarea en la orden (US12 E2, US22 E1): `CreateWorkOrderTaskResource`. Precio sin IGV. */
data class NewWorkOrderTask(
    val serviceId: String,
    val description: String,
    val mechanicId: String? = null,
    val price: BigDecimal? = null
)

/** Cambios de una tarea (US22 E2): `UpdateWorkOrderTaskResource`; lo que va en null no cambia. */
data class WorkOrderTaskChanges(
    val description: String? = null,
    val mechanicId: String? = null,
    val price: BigDecimal? = null
)
