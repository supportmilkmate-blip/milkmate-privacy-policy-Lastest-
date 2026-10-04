package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.BusinessEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BusinessDao {
    @Query("SELECT * FROM businesses WHERE id = :businessId AND deletedAt IS NULL LIMIT 1")
    fun getBusinessFlow(businessId: String): Flow<BusinessEntity?>

    @Query("SELECT * FROM businesses WHERE id = :businessId AND deletedAt IS NULL LIMIT 1")
    suspend fun getBusiness(businessId: String): BusinessEntity?

    @Query("SELECT * FROM businesses WHERE id = :businessId AND deletedAt IS NULL LIMIT 1")
    suspend fun getBusinessById(businessId: String): BusinessEntity?

    @Query("SELECT * FROM businesses WHERE ownerUid = :ownerUid AND deletedAt IS NULL LIMIT 1")
    suspend fun getBusinessByOwner(ownerUid: String): BusinessEntity?

    @Query("SELECT * FROM businesses WHERE phone = :phone AND deletedAt IS NULL LIMIT 1")
    suspend fun getBusinessByPhone(phone: String): BusinessEntity?

    @Query("SELECT * FROM businesses WHERE deletedAt IS NULL")
    suspend fun getAllBusinesses(): List<BusinessEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBusiness(business: BusinessEntity)

    @Update
    suspend fun updateBusiness(business: BusinessEntity)

    @Query("UPDATE businesses SET syncStatus = :syncStatus WHERE id = :businessId")
    suspend fun updateSyncStatus(businessId: String, syncStatus: String)
}
