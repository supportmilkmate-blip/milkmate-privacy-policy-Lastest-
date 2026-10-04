package com.example.data.repository

import com.example.data.local.dao.CustomerDao
import com.example.data.local.entity.BusinessEntity

data class PlanDetails(
    val id: String,
    val name: String,
    val priceInr: Int,
    val billingPeriod: String, // "MONTHLY" or "YEARLY"
    val description: String,
    val playStoreProductId: String
)

sealed class CanCreateResult {
    object Allowed : CanCreateResult()
    data class Blocked(val currentCount: Int, val maxLimit: Int, val customerType: String) : CanCreateResult()
}

class BillingManager(private val customerDao: CustomerDao) {

    companion object {
        const val FREE_LIMIT_INDIVIDUAL = 25
        const val FREE_LIMIT_BULK_BUYER = 5
        const val FREE_LIMIT_SUPPLIER = 5

        val PRO_MONTHLY = PlanDetails(
            id = "pro_monthly",
            name = "Pro Monthly",
            priceInr = 49,
            billingPeriod = "MONTHLY",
            description = "Unlimited customers, bulk buyers & suppliers",
            playStoreProductId = "milkmate_pro_monthly_49"
        )

        val PRO_YEARLY = PlanDetails(
            id = "pro_yearly",
            name = "Pro Yearly",
            priceInr = 499,
            billingPeriod = "YEARLY",
            description = "Unlimited customers, bulk buyers & suppliers (Save 15%)",
            playStoreProductId = "milkmate_pro_yearly_499"
        )
    }

    /**
     * Determines whether the business currently holds an active Pro entitlement.
     */
    fun isProActive(business: BusinessEntity?): Boolean {
        if (business == null) return false
        if (business.subscriptionPlan != "PRO") return false

        // Check expiration: 0 means lifetime or non-expiring, otherwise check against current time
        val now = System.currentTimeMillis()
        if (business.subscriptionExpiresAt > 0 && business.subscriptionExpiresAt < now) {
            return false // Expired Pro entitlement
        }
        return business.subscriptionStatus == "ACTIVE"
    }

    /**
     * Validates whether a new customer of given type can be added.
     * Enforces Free limits (25 Individuals, 5 Bulk Buyers, 5 Suppliers).
     */
    suspend fun canCreateCustomer(business: BusinessEntity?, customerType: String): CanCreateResult {
        if (isProActive(business)) {
            return CanCreateResult.Allowed
        }

        val businessId = business?.id ?: return CanCreateResult.Allowed
        val count = customerDao.countCustomersByType(businessId, customerType.uppercase())

        val limit = when (customerType.uppercase()) {
            "INDIVIDUAL" -> FREE_LIMIT_INDIVIDUAL
            "BULK_BUYER" -> FREE_LIMIT_BULK_BUYER
            "SUPPLIER" -> FREE_LIMIT_SUPPLIER
            else -> FREE_LIMIT_INDIVIDUAL
        }

        return if (count < limit) {
            CanCreateResult.Allowed
        } else {
            CanCreateResult.Blocked(currentCount = count, maxLimit = limit, customerType = customerType)
        }
    }
}
