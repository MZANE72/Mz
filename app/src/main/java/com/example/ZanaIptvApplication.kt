package com.example

import android.app.Application
import com.example.data.local.AppDatabase
import com.example.data.local.PreferencesManager
import com.example.data.repository.IptvRepository

class ZanaIptvApplication : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var preferencesManager: PreferencesManager
        private set

    lateinit var repository: IptvRepository
        private set

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getDatabase(this)
        preferencesManager = PreferencesManager(this)
        repository = IptvRepository(database.channelDao(), preferencesManager)
    }
}
