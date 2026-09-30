package com.example.swiftlab.data.repository

import com.example.swiftlab.data.local.AppDatabase
import com.example.swiftlab.data.local.SessionManager
import com.example.swiftlab.data.local.entity.*
import com.example.swiftlab.data.remote.RetrofitClient
import com.example.swiftlab.data.remote.dto.*
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class AppRepository(
    private val database: AppDatabase,
    private val sessionManager: SessionManager,
    private val retrofitClient: RetrofitClient
) {
    private val gson = Gson()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    private val dateOnlyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    val products: Flow<List<ProductEntity>> = database.productDao().getAll(sessionManager.orgId)
    val stockMovements: Flow<List<StockMovementEntity>> = database.stockMovementDao().getMovements(sessionManager.orgId)
    val customers: Flow<List<CustomerEntity>> = database.customerDao().getAll(sessionManager.orgId)
    val suppliers: Flow<List<SupplierEntity>> = database.supplierDao().getAll(sessionManager.orgId)
    val sales: Flow<List<SaleEntity>> = database.saleDao().getAll(sessionManager.orgId)
    val invoices: Flow<List<InvoiceEntity>> = database.invoiceDao().getAll(sessionManager.orgId)
    val payments: Flow<List<PaymentEntity>> = database.paymentDao().getAll(sessionManager.orgId)
    val pendingSyncCount: Flow<Int> = database.syncQueueDao().getPendingCount()

    // -------------------------------------------------------------
    // PRODUCTS & INVENTORY
    // -------------------------------------------------------------

    suspend fun createProduct(
        sku: String,
        name: String,
        description: String,
        costPrice: Double,
        sellingPrice: Double,
        initialStock: Double
    ): Long = withContext(Dispatchers.IO) {
        val orgId = sessionManager.orgId
        val product = ProductEntity(
            organizationId = orgId,
            sku = sku,
            name = name,
            description = description,
            costPrice = costPrice,
            sellingPrice = sellingPrice,
            currentStock = initialStock,
            active = true
        )
        val productId = database.productDao().insert(product)

        if (initialStock > 0) {
            val movement = StockMovementEntity(
                organizationId = orgId,
                productId = productId,
                productName = name,
                type = "STOCK_IN",
                quantity = initialStock,
                balanceAfter = initialStock,
                referenceType = "INITIAL_STOCK",
                createdAt = dateFormat.format(Date())
            )
            database.stockMovementDao().insert(movement)
        }

        if (sessionManager.isCloudSync) {
            val dto = ProductDTO(
                sku = sku,
                name = name,
                description = description,
                costPrice = costPrice,
                sellingPrice = sellingPrice,
                currentStock = initialStock
            )
            queueSyncItem("Product", productId, "CREATE", gson.toJson(dto))
        }

        productId
    }

    suspend fun adjustStock(
        productId: Long,
        type: String, // STOCK_IN, STOCK_OUT, ADJUSTMENT
        quantity: Double,
        reason: String
    ) = withContext(Dispatchers.IO) {
        val product = database.productDao().getById(productId) ?: return@withContext
        val newStock = when (type) {
            "STOCK_IN" -> product.currentStock + quantity
            "STOCK_OUT" -> (product.currentStock - quantity).coerceAtLeast(0.0)
            else -> quantity // Direct adjustment
        }

        database.productDao().updateStock(productId, newStock)

        val movement = StockMovementEntity(
            organizationId = sessionManager.orgId,
            productId = productId,
            productName = product.name,
            type = type,
            quantity = quantity,
            balanceAfter = newStock,
            referenceType = reason,
            createdAt = dateFormat.format(Date())
        )
        database.stockMovementDao().insert(movement)

        if (sessionManager.isCloudSync) {
            val req = StockAdjustRequest(
                warehouseId = 1L,
                productId = productId,
                type = type,
                quantity = quantity,
                notes = reason
            )
            queueSyncItem("InventoryAdjustment", productId, "ADJUST", gson.toJson(req))
        }
    }

    // -------------------------------------------------------------
    // CUSTOMERS & SUPPLIERS
    // -------------------------------------------------------------

    suspend fun createCustomer(name: String, phone: String, email: String, address: String): Long =
        withContext(Dispatchers.IO) {
            val orgId = sessionManager.orgId
            val entity = CustomerEntity(
                organizationId = orgId,
                name = name,
                phone = phone,
                email = email,
                address = address
            )
            val id = database.customerDao().insert(entity)

            if (sessionManager.isCloudSync) {
                val dto = CustomerDTO(name = name, phone = phone, email = email, address = address)
                queueSyncItem("Customer", id, "CREATE", gson.toJson(dto))
            }
            id
        }

    suspend fun createSupplier(name: String, phone: String, email: String, address: String): Long =
        withContext(Dispatchers.IO) {
            val orgId = sessionManager.orgId
            val entity = SupplierEntity(
                organizationId = orgId,
                name = name,
                phone = phone,
                email = email,
                address = address
            )
            val id = database.supplierDao().insert(entity)

            if (sessionManager.isCloudSync) {
                val dto = SupplierDTO(name = name, phone = phone, email = email, address = address)
                queueSyncItem("Supplier", id, "CREATE", gson.toJson(dto))
            }
            id
        }

    // -------------------------------------------------------------
    // SALES, INVOICES & PAYMENTS
    // -------------------------------------------------------------

    data class SaleItemInput(
        val product: ProductEntity,
        val quantity: Double,
        val unitPrice: Double
    )

    suspend fun recordSale(
        customerId: Long?,
        customerName: String,
        items: List<SaleItemInput>,
        paymentMethod: String,
        paidAmount: Double
    ): Long = withContext(Dispatchers.IO) {
        val orgId = sessionManager.orgId
        val timestamp = System.currentTimeMillis()
        val nowStr = dateFormat.format(Date(timestamp))
        val todayStr = dateOnlyFormat.format(Date(timestamp))

        val totalAmount = items.sumOf { it.quantity * it.unitPrice }
        val saleNumber = "SALE-${timestamp % 1000000}"
        val invoiceNumber = "INV-${timestamp % 1000000}"

        val invoiceStatus = when {
            paidAmount >= totalAmount -> "PAID"
            paidAmount > 0 -> "PARTIALLY_PAID"
            else -> "PENDING"
        }

        // 1. Create Invoice
        val invoice = InvoiceEntity(
            organizationId = orgId,
            invoiceNumber = invoiceNumber,
            customerName = customerName,
            totalAmount = totalAmount,
            paidAmount = paidAmount,
            status = invoiceStatus,
            issueDate = todayStr,
            dueDate = todayStr
        )
        val invoiceId = database.invoiceDao().insert(invoice)

        // 2. Create Sale
        val sale = SaleEntity(
            organizationId = orgId,
            saleNumber = saleNumber,
            customerName = customerName,
            totalAmount = totalAmount,
            paidAmount = paidAmount,
            status = "COMPLETED",
            invoiceId = invoiceId,
            createdAt = nowStr
        )
        val saleId = database.saleDao().insert(sale)

        // 3. Update stock and record stock movements
        for (item in items) {
            val currentStock = item.product.currentStock
            val newStock = (currentStock - item.quantity).coerceAtLeast(0.0)
            database.productDao().updateStock(item.product.id, newStock)

            val movement = StockMovementEntity(
                organizationId = orgId,
                productId = item.product.id,
                productName = item.product.name,
                type = "SALE",
                quantity = item.quantity,
                balanceAfter = newStock,
                referenceType = saleNumber,
                createdAt = nowStr
            )
            database.stockMovementDao().insert(movement)
        }

        // 4. Create Payment if money was received
        if (paidAmount > 0) {
            val paymentNumber = "PAY-${timestamp % 1000000}"
            val payment = PaymentEntity(
                organizationId = orgId,
                invoiceId = invoiceId,
                invoiceNumber = invoiceNumber,
                paymentNumber = paymentNumber,
                amount = paidAmount,
                method = paymentMethod,
                paymentDate = todayStr,
                referenceNumber = "POS-$saleNumber"
            )
            database.paymentDao().insert(payment)
        }

        // 5. Cloud Sync Queue if CLOUD_SYNC mode
        if (sessionManager.isCloudSync) {
            val req = SaleCreateRequest(
                customerId = customerId,
                warehouseId = 1L,
                items = items.map { SaleItemRequest(it.product.id, it.quantity, it.unitPrice) },
                paymentStatus = invoiceStatus,
                paymentMethod = paymentMethod,
                paidAmount = paidAmount,
                notes = "Mobile POS Sale $saleNumber"
            )
            queueSyncItem("Sale", saleId, "CREATE", gson.toJson(req))
        }

        saleId
    }

    suspend fun recordPayment(
        invoiceId: Long,
        invoiceNumber: String,
        amount: Double,
        method: String,
        referenceNumber: String
    ): Long = withContext(Dispatchers.IO) {
        val orgId = sessionManager.orgId
        val timestamp = System.currentTimeMillis()
        val todayStr = dateOnlyFormat.format(Date(timestamp))
        val paymentNumber = "PAY-${timestamp % 1000000}"

        val payment = PaymentEntity(
            organizationId = orgId,
            invoiceId = invoiceId,
            invoiceNumber = invoiceNumber,
            paymentNumber = paymentNumber,
            amount = amount,
            method = method,
            paymentDate = todayStr,
            referenceNumber = referenceNumber
        )
        val id = database.paymentDao().insert(payment)

        if (sessionManager.isCloudSync) {
            val req = PaymentCreateRequest(
                invoiceId = invoiceId,
                amount = amount,
                method = method,
                referenceNumber = referenceNumber,
                notes = "Mobile POS payment"
            )
            queueSyncItem("Payment", id, "CREATE", gson.toJson(req))
        }

        id
    }

    // -------------------------------------------------------------
    // SYNCHRONIZATION ENGINE
    // -------------------------------------------------------------

    private suspend fun queueSyncItem(
        entityType: String,
        entityId: Long,
        operation: String,
        payload: String
    ) {
        // Double check mode - NEVER queue for cloud if LOCAL_ONLY!
        if (sessionManager.isLocalOnly) return

        val item = SyncQueueEntity(
            organizationId = sessionManager.orgId,
            deviceId = sessionManager.deviceId,
            entityType = entityType,
            entityId = entityId,
            operation = operation,
            payload = payload,
            status = "PENDING"
        )
        database.syncQueueDao().insert(item)
    }

    suspend fun syncPendingQueue(): Boolean = withContext(Dispatchers.IO) {
        // Critical: in LOCAL_ONLY mode, do NOTHING!
        if (sessionManager.isLocalOnly) {
            return@withContext false
        }

        val pending = database.syncQueueDao().getPending()
        if (pending.isEmpty()) return@withContext true

        val syncItems = pending.map {
            SyncItemDTO(
                id = it.id,
                entityType = it.entityType,
                entityId = it.entityId,
                operation = it.operation,
                payload = it.payload
            )
        }

        val batchRequest = SyncBatchRequest(
            deviceId = sessionManager.deviceId,
            items = syncItems
        )

        return@withContext try {
            val response = retrofitClient.getService().sendSyncBatch(batchRequest)
            if (response.isSuccessful && response.body()?.success == true) {
                // Delete processed items from local queue
                pending.forEach { database.syncQueueDao().delete(it.id) }
                true
            } else {
                pending.forEach {
                    database.syncQueueDao().updateStatus(it.id, "FAILED", response.message())
                }
                false
            }
        } catch (e: Exception) {
            pending.forEach {
                database.syncQueueDao().updateStatus(it.id, "FAILED", e.localizedMessage)
            }
            false
        }
    }

    suspend fun pullRemoteData(): Boolean = withContext(Dispatchers.IO) {
        // Critical: In LOCAL_ONLY mode, never contact cloud!
        if (sessionManager.isLocalOnly) return@withContext false

        return@withContext try {
            val api = retrofitClient.getService()
            val orgId = sessionManager.orgId

            // 1. Products
            val prodResp = api.getProducts()
            if (prodResp.isSuccessful && prodResp.body() != null) {
                val entities = prodResp.body()!!.map {
                    ProductEntity(
                        id = it.id ?: 0L,
                        organizationId = orgId,
                        sku = it.sku,
                        name = it.name,
                        description = it.description ?: "",
                        costPrice = it.costPrice,
                        sellingPrice = it.sellingPrice,
                        currentStock = it.currentStock,
                        active = it.active
                    )
                }
                database.productDao().insertAll(entities)
            }

            // 2. Customers
            val custResp = api.getCustomers()
            if (custResp.isSuccessful && custResp.body() != null) {
                val entities = custResp.body()!!.map {
                    CustomerEntity(
                        id = it.id ?: 0L,
                        organizationId = orgId,
                        name = it.name,
                        phone = it.phone ?: "",
                        email = it.email ?: "",
                        address = it.address ?: ""
                    )
                }
                database.customerDao().insertAll(entities)
            }

            // 3. Suppliers
            val suppResp = api.getSuppliers()
            if (suppResp.isSuccessful && suppResp.body() != null) {
                val entities = suppResp.body()!!.map {
                    SupplierEntity(
                        id = it.id ?: 0L,
                        organizationId = orgId,
                        name = it.name,
                        phone = it.phone ?: "",
                        email = it.email ?: "",
                        address = it.address ?: ""
                    )
                }
                database.supplierDao().insertAll(entities)
            }

            // 4. Invoices
            val invResp = api.getInvoices()
            if (invResp.isSuccessful && invResp.body() != null) {
                val entities = invResp.body()!!.map {
                    InvoiceEntity(
                        id = it.id,
                        organizationId = orgId,
                        invoiceNumber = it.invoiceNumber,
                        customerName = it.customerName ?: "General Customer",
                        totalAmount = it.totalAmount,
                        paidAmount = it.paidAmount,
                        status = it.status,
                        issueDate = it.issueDate ?: "",
                        dueDate = it.dueDate ?: ""
                    )
                }
                database.invoiceDao().insertAll(entities)
            }

            true
        } catch (e: Exception) {
            false
        }
    }
}
