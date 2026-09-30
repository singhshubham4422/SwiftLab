package com.example.swiftlab.data.remote

import com.example.swiftlab.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    @POST("api/auth/login")
    suspend fun login(@Body request: AuthRequest): Response<AuthResponse>

    @GET("api/products")
    suspend fun getProducts(): Response<List<ProductDTO>>

    @POST("api/products")
    suspend fun createProduct(@Body product: ProductDTO): Response<ProductDTO>

    @GET("api/customers")
    suspend fun getCustomers(): Response<List<CustomerDTO>>

    @POST("api/customers")
    suspend fun createCustomer(@Body customer: CustomerDTO): Response<CustomerDTO>

    @GET("api/suppliers")
    suspend fun getSuppliers(): Response<List<SupplierDTO>>

    @POST("api/suppliers")
    suspend fun createSupplier(@Body supplier: SupplierDTO): Response<SupplierDTO>

    @GET("api/sales")
    suspend fun getSales(): Response<List<SaleDTO>>

    @POST("api/sales")
    suspend fun createSale(@Body request: SaleCreateRequest): Response<SaleDTO>

    @GET("api/invoices")
    suspend fun getInvoices(): Response<List<InvoiceDTO>>

    @POST("api/payments")
    suspend fun recordPayment(@Body request: PaymentCreateRequest): Response<PaymentDTO>

    @GET("api/sync/status")
    suspend fun getSyncStatus(): Response<SyncStatusDTO>

    @POST("api/sync/batch")
    suspend fun sendSyncBatch(@Body request: SyncBatchRequest): Response<SyncBatchResponse>

    @POST("api/sync/mode")
    suspend fun switchMode(@Body request: ModeSwitchRequest): Response<SyncStatusDTO>
}
