package com.example.ui.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.data.local.entity.BusinessEntity
import com.example.ui.theme.*

data class DairyBusinessType(
    val code: String,
    val title: String,
    val shortName: String,
    val badge: String,
    val subtitle: String,
    val description: String,
    val icon: ImageVector,
    val themeColor: Color,
    val showHerdBreeding: Boolean,
    val showMilkingProduction: Boolean,
    val showCattleFeed: Boolean,
    val showProductInventory: Boolean,
    val showFarmerProcurement: Boolean,
    val showRetailRoutes: Boolean,
    val showBilling: Boolean,
    val showBulkDispatches: Boolean,
    val showRetailPos: Boolean,
    val keyFeatures: List<String>,
    val hiddenFeatures: List<String>
) {
    val showInventoryManagement: Boolean
        get() = showCattleFeed || showProductInventory
    val showCollectionDesk: Boolean
        get() = showFarmerProcurement
    val showDeliveryRoutes: Boolean
        get() = showRetailRoutes
}

object BusinessModeFeatures {

    val ALL_TYPES = listOf(
        // 1. Farmer selling to Collection Center / Trader / BMC (No household delivery)
        DairyBusinessType(
            code = "FARMER_WHOLESALE",
            title = "Dairy Farmer (Wholesale / Center)",
            shortName = "Producer (Center)",
            badge = "🚜 Farm to Center",
            subtitle = "Own cattle herd & supply milk to Collection Center or Trader",
            description = "For dairy farmers who produce milk from their own cows/buffalos and supply it daily to a Dairy Collection Centre, BMC Chilling Point, Trader, or Dairy Plant.",
            icon = Icons.Default.Agriculture,
            themeColor = GrassGreen,
            showHerdBreeding = true,
            showMilkingProduction = true,
            showCattleFeed = true,
            showProductInventory = false,
            showFarmerProcurement = false,
            showRetailRoutes = false,
            showBilling = false,
            showBulkDispatches = true,
            showRetailPos = false,
            keyFeatures = listOf(
                "Cattle Milking Yield & Lactation Tracking",
                "Cattle Feed, Silage & Fodder Inventory",
                "Daily Milk Supply Slips to Collection Center / Trader",
                "FAT, SNF & Rate Received Ledger",
                "Cattle Feed, Fodder & Veterinary Expenses"
            ),
            hiddenFeatures = listOf(
                "Household Door-to-Door Delivery Routes",
                "Individual Retail Customer Monthly Khatas",
                "Village Farmer Procurement Desk"
            )
        ),

        // 2. Farmer selling to Household Customers + Surplus to Center
        DairyBusinessType(
            code = "FARMER",
            title = "Dairy Farmer (Direct Retail + Center)",
            shortName = "Farmer & Retail",
            badge = "🚜 Producer & Seller",
            subtitle = "Own herd + deliver to households & sell surplus to Center/Trader",
            description = "For farmers who produce milk from their own cattle, deliver milk to household customer subscription routes, and deliver excess milk to a collection center or trader.",
            icon = Icons.Default.Agriculture,
            themeColor = GrassGreen,
            showHerdBreeding = true,
            showMilkingProduction = true,
            showCattleFeed = true,
            showProductInventory = false,
            showFarmerProcurement = false,
            showRetailRoutes = true,
            showBilling = true,
            showBulkDispatches = true,
            showRetailPos = false,
            keyFeatures = listOf(
                "Cattle Milking Yield & Lactation Stages",
                "Cattle Feed, Fodder & Feed Inventory",
                "Morning & Evening Doorstep Delivery Routes",
                "Monthly Customer Billing & WhatsApp Invoices",
                "Surplus Milk Dispatch to Collection Center"
            ),
            hiddenFeatures = listOf(
                "Village Farmer Milk Collection Desk",
                "Bulk Tanker Logistics"
            )
        ),

        // 3. Milk Trader / Vendor / Wholesaler (Collects from Farmers -> Sells to Bulk & Households)
        DairyBusinessType(
            code = "TRADER",
            title = "Milk Trader / Vendor / Wholesaler",
            shortName = "Milk Trader",
            badge = "🚚 Trader & Vendor",
            subtitle = "Procure milk from farmers & distribute to bulk buyers, hotels & households",
            description = "For milk traders, vendors, and aggregators who do NOT keep cattle, but purchase milk from local village farmers and distribute to bulk buyers, tea stalls, restaurants, and retail customers.",
            icon = Icons.Default.LocalShipping,
            themeColor = GoldenOrange,
            showHerdBreeding = false,
            showMilkingProduction = false,
            showCattleFeed = false,
            showProductInventory = false,
            showFarmerProcurement = true,
            showRetailRoutes = true,
            showBilling = true,
            showBulkDispatches = true,
            showRetailPos = false,
            keyFeatures = listOf(
                "Farmer Milk Collection Desk (FAT, SNF, Rate Chart)",
                "Farmer Passbooks & 1-Tap Cash/UPI Payouts",
                "Commercial B2B Accounts (Hotels, Cafes, Bulk Buyers)",
                "Household Delivery Routes & Trading Margin Spread",
                "Procurement vs Sales Volume Reconciliation"
            ),
            hiddenFeatures = listOf(
                "Cattle Herd, Cows & Buffalo Breeding",
                "Farm Milking Yield Logs",
                "Cattle Feed, Silage & Fodder Stock"
            )
        ),

        // 4. Milk Collection Centre / BMC / Chilling Center (Collects from Farmers -> Supplies Factory)
        DairyBusinessType(
            code = "COLLECTION_CENTER",
            title = "Milk Collection Centre / BMC",
            shortName = "Collection Centre",
            badge = "🏢 BMC Center",
            subtitle = "Procure milk from village farmers & supply factory tankers",
            description = "For village milk collection centres, BMC chilling points, and societies that procure milk from hundreds of farmers, perform FAT/SNF testing, and dispatch bulk tankers to dairy factories.",
            icon = Icons.Default.LocalDrink,
            themeColor = RoyalBluePrimary,
            showHerdBreeding = false,
            showMilkingProduction = false,
            showCattleFeed = false,
            showProductInventory = false,
            showFarmerProcurement = true,
            showRetailRoutes = false,
            showBilling = false,
            showBulkDispatches = true,
            showRetailPos = false,
            keyFeatures = listOf(
                "High-Speed Village Farmer Collection Desk (FAT, SNF, CLR)",
                "Farmer Passbooks, Deductions & Payout Sheets",
                "Bulk Tanker & Chilling Plant Outward Challans",
                "Center Spread: (Factory Sales − Farmer Payouts)"
            ),
            hiddenFeatures = listOf(
                "Door-to-Door Household Delivery Routes",
                "Consumer Household Monthly Khatas",
                "Cattle Herd & Milking Logs",
                "Cattle Feed Stock"
            )
        ),

        // 5. Integrated Producer & Aggregator (Own Herd + Sells to Individuals + Collects from Nearby Farmers)
        DairyBusinessType(
            code = "INTEGRATED",
            title = "Integrated Producer & Collector",
            shortName = "Integrated Farm",
            badge = "🌐 Farm & Aggregator",
            subtitle = "Own Farm + Collect from Farmers + Retail Routes + Bulk Supply",
            description = "For progressive dairy businesses with their own cattle farm who also collect milk from nearby farmers, distribute to household customers, and supply bulk buyers.",
            icon = Icons.Default.Hub,
            themeColor = DeepOceanNavy,
            showHerdBreeding = true,
            showMilkingProduction = true,
            showCattleFeed = true,
            showProductInventory = true,
            showFarmerProcurement = true,
            showRetailRoutes = true,
            showBilling = true,
            showBulkDispatches = true,
            showRetailPos = true,
            keyFeatures = listOf(
                "Own Cattle Milking Yields & Herd Breeding",
                "Village Farmer Milk Procurement Desk (FAT/SNF)",
                "Doorstep Customer Delivery Routes & Monthly Invoices",
                "Bulk Dispatches & Dairy Product Processing (Paneer, Ghee)"
            ),
            hiddenFeatures = emptyList()
        ),

        // 6. Dairy Processing & Byproducts Unit
        DairyBusinessType(
            code = "PROCESSING_UNIT",
            title = "Dairy Processing & Byproducts Unit",
            shortName = "Dairy Processor",
            badge = "🧀 Dairy Processor",
            subtitle = "Process raw milk into Paneer, Ghee, Butter, Curd & Sweets",
            description = "For dairy processing units that intake raw milk batches and manufacture value-added dairy byproducts with conversion yields, cold storage, and distributor invoicing.",
            icon = Icons.Default.Kitchen,
            themeColor = WarmHoney,
            showHerdBreeding = false,
            showMilkingProduction = false,
            showCattleFeed = false,
            showProductInventory = true,
            showFarmerProcurement = false,
            showRetailRoutes = false,
            showBilling = true,
            showBulkDispatches = true,
            showRetailPos = false,
            keyFeatures = listOf(
                "Full Dairy Inventory (Paneer, Ghee, Curd, Butter, Khoya)",
                "Raw Milk-to-Product Conversion Yield Calculations",
                "Wholesale Merchant & Distributor Invoicing",
                "Stock Expiry Alerts & Batch Cold Storage"
            ),
            hiddenFeatures = listOf(
                "Cattle Herd & Milking Logs",
                "Cattle Feed Stock",
                "Door-to-door Household Delivery Routes",
                "Village Farmer Procurement Desk"
            )
        ),

        // 7. Dairy Retail Parlour / Milk Booth
        DairyBusinessType(
            code = "RETAIL_PARLOUR",
            title = "Dairy Parlour & Milk Booth",
            shortName = "Dairy Parlour",
            badge = "🛒 Retail Dairy Booth",
            subtitle = "Walk-in shop counter sales of milk, curd, paneer & sweets",
            description = "For retail dairy shops, milk booths, and parlours selling packaged pouches, loose dispenser milk, paneer, dahi, ghee, and sweets with fast counter billing and instant UPI POS.",
            icon = Icons.Default.Storefront,
            themeColor = Color(0xFF8E24AA),
            showHerdBreeding = false,
            showMilkingProduction = false,
            showCattleFeed = false,
            showProductInventory = true,
            showFarmerProcurement = false,
            showRetailRoutes = false,
            showBilling = true,
            showBulkDispatches = false,
            showRetailPos = true,
            keyFeatures = listOf(
                "Instant Walk-in Cash & UPI Counter POS Billing",
                "Daily Product Inventory (Milk Pouches, Paneer, Curd)",
                "Counter Cash Drawer Reconciliation & Register Closing",
                "Instant Printed / WhatsApp Counter Receipts"
            ),
            hiddenFeatures = listOf(
                "Cattle Herd Breeding & Milking Yield Logs",
                "Cattle Feed Stock",
                "Village Farmer Fat/SNF Procurement Desk",
                "Morning & Evening Door-to-Door Delivery Routes"
            )
        ),

        // 8. Gaushala & Desi A2 Organic Farm
        DairyBusinessType(
            code = "GAUSHALA",
            title = "Gaushala & Desi A2 Farm",
            shortName = "Gaushala",
            badge = "🐮 Gaushala / A2 Farm",
            subtitle = "Pure indigenous Desi cows (Gir/Sahiwal), A2 milk & seva",
            description = "For Gaushalas, Trusts, and ethical A2 organic farms managing indigenous cattle breeds, A2 organic milk distribution, Panchagavya products, and patron donations.",
            icon = Icons.Default.Pets,
            themeColor = Color(0xFF2E7D32),
            showHerdBreeding = true,
            showMilkingProduction = true,
            showCattleFeed = true,
            showProductInventory = true,
            showFarmerProcurement = false,
            showRetailRoutes = true,
            showBilling = true,
            showBulkDispatches = false,
            showRetailPos = false,
            keyFeatures = listOf(
                "Desi Cow Heritage Pedigree, Tagging & Medical Care",
                "Pure A2 Milk Subscription Batch Allotments",
                "Panchagavya, Organic Ghee & Bio-Product Sales",
                "Donor / Patron Khata & Seva Contribution Tracking"
            ),
            hiddenFeatures = listOf(
                "Village Farmer Fat/SNF Procurement Desk",
                "Bulk Tanker Chilling Dispatches"
            )
        ),

        // 9. Custom / Build Your Own Dairy Setup
        DairyBusinessType(
            code = "CUSTOM",
            title = "Custom Dairy Setup (Build Your Own)",
            shortName = "Custom Setup",
            badge = "🛠️ Custom Setup",
            subtitle = "Custom modular setup tailored to your exact business requirements",
            description = "For dairy businesses with unique workflows. Pick and choose exactly which sourcing, distribution, byproducts, and billing features you need.",
            icon = Icons.Default.Tune,
            themeColor = Color(0xFF00897B),
            showHerdBreeding = true,
            showMilkingProduction = true,
            showCattleFeed = true,
            showProductInventory = true,
            showFarmerProcurement = true,
            showRetailRoutes = true,
            showBilling = true,
            showBulkDispatches = true,
            showRetailPos = true,
            keyFeatures = listOf(
                "Fully Customizable Sourcing & Distribution Channels",
                "Granular Feature Toggles for Every Screen & Tab",
                "Selective Rate Charts & Byproduct Conversion Yields",
                "Tailored to Your Exact Business Workflow"
            ),
            hiddenFeatures = emptyList()
        )
    )

    fun getByType(code: String?): DairyBusinessType {
        return ALL_TYPES.find { it.code == code }
            ?: if (code == "FARMER_RETAIL_WHOLESALE") ALL_TYPES.find { it.code == "FARMER" } ?: ALL_TYPES.first()
            else ALL_TYPES.first()
    }

    fun computeModeFromCapabilities(
        sourceOwnCattle: Boolean,
        sourceVillageFarmers: Boolean,
        destHouseholds: Boolean,
        destCollectionCenter: Boolean,
        destBulkCommercial: Boolean,
        destFactoryTankers: Boolean,
        explicitMode: String? = null
    ): String {
        if (explicitMode == "CUSTOM") return "CUSTOM"
        return when {
            sourceOwnCattle && sourceVillageFarmers -> "INTEGRATED"
            !sourceOwnCattle && sourceVillageFarmers && destFactoryTankers -> "COLLECTION_CENTER"
            !sourceOwnCattle && sourceVillageFarmers -> "TRADER"
            sourceOwnCattle && !destHouseholds && (destCollectionCenter || destBulkCommercial || destFactoryTankers) -> "FARMER_WHOLESALE"
            sourceOwnCattle && destHouseholds -> "FARMER"
            !sourceOwnCattle && !sourceVillageFarmers && destBulkCommercial -> "PROCESSING_UNIT"
            else -> explicitMode ?: "FARMER"
        }
    }

    fun showHerdBreeding(business: BusinessEntity?): Boolean {
        if (business == null) return false
        val mode = business.businessMode
        if (mode in listOf("RETAIL_PARLOUR", "TRADER", "COLLECTION_CENTER", "PROCESSING_UNIT", "DELIVERY_AGENT")) {
            return false
        }
        val type = getByType(mode)
        if (!type.showHerdBreeding) return false
        return business.sourceOwnCattle || type.showHerdBreeding
    }

    fun showHerdBreeding(mode: String?): Boolean {
        if (mode == null) return false
        if (mode in listOf("RETAIL_PARLOUR", "TRADER", "COLLECTION_CENTER", "PROCESSING_UNIT", "DELIVERY_AGENT")) {
            return false
        }
        return getByType(mode).showHerdBreeding
    }

    fun showMilkingProduction(business: BusinessEntity?): Boolean {
        if (business == null) return false
        val mode = business.businessMode
        if (mode in listOf("RETAIL_PARLOUR", "TRADER", "COLLECTION_CENTER", "PROCESSING_UNIT", "DELIVERY_AGENT")) {
            return false
        }
        val type = getByType(mode)
        if (!type.showMilkingProduction) return false
        return business.sourceOwnCattle || type.showMilkingProduction
    }

    fun showMilkingProduction(mode: String?): Boolean {
        if (mode == null) return false
        if (mode in listOf("RETAIL_PARLOUR", "TRADER", "COLLECTION_CENTER", "PROCESSING_UNIT", "DELIVERY_AGENT")) {
            return false
        }
        return getByType(mode).showMilkingProduction
    }

    fun showCattleFeed(business: BusinessEntity?): Boolean {
        if (business == null) return false
        val mode = business.businessMode
        if (mode in listOf("RETAIL_PARLOUR", "TRADER", "COLLECTION_CENTER", "PROCESSING_UNIT", "DELIVERY_AGENT")) {
            return false
        }
        val type = getByType(mode)
        if (!type.showCattleFeed) return false
        return business.sourceOwnCattle || type.showCattleFeed
    }

    fun showCattleFeed(mode: String?): Boolean {
        if (mode == null) return false
        if (mode in listOf("RETAIL_PARLOUR", "TRADER", "COLLECTION_CENTER", "PROCESSING_UNIT", "DELIVERY_AGENT")) {
            return false
        }
        return getByType(mode).showCattleFeed
    }

    fun showProductInventory(business: BusinessEntity?): Boolean {
        if (business == null) return false
        return getByType(business.businessMode).showProductInventory
    }

    fun showProductInventory(mode: String?): Boolean = getByType(mode).showProductInventory

    fun showInventoryManagement(business: BusinessEntity?): Boolean {
        if (business == null) return false
        return showCattleFeed(business) || showProductInventory(business)
    }

    fun showInventoryManagement(mode: String?): Boolean = getByType(mode).showInventoryManagement

    fun showCollectionDesk(business: BusinessEntity?): Boolean {
        if (business == null) return false
        val type = getByType(business.businessMode)
        if (!type.showFarmerProcurement) return false
        return business.sourceVillageFarmers || type.showFarmerProcurement
    }

    fun showCollectionDesk(mode: String?): Boolean = getByType(mode).showFarmerProcurement

    fun showDeliveryRoutes(business: BusinessEntity?): Boolean {
        if (business == null) return true
        val type = getByType(business.businessMode)
        if (!type.showRetailRoutes) return false
        return business.destHouseholds || type.showRetailRoutes
    }

    fun showDeliveryRoutes(mode: String?): Boolean = getByType(mode).showRetailRoutes

    fun showBilling(business: BusinessEntity?): Boolean {
        if (business == null) return true
        val type = getByType(business.businessMode)
        return type.showBilling
    }

    fun showBilling(mode: String?): Boolean = getByType(mode).showBilling

    fun showBulkDispatches(business: BusinessEntity?): Boolean {
        if (business == null) return false
        val type = getByType(business.businessMode)
        if (!type.showBulkDispatches) return false
        return business.destCollectionCenter || business.destBulkCommercial || business.destFactoryTankers || type.showBulkDispatches
    }

    fun showBulkDispatches(mode: String?): Boolean = getByType(mode).showBulkDispatches

    fun showRetailPos(business: BusinessEntity?): Boolean {
        if (business == null) return false
        return getByType(business.businessMode).showRetailPos
    }

    fun showRetailPos(mode: String?): Boolean = getByType(mode).showRetailPos

    // Feed vs Dairy Product Item Classification
    fun isFeedItem(itemName: String): Boolean {
        val lower = itemName.lowercase()
        return lower.contains("chokar") || lower.contains("chokkar") || lower.contains("feed") ||
               lower.contains("makka") || lower.contains("mustard") || lower.contains("cake") ||
               lower.contains("doc") || lower.contains("soya") || lower.contains("cotton") ||
               lower.contains("binola") || lower.contains("silage") || lower.contains("fodder") ||
               lower.contains("khal") || lower.contains("khali") || lower.contains("bran") ||
               lower.contains("straw") || lower.contains("bhusa") || lower.contains("bhoosa") ||
               lower.contains("mineral") || lower.contains("calcium") || lower.contains("supplement") ||
               lower.contains("grass") || lower.contains("grain") || lower.contains("pellet") ||
               lower.contains("kutty") || lower.contains("kutti") || lower.contains("churi") ||
               lower.contains("chuni") || lower.contains("pashu") || lower.contains("ahar") ||
               lower.contains("khurak") || lower.contains("dana") || lower.contains("sarson") ||
               itemName.contains("चोकर") || itemName.contains("चोक्खर") || itemName.contains("खली") ||
               itemName.contains("पशु आहार") || itemName.contains("दाना") || itemName.contains("चारा") ||
               itemName.contains("भूसा") || itemName.contains("साइलेज") || itemName.contains("घास") ||
               itemName.contains("ଦାନା") || itemName.contains("ଗୋ-ଖାଦ୍ୟ") || itemName.contains("କୁଣ୍ଡା") ||
               itemName.contains("ଖାଦ୍ୟ")
    }

    fun isDairyProductItem(itemName: String): Boolean = !isFeedItem(itemName)

    fun filterInventoryForMode(items: List<com.example.data.local.entity.InventoryItemEntity>, mode: String?): List<com.example.data.local.entity.InventoryItemEntity> {
        val showFeed = showCattleFeed(mode)
        val showProduct = showProductInventory(mode)
        return when {
            showFeed && showProduct -> items
            showFeed && !showProduct -> items.filter { isFeedItem(it.itemName) || it.itemName.contains("Raw", ignoreCase = true) }
            !showFeed && showProduct -> items.filter { !isFeedItem(it.itemName) }
            !showFeed -> items.filter { !isFeedItem(it.itemName) }
            else -> items.filter { !isFeedItem(it.itemName) }
        }
    }

    fun getDefaultInventoryItemsForMode(mode: String?): List<Triple<String, String, Double>> = when (mode) {
        "RETAIL_PARLOUR" -> listOf(
            Triple("Fresh Paneer", "kg", 360.0),
            Triple("Desi Cow Ghee", "kg", 650.0),
            Triple("Fresh Curd / Dahi", "kg", 70.0),
            Triple("Table Butter / Makhan", "kg", 450.0),
            Triple("Toned Milk Pouch (500ml)", "piece", 27.0),
            Triple("Standardized Milk Pouch (500ml)", "piece", 32.0),
            Triple("Full Cream Milk Pouch (500ml)", "piece", 34.0),
            Triple("Chaas / Buttermilk (200ml)", "piece", 15.0),
            Triple("Khoya / Mawa", "kg", 320.0),
            Triple("Cow Milk (Loose / Dispenser)", "litre", 60.0),
            Triple("Buffalo Milk (Loose / Dispenser)", "litre", 75.0)
        )
        "PROCESSING_UNIT" -> listOf(
            Triple("Raw Milk (Bulk Intake)", "litre", 42.0),
            Triple("Fresh Paneer", "kg", 340.0),
            Triple("Desi Ghee", "kg", 620.0),
            Triple("Curd / Dahi Bulk", "kg", 60.0),
            Triple("White Butter (Bulk)", "kg", 420.0),
            Triple("Khoya / Mawa (Bulk)", "kg", 300.0),
            Triple("Chaas / Buttermilk", "litre", 25.0)
        )
        "TRADER", "COLLECTION_CENTER" -> listOf(
            Triple("Raw Cow Milk (Bulk)", "litre", 40.0),
            Triple("Raw Buffalo Milk (Bulk)", "litre", 60.0)
        )
        "DELIVERY_AGENT" -> listOf(
            Triple("Toned Milk Pouch (500ml)", "piece", 27.0),
            Triple("Full Cream Milk Pouch (500ml)", "piece", 34.0),
            Triple("Cow Milk Bottles (1L)", "piece", 60.0),
            Triple("Curd / Dahi (400g)", "piece", 40.0)
        )
        "GAUSHALA" -> listOf(
            Triple("A2 Desi Cow Ghee (Bilona)", "kg", 1500.0),
            Triple("Desi Cow A2 Milk", "litre", 75.0),
            Triple("Panchagavya Fertilizer", "kg", 40.0),
            Triple("Cow Dung Dhoop Sticks", "box", 80.0),
            Triple("Chokar (Wheat Bran)", "kg", 22.0),
            Triple("Green Fodder & Silage", "kg", 8.0)
        )
        "FARMER", "FARMER_WHOLESALE", "INTEGRATED" -> listOf(
            Triple("Makka", "kg", 24.0),
            Triple("Mustard Cake (Sarson Khal)", "kg", 32.0),
            Triple("Mustard DOC", "kg", 28.0),
            Triple("Soya Meal", "kg", 45.0),
            Triple("Cotton Cake / Binola Khal", "kg", 38.0),
            Triple("Chokar (Wheat Bran)", "kg", 22.0),
            Triple("Mineral Mixture", "kg", 120.0),
            Triple("Silage / Green Fodder", "kg", 8.0)
        )
        else -> listOf(
            Triple("Fresh Paneer", "kg", 360.0),
            Triple("Desi Cow Ghee", "kg", 650.0),
            Triple("Fresh Curd / Dahi", "kg", 70.0),
            Triple("Toned Milk Pouch (500ml)", "piece", 27.0)
        )
    }

    fun getExpenseCategoriesForMode(mode: String?): List<String> = when (mode) {
        "RETAIL_PARLOUR" -> listOf(
            "Dairy Stock Purchase (Paneer/Curd/Ghee)",
            "Pouch Milk Purchase",
            "Shop Rent",
            "Electricity & Deep Fridge Power",
            "Counter Staff & Cashier Wages",
            "Packaging Pouches & Carry Bags",
            "Store Maintenance & Cleaning",
            "Local Cartage & Delivery",
            "Other Retail Expense"
        )
        "TRADER" -> listOf(
            "Farmer Milk Procurement Outlay",
            "Route Vehicle Diesel & Fuel",
            "Vehicle Maintenance & Tyres",
            "Driver & Delivery Helper Wages",
            "Can Washing, Ice & Sanitation",
            "Chilling Center Fees",
            "Route Tolls & Taxes",
            "Other Trading Expense"
        )
        "COLLECTION_CENTER" -> listOf(
            "Farmer Milk Intake Outlay",
            "BMC Chilling Vat Electricity",
            "Testing Chemicals (Acid/Alcohol/Fat)",
            "Center Operator & Helper Salary",
            "Tanker Dispatch Loading / Freight",
            "BMC Servicing & Generator Diesel",
            "Other BMC Expense"
        )
        "PROCESSING_UNIT" -> listOf(
            "Raw Milk Bulk Purchase",
            "Processing Ingredients & Cultures",
            "Cold Storage & Plant Power",
            "Boiler Fuel, LPG & Steam",
            "Packaging Pouches, Jars & Tins",
            "Plant Technicians & Labor",
            "Logistics & Wholesale Dispatch",
            "Machine Maintenance",
            "Other Plant Expense"
        )
        else -> listOf(
            "Cattle Feed (Khali/Pellets)",
            "Supplements & Minerals",
            "Veterinary Doctor & Meds",
            "Milker & Labor Salary",
            "Diesel & Delivery Fuel",
            "Electricity & Power",
            "Shed & Equipment Repair",
            "Packaging & Milk Pouches",
            "Other Farm Expense"
        )
    }

    // Milk Type Helpers
    fun isCowEnabled(supportedMilkTypes: String?): Boolean {
        return supportedMilkTypes != "BUFFALO_ONLY"
    }

    fun isBuffaloEnabled(supportedMilkTypes: String?): Boolean {
        return supportedMilkTypes != "COW_ONLY"
    }

    fun isMixedOrBoth(supportedMilkTypes: String?): Boolean {
        return supportedMilkTypes == "BOTH" || supportedMilkTypes == "MIXED"
    }
}
