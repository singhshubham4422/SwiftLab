package com.example.swiftlab.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.swiftlab.SwiftLabApp
import com.example.swiftlab.data.local.SessionManager
import com.example.swiftlab.data.remote.RetrofitClient
import com.example.swiftlab.data.repository.AppRepository

class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val app = applicationContext as SwiftLabApp
        val sessionManager = SessionManager(app)

        // Strict guard: NEVER sync in LOCAL_ONLY mode
        if (sessionManager.isLocalOnly) {
            return Result.success()
        }

        val retrofitClient = RetrofitClient(sessionManager)
        val repository = AppRepository(app.database, sessionManager, retrofitClient)

        return try {
            val pushSuccess = repository.syncPendingQueue()
            val pullSuccess = repository.pullRemoteData()
            if (pushSuccess && pullSuccess) {
                Result.success()
            } else {
                Result.retry()
            }
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
