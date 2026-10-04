package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.ExpenseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses WHERE businessId = :businessId AND deletedAt IS NULL ORDER BY date DESC")
    fun getAllExpensesFlow(businessId: String): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE businessId = :businessId AND date >= :startDate AND deletedAt IS NULL ORDER BY date DESC")
    fun getExpensesFromDateFlow(businessId: String, startDate: Long): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE businessId = :businessId AND date >= :startDate AND date <= :endDate AND deletedAt IS NULL ORDER BY date DESC")
    suspend fun getExpensesBetweenDates(businessId: String, startDate: Long, endDate: Long): List<ExpenseEntity>

    @Query("SELECT * FROM expenses WHERE businessId = :businessId AND date >= :startDate AND date <= :endDate AND deletedAt IS NULL ORDER BY date DESC")
    fun getExpensesBetweenDatesFlow(businessId: String, startDate: Long, endDate: Long): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE businessId = :businessId AND id = :expenseId AND deletedAt IS NULL LIMIT 1")
    suspend fun getExpenseById(businessId: String, expenseId: String): ExpenseEntity?

    @Query("SELECT SUM(amount) FROM expenses WHERE businessId = :businessId AND date >= :startDate AND date <= :endDate AND deletedAt IS NULL")
    fun getTotalExpensesFlow(businessId: String, startDate: Long, endDate: Long): Flow<Double?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseEntity)

    @Update
    suspend fun updateExpense(expense: ExpenseEntity)

    @Query("UPDATE expenses SET deletedAt = :deletedAt, syncStatus = 'PENDING' WHERE id = :expenseId AND businessId = :businessId")
    suspend fun softDeleteExpense(businessId: String, expenseId: String, deletedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM expenses WHERE id = :expenseId AND businessId = :businessId")
    suspend fun deleteExpensePermanently(businessId: String, expenseId: String)

    @Query("SELECT * FROM expenses WHERE businessId = :businessId AND deletedAt IS NOT NULL ORDER BY deletedAt DESC")
    fun getDeletedExpensesFlow(businessId: String): Flow<List<ExpenseEntity>>

    @Query("UPDATE expenses SET deletedAt = NULL, syncStatus = 'PENDING' WHERE id = :expenseId AND businessId = :businessId")
    suspend fun restoreExpense(businessId: String, expenseId: String)
}
