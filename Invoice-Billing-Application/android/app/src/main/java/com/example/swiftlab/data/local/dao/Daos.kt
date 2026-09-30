package com.example.swiftlab.data.local.dao

import androidx.room.*
import com.example.swiftlab.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Query("SELECT * FROM products WHERE organizationId = :orgId AND active = 1 ORDER BY name ASC")
    fun getAll(orgId: Long): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): ProductEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(product: ProductEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(products: List<ProductEntity>)

    @Query("UPDATE products SET currentStock = :newStock WHERE id = :productId")
    suspend fun updateStock(productId: Long, newStock: Double)
}

@Dao
interface StockDao {
    @Query("SELECT * FROM stocks WHERE organizationId = :orgId")
    fun getStocks(orgId: Long): Flow<List<StockEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(stock: StockEntity)
}

@Dao
interface StockMovementDao {
    @Query("SELECT * FROM stock_movements WHERE organizationId = :orgId ORDER BY id DESC")
    fun getMovements(orgId: Long): Flow<List<StockMovementEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(movement: StockMovementEntity)
}

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers WHERE organizationId = :orgId ORDER BY name ASC")
    fun getAll(orgId: Long): Flow<List<CustomerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(customer: CustomerEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(customers: List<CustomerEntity>)
}

@Dao
interface SupplierDao {
    @Query("SELECT * FROM suppliers WHERE organizationId = :orgId ORDER BY name ASC")
    fun getAll(orgId: Long): Flow<List<SupplierEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(supplier: SupplierEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(suppliers: List<SupplierEntity>)
}

@Dao
interface SaleDao {
    @Query("SELECT * FROM sales WHERE organizationId = :orgId ORDER BY id DESC")
    fun getAll(orgId: Long): Flow<List<SaleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(sale: SaleEntity): Long
}

@Dao
interface InvoiceDao {
    @Query("SELECT * FROM invoices WHERE organizationId = :orgId ORDER BY id DESC")
    fun getAll(orgId: Long): Flow<List<InvoiceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(invoice: InvoiceEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(invoices: List<InvoiceEntity>)
}

@Dao
interface PaymentDao {
    @Query("SELECT * FROM payments WHERE organizationId = :orgId ORDER BY id DESC")
    fun getAll(orgId: Long): Flow<List<PaymentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(payment: PaymentEntity): Long
}

@Dao
interface SyncQueueDao {
    @Query("SELECT * FROM sync_queue WHERE status IN ('PENDING', 'FAILED') ORDER BY id ASC")
    suspend fun getPending(): List<SyncQueueEntity>

    @Query("SELECT COUNT(*) FROM sync_queue WHERE status = 'PENDING'")
    fun getPendingCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: SyncQueueEntity): Long

    @Query("UPDATE sync_queue SET status = :status, errorMessage = :error, retryCount = retryCount + 1 WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String, error: String? = null)

    @Query("DELETE FROM sync_queue WHERE id = :id")
    suspend fun delete(id: Long)
}
