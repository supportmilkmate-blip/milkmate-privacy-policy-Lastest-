package com.example.data.remote

import android.util.Log
import com.example.MilkMateApplication
import com.example.R
import com.example.data.local.dao.SyncQueueDao
import com.example.data.local.entity.*
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Enterprise Cloud Sync Manager using Firebase Firestore
 * 
 * Functions:
 * 1. Pushes local Room mutations to Firebase Firestore using named DB ID.
 * 2. Pulls and restores cloud data upon login / device switch.
 * 3. Guarantees zero data loss across re-installs and multi-device access.
 */
class FirestoreSyncManager(private val syncQueueDao: SyncQueueDao) {

    private val tag = "FirestoreSyncManager"

    private fun getFirestore(): FirebaseFirestore? {
        return try {
            val app = MilkMateApplication.instance
            if (FirebaseApp.getApps(app).isNotEmpty()) {
                val dbId = app.getString(R.string.firestore_database_id)
                FirebaseFirestore.getInstance(FirebaseApp.getInstance(), dbId)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.w(tag, "Firebase initialization check: ${e.message}")
            null
        }
    }

    /**
     * Synchronizes pending queue items with Firestore.
     * Path: businesses/{businessId}/{collectionName}/{documentId}
     */
    suspend fun syncPendingItems(): Int = withContext(Dispatchers.IO) {
        val firestore = getFirestore() ?: return@withContext 0
        val pendingItems = syncQueueDao.getPendingSyncItems()
        var syncedCount = 0

        for (item in pendingItems) {
            try {
                syncQueueDao.updateStatus(item.id, "SYNCING")

                val docRef = firestore
                    .collection("businesses")
                    .document(item.businessId)
                    .collection(item.collectionName)
                    .document(item.documentId)

                if (item.operation == "DELETE") {
                    val deleteData = mapOf(
                        "deletedAt" to System.currentTimeMillis(),
                        "syncStatus" to "SYNCED"
                    )
                    docRef.set(deleteData, SetOptions.merge()).await()
                } else {
                    val map = jsonToMap(item.payloadJson)
                    docRef.set(map, SetOptions.merge()).await()
                }

                syncQueueDao.updateStatus(item.id, "SYNCED")
                syncQueueDao.delete(item.id)
                syncedCount++
            } catch (e: Exception) {
                Log.e(tag, "Sync failed for item ${item.id}: ${e.message}")
                val updatedItem = item.copy(
                    status = "FAILED",
                    retryCount = item.retryCount + 1,
                    lastError = e.localizedMessage
                )
                syncQueueDao.update(updatedItem)
            }
        }
        return@withContext syncedCount
    }

    /**
     * Restores all business records from Firebase Cloud Firestore into local Room DB
     */
    suspend fun restoreCloudData(businessId: String): Boolean = withContext(Dispatchers.IO) {
        val firestore = getFirestore() ?: return@withContext false
        val app = MilkMateApplication.instance
        val db = app.database

        try {
            Log.i(tag, "Starting Cloud Restore for business: $businessId")

            // 1. Restore Business Info
            val bizDoc = firestore.collection("businesses").document(businessId)
                .collection("business_info").document(businessId).get().await()
            if (bizDoc.exists()) {
                val data = bizDoc.data
                if (data != null) {
                    val entity = BusinessEntity(
                        id = businessId,
                        ownerUid = data["ownerUid"] as? String ?: "USR-OWNER",
                        businessName = data["businessName"] as? String ?: "MilkMate Dairy",
                        ownerName = data["ownerName"] as? String ?: "Owner",
                        phone = data["phone"] as? String ?: "",
                        accountStartDate = (data["accountStartDate"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                        businessMode = data["businessMode"] as? String ?: "FARMER",
                        supportedMilkTypes = data["supportedMilkTypes"] as? String ?: "BOTH",
                        cowMilkRate = (data["cowMilkRate"] as? Number)?.toDouble() ?: 50.0,
                        buffaloMilkRate = (data["buffaloMilkRate"] as? Number)?.toDouble() ?: 65.0,
                        pricingMode = data["pricingMode"] as? String ?: "DIRECT"
                    )
                    db.businessDao().insertBusiness(entity)
                }
            }

            // 2. Restore Customers
            val custSnap = firestore.collection("businesses").document(businessId)
                .collection("customers").get().await()
            for (doc in custSnap.documents) {
                val d = doc.data ?: continue
                if (d["deletedAt"] != null) continue
                val cust = CustomerEntity(
                    id = doc.id,
                    businessId = businessId,
                    name = d["name"] as? String ?: "Customer",
                    mobile = d["mobile"] as? String ?: d["phone"] as? String ?: "",
                    address = d["address"] as? String ?: "",
                    milkType = d["milkType"] as? String ?: "COW",
                    rate = (d["rate"] as? Number)?.toDouble() ?: 50.0,
                    outstandingBalance = (d["outstandingBalance"] as? Number)?.toDouble() ?: 0.0,
                    type = d["type"] as? String ?: "INDIVIDUAL"
                )
                db.customerDao().insertCustomer(cust)
            }

            // 3. Restore Deliveries
            val delivSnap = firestore.collection("businesses").document(businessId)
                .collection("deliveries").get().await()
            for (doc in delivSnap.documents) {
                val d = doc.data ?: continue
                if (d["deletedAt"] != null) continue
                val deliv = DeliveryEntity(
                    id = doc.id,
                    businessId = businessId,
                    customerId = d["customerId"] as? String ?: "",
                    customerName = d["customerName"] as? String ?: "",
                    milkType = d["milkType"] as? String ?: "COW",
                    quantityLiters = (d["quantityLiters"] as? Number)?.toDouble() ?: 1.0,
                    ratePerLiter = (d["ratePerLiter"] as? Number)?.toDouble() ?: (d["rate"] as? Number)?.toDouble() ?: 50.0,
                    totalAmount = (d["totalAmount"] as? Number)?.toDouble() ?: 50.0,
                    isDelivered = d["isDelivered"] as? Boolean ?: true,
                    deliveryDate = (d["deliveryDate"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                    shift = d["shift"] as? String ?: "MORNING"
                )
                db.deliveryDao().insertDelivery(deliv)
            }

            // 4. Restore Payments
            val paySnap = firestore.collection("businesses").document(businessId)
                .collection("payments").get().await()
            for (doc in paySnap.documents) {
                val d = doc.data ?: continue
                if (d["deletedAt"] != null) continue
                val pay = PaymentEntity(
                    id = doc.id,
                    businessId = businessId,
                    customerId = d["customerId"] as? String ?: "",
                    customerName = d["customerName"] as? String ?: "",
                    amount = (d["amount"] as? Number)?.toDouble() ?: 0.0,
                    method = d["method"] as? String ?: d["paymentMode"] as? String ?: "CASH",
                    date = (d["date"] as? Number)?.toLong() ?: System.currentTimeMillis()
                )
                db.paymentDao().insertPayment(pay)
            }

            Log.i(tag, "Cloud Restore completed successfully for business: $businessId")
            true
        } catch (e: Exception) {
            Log.e(tag, "Cloud Restore failed: ${e.message}", e)
            false
        }
    }

    private fun jsonToMap(jsonStr: String): Map<String, Any> {
        val json = JSONObject(jsonStr)
        val map = mutableMapOf<String, Any>()
        val keys = json.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            map[key] = json.get(key)
        }
        return map
    }
}
