package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.PaymentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentDao {
    @Query("SELECT * FROM payments WHERE businessId = :businessId AND deletedAt IS NULL ORDER BY date DESC")
    fun getAllPaymentsFlow(businessId: String): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments WHERE businessId = :businessId AND customerId = :customerId AND deletedAt IS NULL ORDER BY date DESC")
    fun getPaymentsByCustomerFlow(businessId: String, customerId: String): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments WHERE businessId = :businessId AND date >= :startDate AND date <= :endDate AND deletedAt IS NULL ORDER BY date DESC")
    suspend fun getPaymentsBetweenDates(businessId: String, startDate: Long, endDate: Long): List<PaymentEntity>

    @Query("SELECT * FROM payments WHERE businessId = :businessId AND date >= :startDate AND date <= :endDate AND deletedAt IS NULL ORDER BY date DESC")
    fun getPaymentsBetweenDatesFlow(businessId: String, startDate: Long, endDate: Long): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments WHERE businessId = :businessId AND id = :paymentId AND deletedAt IS NULL LIMIT 1")
    suspend fun getPaymentById(businessId: String, paymentId: String): PaymentEntity?

    @Query("SELECT SUM(amount) FROM payments WHERE businessId = :businessId AND paymentType = 'CUSTOMER_PAYMENT' AND date >= :startDate AND date <= :endDate AND deletedAt IS NULL")
    fun getTotalCustomerPaymentsFlow(businessId: String, startDate: Long, endDate: Long): Flow<Double?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: PaymentEntity)

    @Update
    suspend fun updatePayment(payment: PaymentEntity)

    @Query("UPDATE payments SET deletedAt = :deletedAt, syncStatus = 'PENDING' WHERE id = :paymentId AND businessId = :businessId")
    suspend fun softDeletePayment(businessId: String, paymentId: String, deletedAt: Long = System.currentTimeMillis())
}
