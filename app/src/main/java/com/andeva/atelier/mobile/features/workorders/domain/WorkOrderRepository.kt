package com.andeva.atelier.mobile.features.workorders.domain

import kotlinx.coroutines.flow.Flow
import java.io.File

/**
 * Órdenes de trabajo (US12–US15, US22, US24, US25, US36, US33 E2, US35 E2). La UI observa la caché `work_orders`
 * (cabeceras de la sede, 5 min); el detalle se lee en línea. Fotos de recepción y propuestas van siempre a la cola
 * (docs/06-offline-sync.md §6); el resto es en línea (06 §2): sin red falla con `IOException` y no se encola.
 * Toda acción deja la cabecera de la orden al día en Room.
 */
interface WorkOrderRepository {
    /** Recarga las órdenes de la sede de la sesión (sin las cerradas). `MissingBranchException` sin sede. */
    suspend fun refreshBranchWorkOrders(): Result<Unit>

    fun observeBranchWorkOrders(): Flow<List<WorkOrder>>

    fun observeWorkOrder(id: String): Flow<WorkOrder?>

    suspend fun getDetail(id: String): Result<WorkOrderDetail>

    /** La orden en borrador que el backend crea tras la llegada de una cita (H10), o null si aún no existe. */
    suspend fun findDraftForVehicle(branchId: String, vehicleId: String): Result<WorkOrder?>

    suspend fun open(workOrder: NewWorkOrder): Result<WorkOrder>

    suspend fun update(id: String, mileageIn: Int?, diagnosticSummary: String?): Result<WorkOrder>

    suspend fun addTask(id: String, task: NewWorkOrderTask): Result<WorkOrderDetail>

    suspend fun updateTask(workOrderId: String, taskId: String, changes: WorkOrderTaskChanges): Result<WorkOrderDetail>

    suspend fun getServiceCatalog(): Result<List<ServiceOption>>

    suspend fun getBranchMechanics(): Result<List<MechanicOption>>

    suspend fun attachIntakePhoto(id: String, photo: File, description: String?): Result<Unit>

    fun observePendingIntakePhotos(id: String): Flow<List<PendingIntakePhoto>>

    /** El `mechanicId` que pide el backend es el membershipId de la sesión. */
    suspend fun submitProposal(
        id: String,
        description: String,
        severity: ProposalSeverity,
        photo: File,
        suggestedServiceId: String?
    ): Result<Unit>

    fun observePendingProposals(id: String): Flow<List<PendingProposal>>

    /** Devuelve el correo al que se envió. */
    suspend fun notifyProposal(workOrderId: String, proposalId: String): Result<String>

    suspend fun approveProposal(workOrderId: String, proposalId: String, approval: ProposalApproval): Result<WorkOrderDetail>

    suspend fun rejectProposal(workOrderId: String, proposalId: String, customerNotes: String): Result<Proposal>

    /** El PDF en `cacheDir/pdfs`; abrirlo es de la UI (docs/04-mobile-integration.md §8). */
    suspend fun downloadQuotationPdf(id: String): Result<File>

    /** Devuelve el correo del cliente al que se envió. */
    suspend fun sendQuotation(id: String): Result<String>

    suspend fun approveQuotation(id: String): Result<WorkOrder>

    suspend fun rejectQuotation(id: String, reason: String): Result<WorkOrder>

    suspend fun getPartMovements(id: String): Result<List<PartMovement>>

    suspend fun getProfitability(id: String): Result<Profitability>
}
