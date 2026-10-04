package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.CustomerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers WHERE businessId = :businessId AND deletedAt IS NULL ORDER BY name ASC")
    fun getAllCustomersFlow(businessId: String): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers WHERE businessId = :businessId AND type = :type AND deletedAt IS NULL ORDER BY name ASC")
    fun getCustomersByTypeFlow(businessId: String, type: String): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers WHERE businessId = :businessId AND id = :customerId AND deletedAt IS NULL LIMIT 1")
    fun getCustomerByIdFlow(businessId: String, customerId: String): Flow<CustomerEntity?>

    @Query("SELECT * FROM customers WHERE businessId = :businessId AND id = :customerId AND deletedAt IS NULL LIMIT 1")
    suspend fun getCustomerById(businessId: String, customerId: String): CustomerEntity?

    @Query("SELECT COUNT(*) FROM customers WHERE businessId = :businessId AND type = :type AND deletedAt IS NULL")
    suspend fun countCustomersByType(businessId: String, type: String): Int

    @Query("SELECT COUNT(*) FROM customers WHERE businessId = :businessId AND deletedAt IS NULL")
    suspend fun countTotalCustomers(businessId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: CustomerEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(customers: List<CustomerEntity>)

    @Update
    suspend fun updateCustomer(customer: CustomerEntity)

    @Query("UPDATE customers SET deletedAt = :deletedAt, syncStatus = 'PENDING' WHERE id = :customerId AND businessId = :businessId")
    suspend fun softDeleteCustomer(businessId: String, customerId: String, deletedAt: Long = System.currentTimeMillis())

    @Query("UPDATE customers SET outstandingBalance = outstandingBalance + :deltaAmount, updatedAt = :now, syncStatus = 'PENDING' WHERE id = :customerId AND businessId = :businessId")
    suspend fun adjustOutstandingBalance(businessId: String, customerId: String, deltaAmount: Double, now: Long = System.currentTimeMillis())

    @Query("UPDATE customers SET route = :route, assignedStaffId = :staffId, assignedStaffName = :staffName, updatedAt = :now, syncStatus = 'PENDING' WHERE id = :customerId AND businessId = :businessId")
    suspend fun allotRouteAndStaff(businessId: String, customerId: String, route: String, staffId: String, staffName: String, now: Long = System.currentTimeMillis())

    @Query("UPDATE customers SET route = :route, assignedStaffId = :staffId, assignedStaffName = :staffName, updatedAt = :now, syncStatus = 'PENDING' WHERE id IN (:customerIds) AND businessId = :businessId")
    suspend fun bulkAllotRouteAndStaff(businessId: String, customerIds: List<String>, route: String, staffId: String, staffName: String, now: Long = System.currentTimeMillis())

    @Query("SELECT * FROM customers WHERE businessId = :businessId AND deletedAt IS NOT NULL ORDER BY deletedAt DESC")
    fun getDeletedCustomersFlow(businessId: String): Flow<List<CustomerEntity>>

    @Query("UPDATE customers SET deletedAt = NULL, syncStatus = 'PENDING' WHERE id = :customerId AND businessId = :businessId")
    suspend fun restoreCustomer(businessId: String, customerId: String)

    @Query("UPDATE customers SET quickPresets = :presets, updatedAt = :now, syncStatus = 'PENDING' WHERE id = :customerId AND businessId = :businessId")
    suspend fun updateQuickPresets(businessId: String, customerId: String, presets: String, now: Long = System.currentTimeMillis())
}

