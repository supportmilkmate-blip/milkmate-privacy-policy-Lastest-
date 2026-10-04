package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_queue")
data class SyncQueueEntity(
    @PrimaryKey val id: String, // Stable UUID
    val businessId: String,
    val collectionName: String, // businesses, customers, deliveries, payments, expenses, inventory_items, inventory_transactions, orders, staff
    val documentId: String,
    val operation: String, // UPSERT, DELETE
    val payloadJson: String,
    val timestamp: Long = System.currentTimeMillis(),
    val retryCount: Int = 0,
    val status: String = "PENDING", // PENDING, SYNCING, SYNCED, FAILED, CONFLICT
    val lastError: String? = null
)
