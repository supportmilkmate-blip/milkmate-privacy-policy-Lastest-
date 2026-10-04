package com.example

import com.example.data.local.entity.BusinessEntity
import com.example.data.local.entity.CustomerEntity
import com.example.data.repository.PricingEngine
import org.junit.Assert.*
import org.junit.Test

class PricingEngineTest {

    private val sampleBusiness = BusinessEntity(
        id = "BIZ-001",
        ownerUid = "USR-001",
        businessName = "Test Dairy",
        ownerName = "Ananda",
        phone = "9876543210",
        accountStartDate = 1700000000000L,
        cowMilkRate = 50.0,
        buffaloMilkRate = 65.0,
        fatBaseRate = 6.5,
        snfBaseRate = 8.5,
        fatDiffRate = 0.5,
        snfDiffRate = 0.4,
        paneerRatePerKg = 350.0,
        khoaRatePerKg = 320.0,
        pricingMode = "DIRECT"
    )

    @Test
    fun directRate_usesConfiguredRates() {
        val cowRate = PricingEngine.calculateRatePerLiter(sampleBusiness, null, "COW")
        assertEquals(50.0, cowRate, 0.001)

        val buffaloRate = PricingEngine.calculateRatePerLiter(sampleBusiness, null, "BUFFALO")
        assertEquals(65.0, buffaloRate, 0.001)
    }

    @Test
    fun customerOverrideRate_takesPrecedence() {
        val customerWithCustomRate = CustomerEntity(
            id = "CUST-001",
            businessId = "BIZ-001",
            name = "John",
            mobile = "9999999999",
            rate = 55.0
        )
        val rate = PricingEngine.calculateRatePerLiter(sampleBusiness, customerWithCustomRate, "COW")
        assertEquals(55.0, rate, 0.001)
    }

    @Test
    fun fatSnfPricing_calculatesDifferentialCorrectly() {
        val fatSnfBiz = sampleBusiness.copy(pricingMode = "FAT_SNF")
        // Base: cow 50.0, baseFat 6.5, baseSnf 8.5
        // If fat = 7.0 (diff +0.5 * 0.5 = +0.25), snf = 9.0 (diff +0.5 * 0.4 = +0.20)
        // Rate = 50.0 + 0.25 + 0.20 = 50.45
        val calculated = PricingEngine.calculateRatePerLiter(fatSnfBiz, null, "COW", fat = 7.0, snf = 9.0)
        assertEquals(50.45, calculated, 0.01)
    }

    @Test
    fun totalAmount_multipliesQuantityAndRate() {
        val total = PricingEngine.calculateTotalAmount(3.5, 52.0)
        assertEquals(182.0, total, 0.001)
    }
}
