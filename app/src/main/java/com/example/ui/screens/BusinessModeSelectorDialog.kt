package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.util.BusinessModeFeatures

data class BusinessModeOption(
    val code: String,
    val title: String,
    val badge: String,
    val subtitle: String,
    val description: String,
    val primaryFeatures: List<String>,
    val icon: ImageVector,
    val themeColor: Color
)

val BUSINESS_MODES = listOf(
    BusinessModeOption(
        code = "FARMER_WHOLESALE",
        title = "Dairy Farmer (Wholesale / Center)",
        badge = "🚜 Farm to Center",
        subtitle = "Produce milk & supply to Collection Center or Trader",
        description = "Tailored for dairy farmers who own cattle and supply their daily milk output directly to a village collection center, BMC chilling point, or milk trader.",
        primaryFeatures = listOf(
            "Cattle Herd Milking Yields & Lactation Tracking",
            "Milk Supply Slips to Collection Center / Trader",
            "FAT, SNF & Rate Received Passbook",
            "Cattle Feed, Fodder & Veterinary Expenses"
        ),
        icon = Icons.Default.Agriculture,
        themeColor = GrassGreen
    ),
    BusinessModeOption(
        code = "FARMER",
        title = "Dairy Farmer (Direct Retail + Center)",
        badge = "🚜 Producer & Seller",
        subtitle = "Own herd + deliver to households & sell surplus to Center",
        description = "Tailored for dairy farmers who own cattle, deliver fresh milk directly to household customer subscription routes, and supply surplus milk to a collection center or trader.",
        primaryFeatures = listOf(
            "Dairy Herd Milking Yield & Lactation Stages",
            "Doorstep Household Delivery Routes & Run-Sheets",
            "Monthly Customer Billing & WhatsApp Invoices",
            "Surplus Milk Dispatch to Collection Center"
        ),
        icon = Icons.Default.Agriculture,
        themeColor = GrassGreen
    ),
    BusinessModeOption(
        code = "INTEGRATED",
        title = "Integrated Producer & Collector",
        badge = "🌐 Farm & Aggregator",
        subtitle = "Own Farm + Farmer Collection + Retail Routes + Bulk Supply",
        description = "For dairy farmers who have their own herd, also collect milk from nearby farmers/suppliers, and sell to both household customers and bulk traders.",
        primaryFeatures = listOf(
            "Own Cattle Milking Yields & Herd Breeding",
            "Village Farmer Milk Collection Desk (FAT/SNF)",
            "Doorstep Customer Delivery Routes & Monthly Invoices",
            "Bulk Dispatches & Value-Added Products (Paneer, Ghee)"
        ),
        icon = Icons.Default.Hub,
        themeColor = DeepOceanNavy
    ),
    BusinessModeOption(
        code = "TRADER",
        title = "Milk Trader / Vendor / Wholesaler",
        badge = "🚚 Trader & Vendor",
        subtitle = "Collect from farmers & supply bulk buyers, hotels & individuals",
        description = "Tailored for milk distributors, vendors, and wholesalers who do not keep cattle, but purchase milk from local farmers and distribute to bulk buyers, tea stalls, restaurants, and retail customers.",
        primaryFeatures = listOf(
            "Farmer Milk Collection Desk (FAT, SNF, Rate Chart)",
            "Farmer Passbooks & 1-Tap Cash/Bank Payouts",
            "Commercial B2B Accounts (Hotels, Cafes, Bulk Buyers)",
            "Household Delivery Routes & Trading Margins"
        ),
        icon = Icons.Default.LocalShipping,
        themeColor = GoldenOrange
    ),
    BusinessModeOption(
        code = "COLLECTION_CENTER",
        title = "Milk Collection Centre (BMC Desk / Kendra)",
        badge = "🏢 BMC Center",
        subtitle = "Procure milk from village farmers & dispatch bulk factory tankers",
        description = "Tailored for village milk collection centers & BMC chilling points that purchase milk from local farmers with FAT/SNF testing, maintain farmer passbooks, and dispatch bulk tankers.",
        primaryFeatures = listOf(
            "Fast Farmer Milk Collection Desk with FAT & SNF Calculator",
            "Farmer Directory, Passbooks & 1-Tap Payment Slips",
            "Bulk Dispatches & Chilling Plant Tanker Outward Challans",
            "Center Spread Profit: (Bulk Factory Sales − Farmer Payouts)"
        ),
        icon = Icons.Default.LocalDrink,
        themeColor = RoyalBluePrimary
    ),
    BusinessModeOption(
        code = "PROCESSING_UNIT",
        title = "Dairy Processing & Byproducts Factory",
        badge = "🧀 Processing Unit",
        subtitle = "Convert raw milk into Paneer, Ghee, Butter, Curd & Khoya",
        description = "Tailored for dairy manufacturing units and micro-dairies that intake raw milk batches and process them into high-margin products with conversion yield logs and wholesale distribution.",
        primaryFeatures = listOf(
            "Milk Intake & Product Conversion Yield Calculations",
            "Batch Tracking for Paneer, Ghee, Butter, Dahi & Khoya",
            "Commercial Packaging, Cold Storage & Distributor Ledger",
            "Processing Margin & Fat Recovery Analysis"
        ),
        icon = Icons.Default.Kitchen,
        themeColor = WarmHoney
    ),
    BusinessModeOption(
        code = "RETAIL_PARLOUR",
        title = "Dairy Parlour & Milk Booth (Retail Counter)",
        badge = "🛒 Dairy Parlour",
        subtitle = "Walk-in shop counter sales of milk, curd, paneer & sweets",
        description = "Tailored for retail dairy shops, milk booths, and parlours selling packaged pouches, loose dispenser milk, paneer, dahi, ghee, and sweets with fast counter billing and instant UPI POS.",
        primaryFeatures = listOf(
            "Instant Walk-in Cash & UPI Counter POS Billing",
            "Pouch & Loose Milk Bottle Stock Tracking",
            "Value-Added Dairy Display (Paneer, Curd, Ghee, Sweets)",
            "Daily Counter Cash Drawer & Evening Drawer Closing"
        ),
        icon = Icons.Default.Storefront,
        themeColor = Color(0xFF8E24AA)
    ),
    BusinessModeOption(
        code = "DELIVERY_AGENT",
        title = "Milk Delivery Agent & Route Operator (Doodhwala)",
        badge = "🛵 Delivery Route",
        subtitle = "Door-to-door morning/evening household subscription routes",
        description = "Tailored for route operators and delivery vendors who manage morning & evening delivery boy runs, household monthly milk cards, bottles/pouches delivered, and pause calendars.",
        primaryFeatures = listOf(
            "Morning & Evening Delivery Boy Run-Sheets",
            "Customer Pause / Vacation Calendars & Extra Milk Requests",
            "Bottle Crate & Pouch Count Reconciliation",
            "Monthly Customer Passbooks & WhatsApp Invoices"
        ),
        icon = Icons.Default.TwoWheeler,
        themeColor = Color(0xFF0288D1)
    ),
    BusinessModeOption(
        code = "GAUSHALA",
        title = "Gaushala & Desi A2 Organic Farm",
        badge = "🐮 Gaushala / A2 Farm",
        subtitle = "Pure indigenous Desi cows (Gir/Sahiwal), A2 milk & seva",
        description = "Tailored for Gaushalas, Trusts, and ethical A2 organic farms managing indigenous cattle breeds, A2 organic milk distribution, Panchagavya products, and patron donations.",
        primaryFeatures = listOf(
            "Desi Cow Heritage Pedigree, Tagging & Medical Care",
            "Pure A2 Milk Subscription Batch Allotments",
            "Panchagavya, Organic Ghee & Dung/Urine Bio-Product Sales",
            "Donor / Patron Khata & Seva Contribution Tracking"
        ),
        icon = Icons.Default.Pets,
        themeColor = Color(0xFF2E7D32)
    ),
    BusinessModeOption(
        code = "CUSTOM",
        title = "Custom Dairy Setup (Build Your Own)",
        badge = "🛠️ Custom Setup",
        subtitle = "Fully modular setup tailored to your exact business requirements",
        description = "For dairy businesses with unique workflows. Pick and choose exactly which sourcing, distribution, byproducts, and billing features you need.",
        primaryFeatures = listOf(
            "Custom Sourcing (Own Cattle + Village Farmers)",
            "Custom Sales Channels (Doorstep, Wholesale, Tankers)",
            "Byproduct Inventory & Yield Conversions",
            "Full Workspace Tailoring"
        ),
        icon = Icons.Default.Tune,
        themeColor = Color(0xFF00897B)
    )
)

@Composable
fun BusinessModeSelectorDialog(
    currentMode: String,
    onDismiss: () -> Unit,
    onSelectMode: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "⚙️ Dairy Operating Role & Profile",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary
                )
                Text(
                    text = "Choose your operating model to dynamically activate needed features and hide unneeded options.",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(BUSINESS_MODES) { option ->
                    val isSelected = (currentMode == option.code) || (currentMode.isBlank() && option.code == "FARMER")

                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) option.themeColor.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
                        ),
                        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, option.themeColor) else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelectMode(option.code)
                            }
                            .testTag("mode_option_${option.code.lowercase()}")
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = option.themeColor.copy(alpha = 0.15f),
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = option.icon,
                                                contentDescription = null,
                                                tint = option.themeColor,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = option.title,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = option.subtitle,
                                            fontSize = 10.sp,
                                            color = TextSecondary
                                        )
                                    }
                                }

                                if (isSelected) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = option.themeColor
                                    ) {
                                        Text(
                                            text = "ACTIVE",
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = option.description,
                                fontSize = 10.sp,
                                color = TextPrimary.copy(alpha = 0.8f),
                                lineHeight = 14.sp
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            // Features Pills
                            val featConfig = BusinessModeFeatures.getByType(option.code)
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (featConfig.showHerdBreeding) GrassGreen.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ) {
                                    Text(
                                        text = "🐄 Herd: ${if (featConfig.showHerdBreeding) "Active" else "Hidden"}",
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (featConfig.showHerdBreeding) GrassGreen else TextMuted,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (featConfig.showCollectionDesk) RoyalBlueLight else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ) {
                                    Text(
                                        text = "🥛 Procurement: ${if (featConfig.showCollectionDesk) "Active" else "Hidden"}",
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (featConfig.showCollectionDesk) RoyalBluePrimary else TextMuted,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (featConfig.showDeliveryRoutes) GoldenOrange.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ) {
                                    Text(
                                        text = "🚚 Routes: ${if (featConfig.showDeliveryRoutes) "Active" else "Hidden"}",
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (featConfig.showDeliveryRoutes) GoldenOrange else TextMuted,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (featConfig.showInventoryManagement) DairyGreenLight else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ) {
                                    Text(
                                        text = if (featConfig.showCattleFeed) "🌾 Feed: Active" else if (featConfig.showProductInventory) "📦 SKUs: Active" else "🌾 Feed: Hidden",
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (featConfig.showInventoryManagement) DairyGreen else TextMuted,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                option.primaryFeatures.take(3).forEach { feat ->
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = option.themeColor,
                                            modifier = Modifier.size(11.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = feat,
                                            fontSize = 10.sp,
                                            color = TextSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done", fontWeight = FontWeight.Bold)
            }
        }
    )
}
