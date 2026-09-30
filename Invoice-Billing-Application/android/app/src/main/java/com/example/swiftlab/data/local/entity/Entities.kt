package com.example.swiftlab.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val organizationId: Long,
    val sku: String,
    val name: String,
    val description: String = "",
    val costPrice: Double = 0.0,
    val sellingPrice: Double = 0.0,
    val currentStock: Double = 0.0,
    val active: Boolean = true
)

@Entity(tableName = "stocks")
data class StockEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val organizationId: Long,
    val warehouseId: Long,
    val productId: Long,
    val quantity: Double = 0.0
)

@Entity(tableName = "stock_movements")
data class StockMovementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val organizationId: Long,
    val productId: Long,
    val productName: String,
    val type: String, // STOCK_IN, SALE, PURCHASE, ADJUSTMENT, etc.
    val quantity: Double,
    val balanceAfter: Double,
    val referenceType: String = "",
    val createdAt: String
)

@Entity(tableName = "customers")
data class CustomerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val organizationId: Long,
    val name: String,
    val phone: String = "",
    val email: String = "",
    val address: String = ""
)

@Entity(tableName = "suppliers")
data class SupplierEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val organizationId: Long,
    val name: String,
    val phone: String = "",
    val email: String = "",
    val address: String = ""
)

@Entity(tableName = "sales")
data class SaleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val organizationId: Long,
    val saleNumber: String,
    val customerName: String,
    val totalAmount: Double,
    val paidAmount: Double,
    val status: String,
    val invoiceId: Long? = null,
    val createdAt: String
)

@Entity(tableName = "invoices")
data class InvoiceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val organizationId: Long,
    val invoiceNumber: String,
    val customerName: String,
    val totalAmount: Double,
    val paidAmount: Double,
    val status: String, // PENDING, PARTIALLY_PAID, PAID
    val issueDate: String,
    val dueDate: String
)

@Entity(tableName = "payments")
data class PaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val organizationId: Long,
    val invoiceId: Long?,
    val invoiceNumber: String = "",
    val paymentNumber: String,
    val amount: Double,
    val method: String, // CASH, UPI, CARD
    val paymentDate: String,
    val referenceNumber: String = ""
)

@Entity(tableName = "sync_queue")
data class SyncQueueEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val organizationId: Long,
    val deviceId: String,
    val entityType: String,
    val entityId: Long,
    val operation: String, // CREATE, UPDATE, DELETE
    val payload: String,
    val status: String = "PENDING", // PENDING, SYNCING, SYNCED, FAILED
    val retryCount: Int = 0,
    val errorMessage: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
