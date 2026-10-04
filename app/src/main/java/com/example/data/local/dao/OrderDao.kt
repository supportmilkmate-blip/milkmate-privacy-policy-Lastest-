package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.OrderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OrderDao {
    @Query("SELECT * FROM orders WHERE businessId = :businessId AND deletedAt IS NULL ORDER BY deliveryDate DESC")
    fun getAllOrdersFlow(businessId: String): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE businessId = :businessId AND status = :status AND deletedAt IS NULL ORDER BY deliveryDate ASC")
    fun getOrdersByStatusFlow(businessId: String, status: String): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE businessId = :businessId AND deliveryDate >= :start AND deliveryDate <= :end AND deletedAt IS NULL")
    suspend fun getOrdersBetweenDates(businessId: String, start: Long, end: Long): List<OrderEntity>

    @Query("SELECT * FROM orders WHERE businessId = :businessId AND id = :orderId AND deletedAt IS NULL LIMIT 1")
    suspend fun getOrderById(businessId: String, orderId: String): OrderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: OrderEntity)

    @Update
    suspend fun updateOrder(order: OrderEntity)

    @Query("UPDATE orders SET status = :status, updatedAt = :now, syncStatus = 'PENDING' WHERE id = :orderId AND businessId = :businessId")
    suspend fun updateOrderStatus(businessId: String, orderId: String, status: String, now: Long = System.currentTimeMillis())

    @Query("UPDATE orders SET deletedAt = :deletedAt, syncStatus = 'PENDING' WHERE id = :orderId AND businessId = :businessId")
    suspend fun softDeleteOrder(businessId: String, orderId: String, deletedAt: Long = System.currentTimeMillis())
}
