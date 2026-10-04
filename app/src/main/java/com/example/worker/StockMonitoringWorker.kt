package com.example.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.MainActivity
import com.example.data.local.MilkMateDatabase
import com.example.data.repository.SessionManager

class StockMonitoringWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val sessionManager = SessionManager(applicationContext)
            val session = sessionManager.sessionState.value

            // Check if notifications & low stock alerts are enabled
            if (!session.notificationsEnabled || !session.lowStockAlertsEnabled) {
                return Result.success()
            }

            val businessId = session.businessId
            if (businessId.isBlank()) {
                return Result.success()
            }

            val database = MilkMateDatabase.getInstance(applicationContext)
            val biz = database.businessDao().getBusinessById(businessId)
            val mode = biz?.businessMode
            val showFeed = com.example.ui.util.BusinessModeFeatures.showCattleFeed(mode)
            val rawItems = database.inventoryDao().getAllInventoryItems(businessId)
            val inventoryItems = com.example.ui.util.BusinessModeFeatures.filterInventoryForMode(rawItems, mode)

            // Find items that are low or out of stock
            val lowStockItems = inventoryItems.filter { item ->
                item.stockAlertStatus == "OUT_OF_STOCK" ||
                item.stockAlertStatus == "LOW_STOCK" ||
                item.currentStock <= item.alertDaysThreshold
            }

            if (lowStockItems.isNotEmpty()) {
                sendStockAlertNotification(applicationContext, lowStockItems, showFeed)
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    private fun sendStockAlertNotification(
        context: Context,
        lowItems: List<com.example.data.local.entity.InventoryItemEntity>,
        showFeed: Boolean
    ) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channelId = if (showFeed) "milkmate_feed_stock_alerts" else "milkmate_product_stock_alerts"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                if (showFeed) "Feed & Cattle Stock Alerts" else "Dairy Product Stock Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = if (showFeed) "Notifies when cattle feed or supplements reach low stock levels" else "Notifies when dairy products and inventory items reach low stock levels"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("NAVIGATE_TO", "INVENTORY")
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val outOfStockCount = lowItems.count { it.currentStock <= 0 }
        val lowCount = lowItems.size - outOfStockCount

        val title = if (showFeed) {
            if (outOfStockCount > 0) "🚨 Urgent: $outOfStockCount Feed Items Out of Stock!"
            else "⚠️ Cattle Feed Low Stock Alert (${lowItems.size} items)"
        } else {
            if (outOfStockCount > 0) "🚨 Urgent: $outOfStockCount Products Out of Stock!"
            else "⚠️ Low Stock Alert (${lowItems.size} products)"
        }

        val summaryText = if (lowItems.size == 1) {
            val item = lowItems.first()
            val daysStr = item.estimatedDaysRemaining?.let { " (~${it.toInt()} days left)" } ?: ""
            val actionWord = if (showFeed) "reorder feed" else "restock item"
            "${item.itemName}: ${item.currentStock} ${item.unit} remaining$daysStr. Tap to $actionWord."
        } else {
            val names = lowItems.take(3).joinToString(", ") { "${it.itemName} (${it.currentStock.toInt()}${it.unit})" }
            "$names${if (lowItems.size > 3) " +${lowItems.size - 3} more" else ""}. Tap to view inventory."
        }

        val bigText = buildString {
            if (showFeed) {
                appendLine("The following cattle feed & farm inventory items need attention:")
            } else {
                appendLine("The following dairy products & store inventory items need attention:")
            }
            lowItems.forEach { item ->
                val daysInfo = item.estimatedDaysRemaining?.let { " • ~${it.toInt()} days left" } ?: ""
                val status = if (item.currentStock <= 0) "🔴 OUT OF STOCK" else "🟡 LOW"
                appendLine("• ${item.itemName}: ${item.currentStock} ${item.unit} ($status$daysInfo)")
            }
            append(if (showFeed) "Tap to reorder feed & update inventory." else "Tap to restock & update inventory.")
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle(title)
            .setContentText(summaryText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        notificationManager.notify(NOTIFICATION_ID, builder.build())
    }

    companion object {
        const val NOTIFICATION_ID = 4001
    }
}
