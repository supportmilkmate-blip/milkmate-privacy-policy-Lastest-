package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.MilkCollectionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MilkCollectionDao {
    @Query("SELECT * FROM milk_collections WHERE businessId = :businessId AND deletedAt IS NULL ORDER BY timestamp DESC")
    fun getAllMilkCollectionsFlow(businessId: String): Flow<List<MilkCollectionEntity>>

    @Query("SELECT * FROM milk_collections WHERE businessId = :businessId AND dateEpochMidnight = :dateEpoch AND deletedAt IS NULL ORDER BY timestamp DESC")
    fun getMilkCollectionsByDateFlow(businessId: String, dateEpoch: Long): Flow<List<MilkCollectionEntity>>

    @Query("SELECT * FROM milk_collections WHERE businessId = :businessId AND dateEpochMidnight = :dateEpoch AND shift = :shift AND deletedAt IS NULL ORDER BY timestamp DESC")
    fun getMilkCollectionsByDateAndShiftFlow(businessId: String, dateEpoch: Long, shift: String): Flow<List<MilkCollectionEntity>>

    @Query("SELECT * FROM milk_collections WHERE farmerId = :farmerId AND deletedAt IS NULL ORDER BY timestamp DESC")
    fun getMilkCollectionsByFarmerFlow(farmerId: String): Flow<List<MilkCollectionEntity>>

    @Query("SELECT * FROM milk_collections WHERE businessId = :businessId AND deletedAt IS NULL ORDER BY timestamp DESC")
    suspend fun getAllMilkCollections(businessId: String): List<MilkCollectionEntity>

    @Query("SELECT * FROM milk_collections WHERE businessId = :businessId AND dateEpochMidnight = :dateEpoch AND deletedAt IS NULL")
    suspend fun getMilkCollectionsByDate(businessId: String, dateEpoch: Long): List<MilkCollectionEntity>

    @Query("SELECT * FROM milk_collections WHERE businessId = :businessId AND dateEpochMidnight >= :startDate AND dateEpochMidnight <= :endDate AND deletedAt IS NULL")
    suspend fun getMilkCollectionsBetweenDates(businessId: String, startDate: Long, endDate: Long): List<MilkCollectionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMilkCollection(collection: MilkCollectionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMilkCollections(collections: List<MilkCollectionEntity>)

    @Update
    suspend fun updateMilkCollection(collection: MilkCollectionEntity)

    @Query("UPDATE milk_collections SET paymentStatus = :status, paymentReference = :ref, updatedAt = :timestamp WHERE id = :id")
    suspend fun updatePaymentStatus(id: String, status: String, ref: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE milk_collections SET paymentStatus = 'PAID', paymentReference = :ref, updatedAt = :timestamp WHERE farmerId = :farmerId AND paymentStatus = 'PENDING'")
    suspend fun markAllFarmerCollectionsPaid(farmerId: String, ref: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE milk_collections SET deletedAt = :deletedAt WHERE id = :id")
    suspend fun softDeleteMilkCollection(id: String, deletedAt: Long = System.currentTimeMillis())
}
