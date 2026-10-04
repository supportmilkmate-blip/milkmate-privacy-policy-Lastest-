package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "staff")
data class StaffEntity(
    @PrimaryKey val id: String, // Staff ID (e.g. STF-101 or UUID)
    val businessId: String,
    val name: String,
    val mobile: String,
    val pin: String,
    val role: String = "DELIVERY_BOY", // MANAGER, DELIVERY_BOY, ACCOUNTANT, CUSTOM
    val permissions: String = "DELIVERY,CUSTOMERS", // Comma-delimited list of permitted modules
    val assignedRoute: String = "",
    val monthlySalary: Double = 12000.0,
    val dailyWage: Double = 400.0,
    val salaryType: String = "MONTHLY", // "MONTHLY" or "DAILY"
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val createdBy: String = "owner",
    val updatedBy: String = "owner",
    val version: Long = 1L,
    val deletedAt: Long? = null,
    val syncStatus: String = "PENDING"
) {
    fun hasPermission(permissionKey: String): Boolean {
        if (role == "MANAGER") return true
        val list = permissions.split(",").map { it.trim().uppercase() }
        return list.contains(permissionKey.uppercase()) || list.contains("ALL")
    }
}
