package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.DeliveryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DeliveryDao {
    @Query("SELECT * FROM deliveries WHERE businessId = :businessId AND deliveryDate = :date AND shift = :shift AND deletedAt IS NULL ORDER BY customerName ASC")
    fun getDeliveriesByDateAndShiftFlow(businessId: String, date: Long, shift: String): Flow<List<DeliveryEntity>>

    @Query("SELECT * FROM deliveries WHERE businessId = :businessId AND deliveryDate = :date AND shift = :shift AND deletedAt IS NULL")
    suspend fun getDeliveriesByDateAndShift(businessId: String, date: Long, shift: String): List<DeliveryEntity>

    @Query("SELECT * FROM deliveries WHERE businessId = :businessId AND deliveryDate = :date AND deletedAt IS NULL ORDER BY customerName ASC")
    fun getDeliveriesByDateFlow(businessId: String, date: Long): Flow<List<DeliveryEntity>>

    @Query("SELECT * FROM deliveries WHERE businessId = :businessId AND customerId = :customerId AND deletedAt IS NULL ORDER BY deliveryDate DESC")
    fun getDeliveriesByCustomerFlow(businessId: String, customerId: String): Flow<List<DeliveryEntity>>

    @Query("SELECT * FROM deliveries WHERE businessId = :businessId AND deliveryDate >= :startDate AND deliveryDate <= :endDate AND deletedAt IS NULL ORDER BY deliveryDate ASC")
    suspend fun getDeliveriesBetweenDates(businessId: String, startDate: Long, endDate: Long): List<DeliveryEntity>

    @Query("SELECT * FROM deliveries WHERE businessId = :businessId AND deliveryDate >= :startDate AND deliveryDate <= :endDate AND deletedAt IS NULL ORDER BY deliveryDate ASC")
    fun getDeliveriesBetweenDatesFlow(businessId: String, startDate: Long, endDate: Long): Flow<List<DeliveryEntity>>

    @Query("SELECT * FROM deliveries WHERE businessId = :businessId AND id = :deliveryId AND deletedAt IS NULL LIMIT 1")
    suspend fun getDeliveryById(businessId: String, deliveryId: String): DeliveryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDelivery(delivery: DeliveryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(deliveries: List<DeliveryEntity>)

    @Update
    suspend fun updateDelivery(delivery: DeliveryEntity)

    @Query("UPDATE deliveries SET deletedAt = :deletedAt, syncStatus = 'PENDING' WHERE id = :deliveryId AND businessId = :businessId")
    suspend fun softDeleteDelivery(businessId: String, deliveryId: String, deletedAt: Long = System.currentTimeMillis())

    @Query("SELECT SUM(quantityLiters) FROM deliveries WHERE businessId = :businessId AND deliveryDate = :date AND shift = :shift AND customerType != 'SUPPLIER' AND isDelivered = 1 AND deletedAt IS NULL")
    fun getTotalDispatchedMilkFlow(businessId: String, date: Long, shift: String): Flow<Double?>

    @Query("SELECT SUM(quantityLiters) FROM deliveries WHERE businessId = :businessId AND deliveryDate = :date AND customerType = 'SUPPLIER' AND isDelivered = 1 AND deletedAt IS NULL")
    fun getTotalCollectedMilkFlow(businessId: String, date: Long): Flow<Double?>
}
