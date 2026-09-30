# SwiftLab Android Native Client Architecture Plan

## 1. Executive Summary

This document specifies the technical architecture for the future native Android client of the SwiftLab Inventory & Billing Platform. 
**No Flutter, React Native, or non-native hybrid frameworks are permitted.**
The client will be built using modern native Android technologies:
- **Language:** Kotlin 2.0+
- **UI Toolkit:** Jetpack Compose & Material 3
- **Local Database:** Room (SQLite) with SQLCipher encryption
- **Networking:** Retrofit 2 + OkHttp 4 + KotlinX Serialization
- **Concurrency:** Kotlin Coroutines & Flow
- **Background Synchronization:** Android Jetpack WorkManager

---

## 2. Authentication & Security

1. **Authentication Flow:**
   - The user inputs `username` and `password`.
   - Client sends `POST /api/auth/login` with client device ID: `ANDROID-<UUID>`.
   - Server validates credentials (BCrypt) and issues an API Bearer Token.
   - The token is stored securely in Android's `EncryptedSharedPreferences` backed by the Android Keystore.
2. **Request Interceptor:**
   An OkHttp `Interceptor` automatically attaches the token:
   ```kotlin
   header("Authorization", "Bearer $storedToken")
   ```

---

## 3. Local Database & Offline-First Strategy (Room)

The Android client maintains a mirrored local Room database matching the core entities:
- `ProductEntity`, `StockEntity`, `CustomerEntity`, `SaleEntity`, `SaleItemEntity`, `SyncQueueEntity`.

All UI components observe local database state via `Flow<List<Product>>`.

---

## 4. WorkManager Background Synchronization

1. **Queueing Offline Transactions:**
   - When the user processes a sale or stock adjustment offline, a local Room transaction writes the record and queues an item in `sync_queue` with status `PENDING`.
2. **Periodic & Network-Constrained Sync:**
   - A `CoroutineWorker` is scheduled via `WorkManager`:
     ```kotlin
     val constraints = Constraints.Builder()
         .setRequiredNetworkType(NetworkType.CONNECTED)
         .build()
     ```
   - When network connectivity is detected, `SyncWorker` reads pending items from Room, dispatches a batch request to `POST /api/sync/push`, and marks synced items upon server confirmation.

---

## 5. Required Mobile Screens (Jetpack Compose)

1. **Auth & Onboarding:**
   - Login Screen (`/login`)
   - Organization Setup & Profile
2. **Dashboard & Summary:**
   - Daily Sales, Cash Flow, Pending Invoices, Sync Status Pill
3. **POS Checkout:**
   - Rapid barcode scanning via CameraX + ML Kit
   - Real-time cart calculation and stock validation
   - Multiple payment methods (Cash, UPI QR, Card)
4. **Inventory & Movement:**
   - Product Catalog with stock badge
   - Quick Stock In / Adjustment dialog
   - Movement History Ledger
5. **Invoices & Receipts:**
   - Invoice List & Filter
   - In-app PDF Viewer & Bluetooth Thermal Receipt Printing
6. **Sync & Settings:**
   - Data Mode Selector (`LOCAL_ONLY` vs `CLOUD_SYNC`)
   - Manual Sync Trigger button
   - Device Identifier display
