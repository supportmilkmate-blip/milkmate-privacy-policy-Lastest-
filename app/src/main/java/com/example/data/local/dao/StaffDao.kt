package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.StaffAttendanceEntity
import com.example.data.local.entity.StaffEntity
import com.example.data.local.entity.StaffPaymentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StaffDao {
    @Query("SELECT * FROM staff WHERE businessId = :businessId AND deletedAt IS NULL ORDER BY name ASC")
    fun getAllStaffFlow(businessId: String): Flow<List<StaffEntity>>

    @Query("SELECT * FROM staff WHERE businessId = :businessId AND id = :staffId AND deletedAt IS NULL LIMIT 1")
    suspend fun getStaffById(businessId: String, staffId: String): StaffEntity?

    @Query("SELECT * FROM staff WHERE id = :staffId AND pin = :pin AND isActive = 1 AND deletedAt IS NULL LIMIT 1")
    suspend fun authenticateStaff(staffId: String, pin: String): StaffEntity?

    @Query("SELECT * FROM staff WHERE businessId = :businessId AND (id = :identifier OR mobile = :identifier) AND pin = :pin AND isActive = 1 AND deletedAt IS NULL LIMIT 1")
    suspend fun authenticateStaffInBusiness(businessId: String, identifier: String, pin: String): StaffEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStaff(staff: StaffEntity)

    @Update
    suspend fun updateStaff(staff: StaffEntity)

    @Query("UPDATE staff SET deletedAt = :deletedAt, syncStatus = 'PENDING' WHERE id = :staffId AND businessId = :businessId")
    suspend fun softDeleteStaff(businessId: String, staffId: String, deletedAt: Long = System.currentTimeMillis())

    // --- Attendance ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendance(attendance: StaffAttendanceEntity)

    @Query("SELECT * FROM staff_attendance WHERE businessId = :businessId AND date = :date")
    fun getAttendanceForDateFlow(businessId: String, date: Long): Flow<List<StaffAttendanceEntity>>

    @Query("SELECT * FROM staff_attendance WHERE businessId = :businessId AND date >= :startDate AND date <= :endDate")
    fun getAttendanceForRangeFlow(businessId: String, startDate: Long, endDate: Long): Flow<List<StaffAttendanceEntity>>

    @Query("SELECT * FROM staff_attendance WHERE businessId = :businessId ORDER BY date DESC")
    fun getAllAttendanceFlow(businessId: String): Flow<List<StaffAttendanceEntity>>

    @Query("DELETE FROM staff_attendance WHERE id = :id")
    suspend fun deleteAttendance(id: String)

    // --- Salary & Advance Payments ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStaffPayment(payment: StaffPaymentEntity)

    @Query("SELECT * FROM staff_payments WHERE businessId = :businessId ORDER BY date DESC")
    fun getAllStaffPaymentsFlow(businessId: String): Flow<List<StaffPaymentEntity>>

    @Query("SELECT * FROM staff_payments WHERE businessId = :businessId AND monthYear = :monthYear ORDER BY date DESC")
    fun getStaffPaymentsForMonthFlow(businessId: String, monthYear: String): Flow<List<StaffPaymentEntity>>

    @Query("DELETE FROM staff_payments WHERE id = :id")
    suspend fun deleteStaffPayment(id: String)
}
