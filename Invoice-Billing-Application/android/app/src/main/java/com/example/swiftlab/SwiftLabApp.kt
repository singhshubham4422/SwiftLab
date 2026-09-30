package com.example.swiftlab

import android.app.Application
import android.content.Context
import androidx.room.Room
import com.example.swiftlab.data.local.AppDatabase

class SwiftLabApp : Application() {

    lateinit var database: AppDatabase
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "swiftlab_mobile.db"
        ).fallbackToDestructiveMigration().build()
    }

    companion object {
        lateinit var instance: SwiftLabApp
            private set

        fun getContext(): Context = instance.applicationContext
    }
}
