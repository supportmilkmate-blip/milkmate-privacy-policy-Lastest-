package com.example

import android.app.Application
import com.example.data.local.MilkMateDatabase
import com.example.data.remote.FirestoreSyncManager
import com.example.data.repository.*

class MilkMateApplication : Application() {

    lateinit var database: MilkMateDatabase
        private set

    lateinit var sessionManager: SessionManager
        private set

    lateinit var billingManager: BillingManager
        private set

    lateinit var reportEngine: ReportEngine
        private set

    lateinit var consumptionForecastEngine: ConsumptionForecastEngine
        private set

    lateinit var exportManager: ExportManager
        private set

    lateinit var backupManager: BackupManager
        private set

    lateinit var firestoreSyncManager: FirestoreSyncManager
        private set

    lateinit var repository: MilkMateRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        database = MilkMateDatabase.getInstance(this)
        sessionManager = SessionManager(this)
        billingManager = BillingManager(database.customerDao())
        reportEngine = ReportEngine(
            deliveryDao = database.deliveryDao(),
            paymentDao = database.paymentDao(),
            expenseDao = database.expenseDao(),
            customerDao = database.customerDao(),
            inventoryDao = database.inventoryDao(),
            milkWastageDao = database.milkWastageDao()
        )
        consumptionForecastEngine = ConsumptionForecastEngine(
            deliveryDao = database.deliveryDao(),
            inventoryDao = database.inventoryDao()
        )
        exportManager = ExportManager(this)
        backupManager = BackupManager(this, database)
        firestoreSyncManager = FirestoreSyncManager(database.syncQueueDao())

        repository = MilkMateRepository(
            db = database,
            sessionManager = sessionManager,
            billingManager = billingManager,
            reportEngine = reportEngine,
            consumptionForecastEngine = consumptionForecastEngine,
            exportManager = exportManager,
            backupManager = backupManager,
            firestoreSyncManager = firestoreSyncManager
        )

        // Schedule WorkManager background stock level monitoring service
        com.example.worker.StockWorkScheduler.schedulePeriodicMonitoring(this)
    }

    companion object {
        lateinit var instance: MilkMateApplication
            private set
    }
}
