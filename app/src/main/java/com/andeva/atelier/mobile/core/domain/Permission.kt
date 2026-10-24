package com.andeva.atelier.mobile.core.domain

/**
 * Permiso atómico del claim `permissions`. La UI se habilita por permiso, nunca por rol
 * (docs/02-roles-and-permissions.md).
 */
@JvmInline
value class Permission(val value: String) {
    companion object {
        // IAM
        val BRANCHES_READ = Permission("iam:branches:read")
        val MEMBERS_READ = Permission("iam:members:read")

        // HR
        val ATTENDANCE_CLOCK_IN = Permission("hr:attendance:clock_in")
        val ATTENDANCE_CLOCK_OUT = Permission("hr:attendance:clock_out")
        val ATTENDANCE_READ_OWN = Permission("hr:attendance:read_own")
        val ATTENDANCE_AUDIT_ALL = Permission("hr:attendance:audit_all")
        val SHIFTS_READ = Permission("hr:shifts:read")
        val JUSTIFICATIONS_CREATE = Permission("hr:justifications:create")
        val JUSTIFICATIONS_APPROVE = Permission("hr:justifications:approve")

        // CRM
        val CUSTOMERS_READ = Permission("crm:customers:read")
        val CUSTOMERS_CREATE = Permission("crm:customers:create")
        val VEHICLES_READ = Permission("crm:vehicles:read")
        val VEHICLES_CREATE = Permission("crm:vehicles:create")
        val APPOINTMENTS_READ = Permission("crm:appointments:read")
        val APPOINTMENTS_UPDATE = Permission("crm:appointments:update")

        // Operations
        val WORK_ORDERS_READ = Permission("operations:work_orders:read")
        val WORK_ORDERS_CREATE = Permission("operations:work_orders:create")
        val WORK_ORDERS_UPDATE = Permission("operations:work_orders:update")
        val INSPECTIONS_UPLOAD_PHOTOS = Permission("operations:inspections:upload_photos")
        val PROPOSALS_CREATE = Permission("operations:proposals:create")
        val PROPOSALS_NOTIFY = Permission("operations:proposals:notify")
        val PROPOSALS_RESOLVE = Permission("operations:proposals:resolve")
        val QUOTATIONS_READ = Permission("operations:quotations:read")
        val QUOTATIONS_SEND = Permission("operations:quotations:send")
        val QUOTATIONS_APPROVE = Permission("operations:quotations:approve")
        val TASKS_READ = Permission("operations:tasks:read")
        val TASKS_TRACK_TIME = Permission("operations:tasks:track_time")
        val TASKS_UPLOAD_PHOTOS = Permission("operations:tasks:upload_photos")
        val TASKS_HOLD_REQUEST = Permission("operations:tasks:hold_request")
        val TASKS_HOLD_VALIDATE = Permission("operations:tasks:hold_validate")
        val TASKS_COMPLETE = Permission("operations:tasks:complete")
        val TASKS_CHARGE_PARTS = Permission("operations:tasks:charge_parts")
        val BAYS_READ = Permission("operations:bays:read")
        val BAYS_REASSIGN = Permission("operations:bays:reassign")
        val VEHICLE_HANDOVER_EXECUTE = Permission("operations:vehicle_handover:execute")
        val SERVICES_READ = Permission("operations:services:read")

        // Inventory
        val PARTS_READ = Permission("inventory:parts:read")
        val BATCHES_READ = Permission("inventory:batches:read")
        val BATCHES_RESTORE = Permission("inventory:batches:restore")
        val ALERTS_READ = Permission("inventory:alerts:read")
        val ALERTS_RESOLVE = Permission("inventory:alerts:resolve")

        // IoT
        val DEVICES_READ = Permission("iot:devices:read")
        val INSTALLATIONS_MANAGE = Permission("iot:installations:manage")
        val TELEMETRY_INGEST = Permission("iot:telemetry:ingest")
        val FAULTS_READ = Permission("iot:faults:read")
        val FAULTS_RESOLVE = Permission("iot:faults:resolve")
        val HEALTH_REPORTS_GENERATE = Permission("iot:health_reports:generate")
        val HEALTH_REPORTS_READ = Permission("iot:health_reports:read")
        val PREDICTIVE_ALERTS_READ = Permission("iot:alerts:read")
        val PREDICTIVE_ALERTS_ACKNOWLEDGE = Permission("iot:alerts:acknowledge")

        // Invoicing
        val INVOICES_READ = Permission("invoicing:invoices:read")
        val INVOICES_ISSUE_SUNAT = Permission("invoicing:invoices:issue_sunat")
        val PAYMENTS_CREATE = Permission("invoicing:payments:create")
        val EXIT_PASSES_ISSUE = Permission("invoicing:exit_passes:issue")
        val CASHFLOW_EXPORT_PDF = Permission("invoicing:cashflow:export_pdf")
        val FISCAL_CONFIG_MANAGE = Permission("invoicing:fiscal_config:manage")
    }
}
