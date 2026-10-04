package com.example.worker

import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit

object StockWorkScheduler {

    private const val UNIQUE_PERIODIC_WORK_NAME = "MilkMatePeriodicStockMonitor"

    /**
     * Schedules periodic background monitoring of feed and stock inventory (runs every 6 hours).
     */
    fun schedulePeriodicMonitoring(context: Context) {
        try {
            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(false)
                .build()

            val periodicWorkRequest = PeriodicWorkRequestBuilder<StockMonitoringWorker>(
                6, TimeUnit.HOURS,
                30, TimeUnit.MINUTES // Flex interval
            )
                .setConstraints(constraints)
                .addTag("stock_monitoring")
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_PERIODIC_WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                periodicWorkRequest
            )
        } catch (e: Throwable) {
            android.util.Log.w("StockWorkScheduler", "Failed to schedule periodic monitoring: ${e.message}")
        }
    }

    /**
     * Triggers an immediate one-time stock check and alert delivery.
     * Useful when stock is updated, on app startup, or when testing notifications in Settings.
     */
    fun triggerImmediateStockCheck(context: Context) {
        try {
            val oneTimeWork = OneTimeWorkRequestBuilder<StockMonitoringWorker>()
                .addTag("stock_monitoring_immediate")
                .build()

            WorkManager.getInstance(context).enqueue(oneTimeWork)
        } catch (e: Throwable) {
            android.util.Log.w("StockWorkScheduler", "Failed to trigger immediate check: ${e.message}")
        }
    }

    /**
     * Cancels scheduled stock monitoring if user disables alerts in settings.
     */
    fun cancelStockMonitoring(context: Context) {
        try {
            WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_PERIODIC_WORK_NAME)
        } catch (e: Throwable) {
            android.util.Log.w("StockWorkScheduler", "Failed to cancel monitoring: ${e.message}")
        }
    }
}
