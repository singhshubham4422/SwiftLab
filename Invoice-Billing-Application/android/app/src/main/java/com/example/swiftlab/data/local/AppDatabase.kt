package com.example.swiftlab.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.swiftlab.data.local.dao.*
import com.example.swiftlab.data.local.entity.*

@Database(
    entities = [
        ProductEntity::class,
        StockEntity::class,
        StockMovementEntity::class,
        CustomerEntity::class,
        SupplierEntity::class,
        SaleEntity::class,
        InvoiceEntity::class,
        PaymentEntity::class,
        SyncQueueEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun stockDao(): StockDao
    abstract fun stockMovementDao(): StockMovementDao
    abstract fun customerDao(): CustomerDao
    abstract fun supplierDao(): SupplierDao
    abstract fun saleDao(): SaleDao
    abstract fun invoiceDao(): InvoiceDao
    abstract fun paymentDao(): PaymentDao
    abstract fun syncQueueDao(): SyncQueueDao
}
