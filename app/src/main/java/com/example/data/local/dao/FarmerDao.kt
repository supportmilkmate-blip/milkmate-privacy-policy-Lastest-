package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.FarmerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FarmerDao {
    @Query("SELECT * FROM farmers WHERE businessId = :businessId AND deletedAt IS NULL ORDER BY farmerCode ASC, name ASC")
    fun getFarmersFlow(businessId: String): Flow<List<FarmerEntity>>

    @Query("SELECT * FROM farmers WHERE businessId = :businessId AND deletedAt IS NULL ORDER BY farmerCode ASC, name ASC")
    suspend fun getFarmers(businessId: String): List<FarmerEntity>

    @Query("SELECT * FROM farmers WHERE id = :id AND deletedAt IS NULL LIMIT 1")
    suspend fun getFarmerById(id: String): FarmerEntity?

    @Query("SELECT * FROM farmers WHERE businessId = :businessId AND farmerCode = :code AND deletedAt IS NULL LIMIT 1")
    suspend fun getFarmerByCode(businessId: String, code: String): FarmerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFarmer(farmer: FarmerEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFarmers(farmers: List<FarmerEntity>)

    @Update
    suspend fun updateFarmer(farmer: FarmerEntity)

    @Query("UPDATE farmers SET balancePayable = balancePayable + :amountDelta, updatedAt = :timestamp WHERE id = :farmerId")
    suspend fun updateFarmerBalance(farmerId: String, amountDelta: Double, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE farmers SET deletedAt = :deletedAt WHERE id = :farmerId")
    suspend fun softDeleteFarmer(farmerId: String, deletedAt: Long = System.currentTimeMillis())
}
