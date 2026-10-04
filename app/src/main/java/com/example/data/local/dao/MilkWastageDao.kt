package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.MilkWastageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MilkWastageDao {

    @Query("SELECT * FROM milk_wastage WHERE businessId = :businessId ORDER BY date DESC, createdAt DESC")
    fun getAllWastage(businessId: String): Flow<List<MilkWastageEntity>>

    @Query("SELECT * FROM milk_wastage WHERE businessId = :businessId AND date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    suspend fun getWastageBetweenDates(businessId: String, startDate: Long, endDate: Long): List<MilkWastageEntity>

    @Query("SELECT * FROM milk_wastage WHERE businessId = :businessId AND date = :date")
    suspend fun getWastageByDate(businessId: String, date: Long): List<MilkWastageEntity>

    @Query("SELECT SUM(quantityLiters) FROM milk_wastage WHERE businessId = :businessId AND date BETWEEN :startDate AND :endDate")
    suspend fun getTotalWastageLitersBetweenDates(businessId: String, startDate: Long, endDate: Long): Double?

    @Query("SELECT SUM(totalLossAmount) FROM milk_wastage WHERE businessId = :businessId AND date BETWEEN :startDate AND :endDate")
    suspend fun getTotalWastageLossAmountBetweenDates(businessId: String, startDate: Long, endDate: Long): Double?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWastage(wastage: MilkWastageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(wastageList: List<MilkWastageEntity>)

    @Update
    suspend fun updateWastage(wastage: MilkWastageEntity)

    @Delete
    suspend fun deleteWastage(wastage: MilkWastageEntity)

    @Query("DELETE FROM milk_wastage WHERE id = :id AND businessId = :businessId")
    suspend fun deleteById(businessId: String, id: String)
}
