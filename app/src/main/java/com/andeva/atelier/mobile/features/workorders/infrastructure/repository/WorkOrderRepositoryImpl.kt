package com.andeva.atelier.mobile.features.workorders.infrastructure.repository

import com.andeva.atelier.mobile.core.data.local.SessionManager
import com.andeva.atelier.mobile.core.data.remote.PdfDownloader
import com.andeva.atelier.mobile.core.data.remote.apiCall
import com.andeva.atelier.mobile.core.data.remote.employee.EmployeeService
import com.andeva.atelier.mobile.core.data.sync.CacheMetadataDao
import com.andeva.atelier.mobile.core.data.sync.CacheMetadataEntity
import com.andeva.atelier.mobile.core.data.sync.PendingOperationDao
import com.andeva.atelier.mobile.core.data.sync.PendingOperationEntity
import com.andeva.atelier.mobile.core.data.sync.PendingUploadDao
import com.andeva.atelier.mobile.core.domain.MissingBranchException
import com.andeva.atelier.mobile.core.domain.Permission
import com.andeva.atelier.mobile.core.media.PhotoCapture
import com.andeva.atelier.mobile.core.media.StoragePaths
import com.andeva.atelier.mobile.features.workorders.domain.MechanicOption
import com.andeva.atelier.mobile.features.workorders.domain.NewWorkOrder
import com.andeva.atelier.mobile.features.workorders.domain.NewWorkOrderTask
import com.andeva.atelier.mobile.features.workorders.domain.PartMovement
import com.andeva.atelier.mobile.features.workorders.domain.PendingIntakePhoto
import com.andeva.atelier.mobile.features.workorders.domain.PendingProposal
import com.andeva.atelier.mobile.features.workorders.domain.Profitability
import com.andeva.atelier.mobile.features.workorders.domain.Proposal
import com.andeva.atelier.mobile.features.workorders.domain.ProposalApproval
import com.andeva.atelier.mobile.features.workorders.domain.ProposalSeverity
import com.andeva.atelier.mobile.features.workorders.domain.ServiceOption
import com.andeva.atelier.mobile.features.workorders.domain.WorkOrder
import com.andeva.atelier.mobile.features.workorders.domain.WorkOrderDetail
import com.andeva.atelier.mobile.features.workorders.domain.WorkOrderRepository
import com.andeva.atelier.mobile.features.workorders.domain.WorkOrderStatus
import com.andeva.atelier.mobile.features.workorders.domain.WorkOrderTaskChanges
import com.andeva.atelier.mobile.features.workorders.infrastructure.local.WorkOrderDao
import com.andeva.atelier.mobile.features.workorders.infrastructure.local.toDetail
import com.andeva.atelier.mobile.features.workorders.infrastructure.local.toDomain
import com.andeva.atelier.mobile.features.workorders.infrastructure.local.toEntity
import com.andeva.atelier.mobile.features.workorders.infrastructure.local.toMechanicOption
import com.andeva.atelier.mobile.features.workorders.infrastructure.remote.ApproveProposalRequestDto
import com.andeva.atelier.mobile.features.workorders.infrastructure.remote.AttachIntakeImagePayload
import com.andeva.atelier.mobile.features.workorders.infrastructure.remote.CreateWorkOrderRequestDto
import com.andeva.atelier.mobile.features.workorders.infrastructure.remote.CreateWorkOrderTaskRequestDto
import com.andeva.atelier.mobile.features.workorders.infrastructure.remote.ReasonRequestDto
import com.andeva.atelier.mobile.features.workorders.infrastructure.remote.RejectProposalRequestDto
import com.andeva.atelier.mobile.features.workorders.infrastructure.remote.ServiceCatalogService
import com.andeva.atelier.mobile.features.workorders.infrastructure.remote.SubmitProposalPayload
import com.andeva.atelier.mobile.features.workorders.infrastructure.remote.UpdateWorkOrderRequestDto
import com.andeva.atelier.mobile.features.workorders.infrastructure.remote.UpdateWorkOrderTaskRequestDto
import com.andeva.atelier.mobile.features.workorders.infrastructure.remote.WorkOrderDto
import com.andeva.atelier.mobile.features.workorders.infrastructure.remote.WorkOrderService
import com.andeva.atelier.mobile.features.workorders.infrastructure.sync.AttachIntakeImageHandler
import com.andeva.atelier.mobile.features.workorders.infrastructure.sync.SubmitProposalHandler
import com.google.gson.Gson
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import retrofit2.Response
import java.io.File
import java.time.Clock
import java.time.Instant
import javax.inject.Inject

/**
 * Offline-first para la cabecera (docs/06-offline-sync.md §4): la UI observa `work_orders` y cada respuesta del
 * backend la actualiza. El detalle (tareas, propuestas, fotos) es en línea. Fotos de recepción y propuestas van
 * siempre a la cola con [PhotoCapture] (06 §6); el resto es en línea (06 §2).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class WorkOrderRepositoryImpl @Inject constructor(
    private val workOrderService: WorkOrderService,
    private val serviceCatalogService: ServiceCatalogService,
    private val employeeService: EmployeeService,
    private val workOrderDao: WorkOrderDao,
    private val cacheMetadataDao: CacheMetadataDao,
    private val pendingOperationDao: PendingOperationDao,
    private val pendingUploadDao: PendingUploadDao,
    private val photoCapture: PhotoCapture,
    private val pdfDownloader: PdfDownloader,
    private val sessionManager: SessionManager,
    private val gson: Gson,
    private val clock: Clock
) : WorkOrderRepository {

    /** El catálogo de servicios cambia poco: se lee una vez por taller mientras viva el proceso. */
    @Volatile
    private var services: Pair<String, List<ServiceOption>>? = null

    override suspend fun refreshBranchWorkOrders(): Result<Unit> {
        val session = sessionManager.getSession() ?: return Result.failure(IllegalStateException("Sin sesión"))
        val branchId = session.branchId ?: return Result.failure(MissingBranchException())
        if (!session.has(Permission.WORK_ORDERS_READ)) return Result.success(Unit)
        return refreshBranch(branchId).map { }
    }

    override fun observeBranchWorkOrders(): Flow<List<WorkOrder>> = sessionManager.session
        .map { it?.branchId }
        .distinctUntilChanged()
        .flatMapLatest { branchId ->
            if (branchId == null) return@flatMapLatest flowOf(emptyList())
            workOrderDao.observeBranch(branchId).map { rows -> rows.map { it.toDomain() } }
        }

    override fun observeWorkOrder(id: String): Flow<WorkOrder?> = workOrderDao.observeById(id).map { it?.toDomain() }

    override suspend fun getDetail(id: String): Result<WorkOrderDetail> =
        apiCall(gson, { workOrderService.getWorkOrder(id) }) { it }.map { dto -> dto.toDetail(saveHeader(dto)) }

    override suspend fun findDraftForVehicle(branchId: String, vehicleId: String): Result<WorkOrder?> =
        refreshBranch(branchId).map { rows ->
            rows.firstOrNull { it.vehicleId == vehicleId && it.status == WorkOrderStatus.DRAFT.name }
                ?.let { workOrderDao.findById(it.id)?.toDomain() }
        }

    override suspend fun open(workOrder: NewWorkOrder): Result<WorkOrder> {
        val branchId = sessionManager.getSession()?.branchId ?: return Result.failure(MissingBranchException())
        val body = CreateWorkOrderRequestDto(
            vehicleId = workOrder.vehicleId,
            customerId = workOrder.customerId,
            branchId = branchId,
            mileageIn = workOrder.mileageIn,
            diagnosticSummary = workOrder.diagnosticSummary.blankToNull(),
            appointmentId = workOrder.appointmentId
        )
        return header { workOrderService.create(body) }
    }

    override suspend fun update(id: String, mileageIn: Int?, diagnosticSummary: String?): Result<WorkOrder> =
        header { workOrderService.update(id, UpdateWorkOrderRequestDto(mileageIn, diagnosticSummary.blankToNull())) }

    /** La tarea nueva cambia los totales y la respuesta no trae la orden: se recarga el detalle. */
    override suspend fun addTask(id: String, task: NewWorkOrderTask): Result<WorkOrderDetail> {
        val body = CreateWorkOrderTaskRequestDto(task.serviceId, task.description.trim(), task.mechanicId, task.price)
        return apiCall(gson, { workOrderService.addTask(id, body) }) { it }.then { getDetail(id) }
    }

    override suspend fun updateTask(workOrderId: String, taskId: String, changes: WorkOrderTaskChanges): Result<WorkOrderDetail> {
        val body = UpdateWorkOrderTaskRequestDto(changes.description.blankToNull(), changes.mechanicId, changes.price)
        return apiCall(gson, { workOrderService.updateTask(taskId, body) }) { it }.then { getDetail(workOrderId) }
    }

    override suspend fun getServiceCatalog(): Result<List<ServiceOption>> {
        val session = sessionManager.getSession() ?: return Result.failure(IllegalStateException("Sin sesión"))
        if (!session.has(Permission.SERVICES_READ)) return Result.success(emptyList())
        services?.takeIf { it.first == session.tenantId }?.let { return Result.success(it.second) }
        return apiCall(gson, { serviceCatalogService.getServices() }) { dtos -> dtos.map { it.toDomain() } }
            .onSuccess { services = session.tenantId to it }
    }

    /** Sin `iam:members:read` no se puede listar (H11): lista vacía y la tarea se crea sin mecánico. */
    override suspend fun getBranchMechanics(): Result<List<MechanicOption>> {
        val session = sessionManager.getSession() ?: return Result.failure(IllegalStateException("Sin sesión"))
        val branchId = session.branchId ?: return Result.failure(MissingBranchException())
        if (!session.has(Permission.MEMBERS_READ)) return Result.success(emptyList())
        return apiCall(gson, { employeeService.getByBranch(branchId) }) { dtos ->
            dtos.filter { it.employmentStatus == null || it.employmentStatus == ACTIVE }.mapNotNull { it.toMechanicOption() }
        }
    }

    override suspend fun attachIntakePhoto(id: String, photo: File, description: String?): Result<Unit> =
        photoCapture.capture(
            photo = photo,
            target = INTAKE_TARGET,
            storagePath = { tenantId, photoId -> StoragePaths.intake(tenantId, id, photoId) },
            attachType = AttachIntakeImageHandler.TYPE,
            attachDedupeKey = { photoId -> "${intakePrefix(id)}$photoId" },
            attachPayload = { imageUrl -> AttachIntakeImagePayload(id, imageUrl, description.blankToNull()) }
        ).map { }

    override fun observePendingIntakePhotos(id: String): Flow<List<PendingIntakePhoto>> =
        pending(AttachIntakeImageHandler.TYPE, intakePrefix(id), INTAKE_TARGET) { operation, localFile ->
            val payload = gson.fromJson(operation.payload, AttachIntakeImagePayload::class.java)
            PendingIntakePhoto(
                operationId = operation.id,
                localFilePath = localFile,
                imageUrl = payload.imageUrl,
                description = payload.description,
                status = operation.status,
                lastErrorMessage = operation.lastErrorMessage,
                capturedAt = Instant.ofEpochMilli(operation.createdAt)
            )
        }

    override suspend fun submitProposal(
        id: String,
        description: String,
        severity: ProposalSeverity,
        photo: File,
        suggestedServiceId: String?
    ): Result<Unit> {
        val mechanicId = sessionManager.getSession()?.membershipId
            ?: return Result.failure(IllegalStateException("Sin membresía en la sesión"))
        return photoCapture.capture(
            photo = photo,
            target = PROPOSAL_TARGET,
            storagePath = { tenantId, photoId -> StoragePaths.proposal(tenantId, id, photoId) },
            attachType = SubmitProposalHandler.TYPE,
            attachDedupeKey = { photoId -> "${proposalPrefix(id)}$photoId" },
            attachPayload = { imageUrl ->
                SubmitProposalPayload(id, mechanicId, description.trim(), severity.name, imageUrl, suggestedServiceId)
            }
        ).map { }
    }

    override fun observePendingProposals(id: String): Flow<List<PendingProposal>> =
        pending(SubmitProposalHandler.TYPE, proposalPrefix(id), PROPOSAL_TARGET) { operation, localFile ->
            val payload = gson.fromJson(operation.payload, SubmitProposalPayload::class.java)
            PendingProposal(
                operationId = operation.id,
                localFilePath = localFile,
                imageUrl = payload.imageUrl,
                description = payload.description,
                severity = ProposalSeverity.entries.firstOrNull { it.name == payload.severity },
                status = operation.status,
                lastErrorMessage = operation.lastErrorMessage,
                capturedAt = Instant.ofEpochMilli(operation.createdAt)
            )
        }

    override suspend fun notifyProposal(workOrderId: String, proposalId: String): Result<String> =
        apiCall(gson, { workOrderService.notifyProposal(workOrderId, proposalId) }) { it.recipient.orEmpty() }

    /** Crea la tarea y cambia los totales (US15 E1): se recarga el detalle. */
    override suspend fun approveProposal(workOrderId: String, proposalId: String, approval: ProposalApproval): Result<WorkOrderDetail> {
        val body = ApproveProposalRequestDto(
            serviceId = approval.serviceId,
            finalPrice = approval.finalPrice,
            laborHours = approval.laborHours,
            mechanicId = approval.mechanicId,
            notes = approval.notes.blankToNull()
        )
        return apiCall(gson, { workOrderService.approveProposal(workOrderId, proposalId, body) }) { it }
            .then { getDetail(workOrderId) }
    }

    override suspend fun rejectProposal(workOrderId: String, proposalId: String, customerNotes: String): Result<Proposal> =
        apiCall(gson, { workOrderService.rejectProposal(workOrderId, proposalId, RejectProposalRequestDto(customerNotes.trim())) }) {
            it.toDomain()
        }

    override suspend fun downloadQuotationPdf(id: String): Result<File> =
        pdfDownloader.download("proforma-$id") { workOrderService.getQuotationPdf(id) }

    override suspend fun sendQuotation(id: String): Result<String> =
        apiCall(gson, { workOrderService.sendQuotation(id) }) { it.recipient.orEmpty() }

    override suspend fun approveQuotation(id: String): Result<WorkOrder> = header { workOrderService.approveQuotation(id) }

    override suspend fun rejectQuotation(id: String, reason: String): Result<WorkOrder> =
        header { workOrderService.rejectQuotation(id, ReasonRequestDto(reason.trim())) }

    override suspend fun getPartMovements(id: String): Result<List<PartMovement>> =
        apiCall(gson, { workOrderService.getPartMovements(id) }) { dtos -> dtos.map { it.toDomain() } }

    override suspend fun getProfitability(id: String): Result<Profitability> =
        apiCall(gson, { workOrderService.getProfitability(id) }) { it.toDomain() }

    /** Recarga `work_orders:{branchId}` sin las cerradas y devuelve lo que llegó. */
    private suspend fun refreshBranch(branchId: String): Result<List<WorkOrderDto>> {
        val now = clock.millis()
        return apiCall(gson, { workOrderService.getWorkOrders(branchId) }) { it }.mapCatching { dtos ->
            val open = dtos.filterNot { dto -> WorkOrderStatus.entries.any { it.name == dto.status && it.isClosed } }
            val rows = open.map { it.toEntity(now) }
            workOrderDao.replaceBranch(branchId, rows)
            cacheMetadataDao.upsert(CacheMetadataEntity(branchScope(branchId), now, rows.size))
            open
        }
    }

    /** Acción que devuelve `WorkOrderResource`: deja la cabecera al día (o la saca de la caché si quedó cerrada). */
    private suspend fun header(call: suspend () -> Response<WorkOrderDto>): Result<WorkOrder> =
        apiCall(gson, call) { it }.map { saveHeader(it) }

    private suspend fun saveHeader(dto: WorkOrderDto): WorkOrder {
        val row = dto.toEntity(clock.millis())
        val closed = WorkOrderStatus.entries.any { it.name == dto.status && it.isClosed }
        if (closed) {
            workOrderDao.delete(dto.id)
            return row.toDomain()
        }
        workOrderDao.saveHeader(row)
        return workOrderDao.findById(dto.id)?.toDomain() ?: row.toDomain()
    }

    /** Operaciones en cola de un tipo para la orden, con el archivo local mientras no se suba. */
    private fun <T> pending(
        type: String,
        prefix: String,
        target: String,
        map: (PendingOperationEntity, String?) -> T
    ): Flow<List<T>> = combine(
        pendingOperationDao.observeByTypeAndDedupePrefix(type, prefix),
        pendingUploadDao.observeByTarget(target)
    ) { operations, uploads ->
        val localFiles = uploads.associate { it.id to it.localFilePath }
        operations.map { map(it, localFiles[it.dedupeKey.removePrefix(prefix)]) }
    }

    private suspend fun <T, R> Result<T>.then(next: suspend () -> Result<R>): Result<R> =
        fold(onSuccess = { next() }, onFailure = { Result.failure(it) })

    private fun String?.blankToNull() = this?.trim()?.takeIf { it.isNotEmpty() }

    companion object {
        const val INTAKE_TARGET = "INTAKE"
        const val PROPOSAL_TARGET = "PROPOSAL"
        private const val ACTIVE = "ACTIVE"

        fun branchScope(branchId: String) = "work_orders:$branchId"

        /** El prefijo `INTAKE_IMAGE:{woId}:` agrupa las fotos en cola de una orden; el sufijo es el id de la foto. */
        fun intakePrefix(workOrderId: String) = "INTAKE_IMAGE:$workOrderId:"

        fun proposalPrefix(workOrderId: String) = "PROPOSAL:$workOrderId:"
    }
}
