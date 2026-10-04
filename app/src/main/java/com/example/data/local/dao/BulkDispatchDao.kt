package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.BulkDispatchEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BulkDispatchDao {
    @Query("SELECT * FROM bulk_dispatches WHERE businessId = :businessId AND deletedAt IS NULL ORDER BY timestamp DESC")
    fun getAllDispatchesFlow(businessId: String): Flow<List<BulkDispatchEntity>>

    @Query("SELECT * FROM bulk_dispatches WHERE businessId = :businessId AND dateEpochMidnight = :dateEpoch AND deletedAt IS NULL ORDER BY timestamp DESC")
    fun getDispatchesByDateFlow(businessId: String, dateEpoch: Long): Flow<List<BulkDispatchEntity>>

    @Query("SELECT * FROM bulk_dispatches WHERE businessId = :businessId AND dateEpochMidnight = :dateEpoch AND shift = :shift AND deletedAt IS NULL ORDER BY timestamp DESC")
    fun getDispatchesByDateAndShiftFlow(businessId: String, dateEpoch: Long, shift: String): Flow<List<BulkDispatchEntity>>

    @Query("SELECT * FROM bulk_dispatches WHERE businessId = :businessId AND deletedAt IS NULL ORDER BY timestamp DESC")
    suspend fun getAllDispatches(businessId: String): List<BulkDispatchEntity>

    @Query("SELECT * FROM bulk_dispatches WHERE businessId = :businessId AND dateEpochMidnight >= :startDate AND dateEpochMidnight <= :endDate AND deletedAt IS NULL")
    suspend fun getDispatchesBetweenDates(businessId: String, startDate: Long, endDate: Long): List<BulkDispatchEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDispatch(dispatch: BulkDispatchEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDispatches(dispatches: List<BulkDispatchEntity>)

    @Update
    suspend fun updateDispatch(dispatch: BulkDispatchEntity)

    @Query("UPDATE bulk_dispatches SET deletedAt = :deletedAt WHERE id = :id")
    suspend fun softDeleteDispatch(id: String, deletedAt: Long = System.currentTimeMillis())
}
