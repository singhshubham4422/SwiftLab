package com.example.swiftlab.sync

import android.content.Context
import androidx.work.*
import com.example.swiftlab.data.local.SessionManager
import java.util.concurrent.TimeUnit

class SyncManager(private val context: Context) {

    private val sessionManager = SessionManager(context)
    private val workManager = WorkManager.getInstance(context)

    companion object {
        private const val SYNC_PERIODIC_WORK_TAG = "swiftlab_periodic_sync"
        private const val SYNC_ONE_TIME_WORK_TAG = "swiftlab_onetime_sync"
    }

    fun schedulePeriodicSync() {
        if (sessionManager.isLocalOnly) {
            cancelAllSync()
            return
        }

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
            .build()

        workManager.enqueueUniquePeriodicWork(
            SYNC_PERIODIC_WORK_TAG,
            ExistingPeriodicWorkPolicy.UPDATE,
            syncRequest
        )
    }

    fun triggerImmediateSync() {
        if (sessionManager.isLocalOnly) return

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val oneTimeRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .build()

        workManager.enqueueUniqueWork(
            SYNC_ONE_TIME_WORK_TAG,
            ExistingWorkPolicy.REPLACE,
            oneTimeRequest
        )
    }

    fun cancelAllSync() {
        workManager.cancelUniqueWork(SYNC_PERIODIC_WORK_TAG)
        workManager.cancelUniqueWork(SYNC_ONE_TIME_WORK_TAG)
    }
}
