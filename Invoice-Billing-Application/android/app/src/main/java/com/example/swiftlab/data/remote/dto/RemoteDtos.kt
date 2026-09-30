package com.example.swiftlab.data.remote.dto

data class AuthRequest(
    val email: String,
    val password: String
)

data class AuthResponse(
    val token: String,
    val user: UserDTO,
    val organization: OrganizationDTO?
)

data class UserDTO(
    val id: Long,
    val organizationId: Long?,
    val email: String,
    val fullName: String,
    val role: String,
    val active: Boolean
)

data class OrganizationDTO(
    val id: Long,
    val name: String,
    val currencySymbol: String = "₹",
    val currencyCode: String = "INR",
    val active: Boolean = true
)

data class ProductDTO(
    val id: Long? = null,
    val organizationId: Long? = null,
    val categoryId: Long? = null,
    val brandId: Long? = null,
    val unitId: Long? = null,
    val sku: String,
    val name: String,
    val description: String? = null,
    val costPrice: Double = 0.0,
    val sellingPrice: Double = 0.0,
    val currentStock: Double = 0.0,
    val alertQuantity: Double = 5.0,
    val active: Boolean = true
)

data class CustomerDTO(
    val id: Long? = null,
    val organizationId: Long? = null,
    val name: String,
    val email: String? = null,
    val phone: String? = null,
    val address: String? = null,
    val balance: Double = 0.0
)

data class SupplierDTO(
    val id: Long? = null,
    val organizationId: Long? = null,
    val name: String,
    val email: String? = null,
    val phone: String? = null,
    val address: String? = null
)

data class SaleItemRequest(
    val productId: Long,
    val quantity: Double,
    val unitPrice: Double
)

data class SaleCreateRequest(
    val customerId: Long? = null,
    val warehouseId: Long? = null,
    val items: List<SaleItemRequest>,
    val paymentStatus: String = "PAID",
    val paymentMethod: String = "CASH",
    val paidAmount: Double = 0.0,
    val notes: String? = null
)

data class SaleDTO(
    val id: Long,
    val saleNumber: String,
    val customerId: Long?,
    val customerName: String? = null,
    val totalAmount: Double,
    val paidAmount: Double,
    val status: String,
    val invoiceId: Long? = null,
    val createdAt: String? = null
)

data class InvoiceDTO(
    val id: Long,
    val invoiceNumber: String,
    val customerName: String? = null,
    val totalAmount: Double,
    val paidAmount: Double,
    val status: String,
    val issueDate: String? = null,
    val dueDate: String? = null
)

data class PaymentCreateRequest(
    val invoiceId: Long? = null,
    val amount: Double,
    val method: String = "CASH",
    val referenceNumber: String? = null,
    val notes: String? = null
)

data class PaymentDTO(
    val id: Long,
    val paymentNumber: String,
    val invoiceId: Long?,
    val invoiceNumber: String? = null,
    val amount: Double,
    val method: String,
    val paymentDate: String? = null,
    val referenceNumber: String? = null
)

data class StockBalanceDTO(
    val productId: Long,
    val productName: String,
    val sku: String,
    val quantity: Double
)

data class StockMovementDTO(
    val id: Long,
    val productId: Long,
    val productName: String,
    val type: String,
    val quantity: Double,
    val balanceAfter: Double,
    val referenceType: String? = null,
    val createdAt: String
)

data class StockAdjustRequest(
    val warehouseId: Long? = null,
    val productId: Long,
    val type: String,
    val quantity: Double,
    val notes: String? = null
)

data class SyncStatusDTO(
    val mode: String,
    val pendingCount: Long = 0,
    val failedCount: Long = 0,
    val lastSyncTime: String? = null
)

data class ModeSwitchRequest(
    val targetMode: String,
    val confirm: Boolean = true
)

data class SyncItemDTO(
    val id: Long,
    val entityType: String,
    val entityId: Long,
    val operation: String,
    val payload: String
)

data class SyncBatchRequest(
    val deviceId: String,
    val items: List<SyncItemDTO>
)

data class SyncBatchResponse(
    val processedCount: Int,
    val errorCount: Int,
    val success: Boolean
)
