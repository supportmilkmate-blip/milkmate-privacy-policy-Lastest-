package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.InventoryItemEntity
import com.example.data.local.entity.InventoryMonthClosingEntity
import com.example.data.local.entity.InventoryTransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InventoryDao {
    @Query("SELECT * FROM inventory_items WHERE businessId = :businessId AND deletedAt IS NULL ORDER BY itemName ASC")
    fun getAllInventoryItemsFlow(businessId: String): Flow<List<InventoryItemEntity>>

    @Query("SELECT * FROM inventory_items WHERE businessId = :businessId AND deletedAt IS NULL ORDER BY itemName ASC")
    suspend fun getAllInventoryItems(businessId: String): List<InventoryItemEntity>

    @Query("SELECT * FROM inventory_items WHERE businessId = :businessId AND id = :itemId AND deletedAt IS NULL LIMIT 1")
    suspend fun getInventoryItemById(businessId: String, itemId: String): InventoryItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: InventoryItemEntity)

    @Update
    suspend fun updateItem(item: InventoryItemEntity)

    @Query("UPDATE inventory_items SET currentStock = currentStock + :deltaQuantity, updatedAt = :now, syncStatus = 'PENDING' WHERE id = :itemId AND businessId = :businessId")
    suspend fun adjustCurrentStock(businessId: String, itemId: String, deltaQuantity: Double, now: Long = System.currentTimeMillis())

    @Query("UPDATE inventory_items SET currentStock = :newStock, updatedAt = :now, syncStatus = 'PENDING' WHERE id = :itemId AND businessId = :businessId")
    suspend fun setCurrentStock(businessId: String, itemId: String, newStock: Double, now: Long = System.currentTimeMillis())

    @Query("UPDATE inventory_items SET dailyUsage = :dailyUsage, updatedAt = :now, syncStatus = 'PENDING' WHERE id = :itemId AND businessId = :businessId")
    suspend fun updateDailyUsage(businessId: String, itemId: String, dailyUsage: Double?, now: Long = System.currentTimeMillis())

    @Query("UPDATE inventory_items SET deletedAt = :deletedAt, syncStatus = 'PENDING' WHERE id = :itemId AND businessId = :businessId")
    suspend fun softDeleteItem(businessId: String, itemId: String, deletedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM inventory_items WHERE businessId = :businessId AND id = :itemId")
    suspend fun deleteItemPermanently(businessId: String, itemId: String)

    @Query("DELETE FROM inventory_items WHERE businessId = :businessId AND id IN (:itemIds)")
    suspend fun deleteItemsPermanently(businessId: String, itemIds: List<String>)

    // Transactions
    @Query("SELECT * FROM inventory_transactions WHERE businessId = :businessId AND deletedAt IS NULL ORDER BY date DESC, createdAt DESC")
    fun getAllTransactionsFlow(businessId: String): Flow<List<InventoryTransactionEntity>>

    @Query("SELECT * FROM inventory_transactions WHERE businessId = :businessId AND deletedAt IS NULL ORDER BY date DESC, createdAt DESC")
    suspend fun getAllTransactions(businessId: String): List<InventoryTransactionEntity>

    @Query("SELECT * FROM inventory_transactions WHERE linkedExpenseId = :linkedExpenseId AND deletedAt IS NULL LIMIT 1")
    suspend fun getTransactionByLinkedExpenseId(linkedExpenseId: String): InventoryTransactionEntity?

    @Query("SELECT * FROM inventory_transactions WHERE businessId = :businessId AND deletedAt IS NULL AND date >= :startDate AND date <= :endDate ORDER BY date DESC, createdAt DESC")
    fun getTransactionsBetweenDatesFlow(businessId: String, startDate: Long, endDate: Long): Flow<List<InventoryTransactionEntity>>

    @Query("SELECT * FROM inventory_transactions WHERE businessId = :businessId AND deletedAt IS NULL AND date >= :startDate AND date <= :endDate ORDER BY date DESC, createdAt DESC")
    suspend fun getTransactionsBetweenDates(businessId: String, startDate: Long, endDate: Long): List<InventoryTransactionEntity>

    @Query("SELECT * FROM inventory_transactions WHERE businessId = :businessId AND itemId = :itemId AND deletedAt IS NULL ORDER BY date ASC, createdAt ASC")
    fun getTransactionsByItemFlow(businessId: String, itemId: String): Flow<List<InventoryTransactionEntity>>

    @Query("SELECT * FROM inventory_transactions WHERE businessId = :businessId AND itemId = :itemId AND deletedAt IS NULL ORDER BY date ASC, createdAt ASC")
    suspend fun getTransactionsByItem(businessId: String, itemId: String): List<InventoryTransactionEntity>

    @Query("SELECT * FROM inventory_transactions WHERE id = :transactionId AND deletedAt IS NULL LIMIT 1")
    suspend fun getTransactionById(transactionId: String): InventoryTransactionEntity?

    @Query("SELECT * FROM inventory_transactions WHERE businessId = :businessId AND id = :transactionId AND deletedAt IS NULL LIMIT 1")
    suspend fun getTransactionById(businessId: String, transactionId: String): InventoryTransactionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: InventoryTransactionEntity)

    @Update
    suspend fun updateTransaction(transaction: InventoryTransactionEntity)

    @Query("UPDATE inventory_transactions SET deletedAt = :deletedAt, syncStatus = 'PENDING' WHERE id = :transactionId")
    suspend fun softDeleteTransaction(transactionId: String, deletedAt: Long = System.currentTimeMillis())

    // Month Closings
    @Query("SELECT * FROM inventory_month_closings WHERE businessId = :businessId AND yearMonth = :yearMonth")
    fun getMonthClosingsFlow(businessId: String, yearMonth: String): Flow<List<InventoryMonthClosingEntity>>

    @Query("SELECT * FROM inventory_month_closings WHERE businessId = :businessId AND yearMonth = :yearMonth")
    suspend fun getMonthClosings(businessId: String, yearMonth: String): List<InventoryMonthClosingEntity>

    @Query("SELECT * FROM inventory_month_closings WHERE businessId = :businessId AND itemId = :itemId AND yearMonth = :yearMonth LIMIT 1")
    suspend fun getMonthClosing(businessId: String, itemId: String, yearMonth: String): InventoryMonthClosingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMonthClosing(closing: InventoryMonthClosingEntity)

    @Query("SELECT SUM(currentStock * costPerUnit) FROM inventory_items WHERE businessId = :businessId AND deletedAt IS NULL")
    fun getStockValuationFlow(businessId: String): Flow<Double?>
}
