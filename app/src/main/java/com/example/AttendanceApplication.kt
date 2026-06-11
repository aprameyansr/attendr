package com.example

import android.app.Application
import com.example.data.database.AppDatabase
import com.example.data.repository.AppRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AttendanceApplication : Application() {

    // No heavy DI framework needed - constructor injection via Application context is clean, fast, and 100% robust
    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { AppRepository(database) }

    private val applicationScope = CoroutineScope(SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        
        // Initialize default settings and period timings asynchronously
        applicationScope.launch {
            repository.initializeDefaultsIfEmpty()
        }
    }
}
