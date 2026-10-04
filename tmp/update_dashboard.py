# Script to update DashboardScreen.kt with slide-by-slide HorizontalPager and contextual business cards
import re

with open('app/src/main/java/com/example/ui/screens/DashboardScreen.kt', 'r') as f:
    content = f.read()

# 1. Add DashboardSlideItem data class right before DailyProductionPoint
slide_item_def = """data class DashboardSlideItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val themeColor: Color
)

"""

if 'data class DashboardSlideItem' not in content:
    content = content.replace("data class DailyProductionPoint(", slide_item_def + "data class DailyProductionPoint(")

# 2. Find the start of LazyColumn inside DashboardScreen
# It starts around: val todayFormatted = remember { ... } \n\n LazyColumn(
# And ends at: if (showCustomFormulaStudio) {

pattern = re.compile(r'(\s+val todayFormatted = remember \{[\s\S]*?\n\s+\}\n\n)\s+LazyColumn\([\s\S]*?\n\s+\}\n\n(\s+if \(showCustomFormulaStudio\))', re.MULTILINE)

m = pattern.search(content)
if not m:
    print("Pattern match failed!")
    exit(1)

today_formatted_part = m.group(1)
custom_formula_part = m.group(2)

new_body = """    val currentMode = business?.businessMode ?: "FARMER"
    val supportedTypes = business?.supportedMilkTypes ?: "BOTH"

    // Contextual slide-by-slide sections dynamically tailored to the active business profile
    val dynamicTabs = remember(currentMode, supportedTypes) {
        when (currentMode) {
            "TRADER" -> listOf(
                DashboardSlideItem(
                    id = "sourcing_sales",
                    title = "🥛 Sourcing & Sales",
                    subtitle = "Procurement & Trading Margin",
                    icon = Icons.Default.LocalShipping,
                    themeColor = GoldenOrange
                ),
                DashboardSlideItem(
                    id = "operations",
                    title = "⚡ Operations",
                    subtitle = "Routes, Dispatches & Logistics",
                    icon = Icons.Default.Tune,
                    themeColor = RoyalBluePrimary
                ),
                DashboardSlideItem(
                    id = "financials",
                    title = "📊 Financials",
                    subtitle = "Trading Margins & Khata",
                    icon = Icons.Default.AccountBalanceWallet,
                    themeColor = GrassGreen
                )
            )

            "COLLECTION_CENTER" -> listOf(
                DashboardSlideItem(
                    id = "sourcing_sales",
                    title = "🥛 Sourcing & Intake",
                    subtitle = "Village Procurement & Tankers",
                    icon = Icons.Default.LocalDrink,
                    themeColor = RoyalBluePrimary
                ),
                DashboardSlideItem(
                    id = "operations",
                    title = "⚡ Operations",
                    subtitle = "Chilling BMC & Center Logistics",
                    icon = Icons.Default.Tune,
                    themeColor = WarmHoney
                ),
                DashboardSlideItem(
                    id = "financials",
                    title = "📊 Financials",
                    subtitle = "Center Spread & Farmer Payouts",
                    icon = Icons.Default.AccountBalanceWallet,
                    themeColor = GrassGreen
                )
            )

            "RETAIL_PARLOUR" -> listOf(
                DashboardSlideItem(
                    id = "sourcing_sales",
                    title = "🛒 Counter Sales",
                    subtitle = "POS, Pouches & Milk Booth",
                    icon = Icons.Default.Storefront,
                    themeColor = Color(0xFF8E24AA)
                ),
                DashboardSlideItem(
                    id = "operations",
                    title = "⚡ Operations",
                    subtitle = "Cash Drawer, Stock & Shelf",
                    icon = Icons.Default.PointOfSale,
                    themeColor = RoyalBluePrimary
                ),
                DashboardSlideItem(
                    id = "financials",
                    title = "📊 Financials",
                    subtitle = "Daily Cash, UPI & Margins",
                    icon = Icons.Default.AccountBalanceWallet,
                    themeColor = GrassGreen
                )
            )

            "PROCESSING_UNIT" -> listOf(
                DashboardSlideItem(
                    id = "sourcing_sales",
                    title = "🧀 Sourcing & Processing",
                    subtitle = "Raw Intake & Conversion Yields",
                    icon = Icons.Default.Kitchen,
                    themeColor = WarmHoney
                ),
                DashboardSlideItem(
                    id = "operations",
                    title = "⚡ Operations",
                    subtitle = "Cold Storage, Batches & Inventory",
                    icon = Icons.Default.Inventory2,
                    themeColor = RoyalBluePrimary
                ),
                DashboardSlideItem(
                    id = "financials",
                    title = "📊 Financials",
                    subtitle = "Processing Margins & Invoicing",
                    icon = Icons.Default.AccountBalanceWallet,
                    themeColor = GrassGreen
                )
            )

            "FARMER_WHOLESALE" -> listOf(
                DashboardSlideItem(
                    id = "sourcing_sales",
                    title = "🥛 Production & Supply",
                    subtitle = "Milking Output & Center Slips",
                    icon = Icons.Default.LocalShipping,
                    themeColor = DairyGreen
                ),
                DashboardSlideItem(
                    id = "herd_breeding",
                    title = when (supportedTypes) {
                        "COW_ONLY" -> "🐄 Cow Herd"
                        "BUFFALO_ONLY" -> "🐃 Buffalo Herd"
                        else -> "🐄 Herd & Breeding"
                    },
                    subtitle = "Animal Health & Yield",
                    icon = Icons.Default.Pets,
                    themeColor = Color(0xFF673AB7)
                ),
                DashboardSlideItem(
                    id = "operations",
                    title = "⚡ Operations",
                    subtitle = "Feed, Fodder & Farm Upkeep",
                    icon = Icons.Default.Tune,
                    themeColor = WarmHoney
                ),
                DashboardSlideItem(
                    id = "financials",
                    title = "📊 Financials",
                    subtitle = "Center Dues & Net Profit",
                    icon = Icons.Default.AccountBalanceWallet,
                    themeColor = RoyalBluePrimary
                )
            )

            "INTEGRATED" -> listOf(
                DashboardSlideItem(
                    id = "sourcing_sales",
                    title = "🥛 Sourcing & Sales",
                    subtitle = "Own Milk + Village Procurement",
                    icon = Icons.Default.Hub,
                    themeColor = DeepOceanNavy
                ),
                DashboardSlideItem(
                    id = "herd_breeding",
                    title = "🐄 Herd & Breeding",
                    subtitle = "Cattle Health & Milking Yield",
                    icon = Icons.Default.Pets,
                    themeColor = Color(0xFF673AB7)
                ),
                DashboardSlideItem(
                    id = "operations",
                    title = "⚡ Operations",
                    subtitle = "Routes, BMC & Inventory",
                    icon = Icons.Default.Tune,
                    themeColor = WarmHoney
                ),
                DashboardSlideItem(
                    id = "financials",
                    title = "📊 Financials",
                    subtitle = "P&L, Receivables & Payables",
                    icon = Icons.Default.AccountBalanceWallet,
                    themeColor = GrassGreen
                )
            )

            else -> listOf( // "FARMER", "GAUSHALA"
                DashboardSlideItem(
                    id = "sourcing_sales",
                    title = "🥛 Sourcing & Sales",
                    subtitle = "Milking Yield & Route Sales",
                    icon = Icons.Default.LocalDrink,
                    themeColor = DairyGreen
                ),
                DashboardSlideItem(
                    id = "herd_breeding",
                    title = when (supportedTypes) {
                        "COW_ONLY" -> "🐄 Cow Herd"
                        "BUFFALO_ONLY" -> "🐃 Buffalo Herd"
                        else -> "🐄 Herd & Breeding"
                    },
                    subtitle = "Herd Lifecycle & Genetics",
                    icon = Icons.Default.Pets,
                    themeColor = Color(0xFF673AB7)
                ),
                DashboardSlideItem(
                    id = "operations",
                    title = "⚡ Operations",
                    subtitle = "Routes, Milking Run & Feed",
                    icon = Icons.Default.Tune,
                    themeColor = WarmHoney
                ),
                DashboardSlideItem(
                    id = "financials",
                    title = "📊 Financials",
                    subtitle = "Customer Khata & Net Margin",
                    icon = Icons.Default.AccountBalanceWallet,
                    themeColor = RoyalBluePrimary
                )
            )
        }
    }

    val pagerState = rememberPagerState(pageCount = { dynamicTabs.size })
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceBg)
    ) {
        // ----------------- PINNED TOP DASHBOARD CONTROL CENTER -----------------
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Business Header & Live Shift Tag
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = (business?.businessName ?: "MilkMate Dairy").uppercase(),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = RoyalBluePrimary,
                            letterSpacing = 1.sp
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (selectedShift == "MORNING") FreshGoldLight else RoyalBlueLight,
                            modifier = Modifier.clickable {
                                val next = if (selectedShift == "MORNING") "EVENING" else "MORNING"
                                viewModel.setShift(next)
                            }
                        ) {
                            Text(
                                text = if (selectedShift == "MORNING") "🌅 MORNING ⇄" else "🌆 EVENING ⇄",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedShift == "MORNING") WarmHoney else RoyalBluePrimary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = todayFormatted,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                if (customers.isEmpty() && farmers.isEmpty()) {
                    FilledTonalButton(
                        onClick = { viewModel.populateDemoData() },
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = DairyGreenLight),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(Icons.Default.AutoFixHigh, contentDescription = null, tint = DairyGreen, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Load Sample", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DairyGreen)
                    }
                } else {
                    IconButton(
                        onClick = { viewModel.populateDemoData() },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reload Demo Data", tint = RoyalBluePrimary, modifier = Modifier.size(18.dp))
                    }
                }
            }

            // Active Milk Rate & Formula Strip
            val biz = business
            val formulaLabel = when (biz?.pricingMode?.uppercase()) {
                "PANEER_YIELD" -> "🧀 Paneer Yield Pricing (Grams/L → Rate)"
                "KHOA_YIELD", "KHOYA_YIELD" -> "🍬 Khoya Yield Pricing (Grams/L → Rate)"
                "GHEE_YIELD" -> "🏺 Ghee Fat Recovery Pricing"
                "FAT_SNF" -> "🧪 Cooperative FAT + SNF Matrix"
                "FAT_ONLY" -> "🧈 Pure Fat Multiplier"
                "CUSTOM" -> "⚙️ Custom: ${biz.customFormulaName}"
                else -> "💵 Flat Base Rate (Cow ₹${biz?.cowMilkRate ?: 50.0} / Buffalo ₹${biz?.buffaloMilkRate ?: 65.0})"
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                shadowElevation = 0.5.dp,
                modifier = Modifier.fillMaxWidth().clickable { showCustomFormulaStudio = true }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Surface(
                            shape = CircleShape,
                            color = RoyalBlueLight,
                            modifier = Modifier.size(26.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Functions, contentDescription = null, tint = RoyalBluePrimary, modifier = Modifier.size(15.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("active_rate_engine".loc() + ":", fontSize = 9.sp, color = TextSecondary)
                            Text(formulaLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary, maxLines = 1)
                        }
                    }
                    Text(
                        text = "customize".loc() + " ⚙️",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = RoyalBluePrimary
                    )
                }
            }
        }

        // ----------------- GROWW-STYLE TOP HORIZONTAL SLIDE TAB BAR -----------------
        ScrollableTabRow(
            selectedTabIndex = pagerState.currentPage.coerceIn(0, dynamicTabs.size - 1),
            edgePadding = 12.dp,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = RoyalBluePrimary,
            indicator = { tabPositions ->
                val curr = pagerState.currentPage.coerceIn(0, dynamicTabs.size - 1)
                if (curr in tabPositions.indices) {
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[curr]),
                        height = 3.dp,
                        color = dynamicTabs[curr].themeColor
                    )
                }
            },
            divider = {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
            },
            modifier = Modifier.fillMaxWidth().testTag("groww_home_slide_tabs")
        ) {
            dynamicTabs.forEachIndexed { index, tabItem ->
                val isSelected = pagerState.currentPage == index
                Tab(
                    selected = isSelected,
                    onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    },
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (isSelected) tabItem.themeColor.copy(alpha = 0.15f) else Color.Transparent,
                                modifier = Modifier.size(22.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = tabItem.icon,
                                        contentDescription = null,
                                        tint = if (isSelected) tabItem.themeColor else TextSecondary,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }
                            Text(
                                text = tabItem.title,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                color = if (isSelected) tabItem.themeColor else TextSecondary
                            )
                        }
                    }
                )
            }
        }

        // ----------------- HORIZONTAL PAGER (SWIPEABLE SLIDE-BY-SLIDE VIEWS) -----------------
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { pageIndex ->
            val slide = dynamicTabs.getOrNull(pageIndex) ?: dynamicTabs.first()
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 10.dp, bottom = 48.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                when (slide.id) {
                    // ================= SLIDE: SOURCING & SALES / INTAKE =================
                    "sourcing_sales" -> {
                        // 1. Contextual Cockpit Header Card
                        item {
                            val shiftCollections = todayCollections.filter { it.shift == selectedShift }
                            val totalCollectedLiters = shiftCollections.sumOf { it.quantityLiters }
                            val avgProcureFat = if (shiftCollections.isNotEmpty()) shiftCollections.map { it.fat }.average() else 0.0
                            val totalDispatchedLiters = bulkDispatches.filter { it.dateEpochMidnight == todayMidnight }.sumOf { it.totalLiters }
                            val totalDispatchBilling = bulkDispatches.filter { it.dateEpochMidnight == todayMidnight }.sumOf { it.totalAmount }
                            val totalFarmerDues = farmers.sumOf { it.balancePayable }

                            when (currentMode) {
                                "RETAIL_PARLOUR" -> {
                                    Card(
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(containerColor = OceanMidnight),
                                        modifier = Modifier.fillMaxWidth().testTag("retail_counter_cockpit_card")
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Default.Storefront, contentDescription = null, tint = FreshGold, modifier = Modifier.size(18.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("🛒 Retail Counter POS & Milk Booth", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                }
                                                OutlinedButton(
                                                    onClick = { onNavigateToTab(1) },
                                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = FreshGold),
                                                    border = androidx.compose.foundation.BorderStroke(1.dp, FreshGold),
                                                    shape = RoundedCornerShape(8.dp),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                    modifier = Modifier.height(28.dp)
                                                ) {
                                                    Text("Open POS →", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }

                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Column {
                                                    Text("Today Counter Sales", color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp)
                                                    Text("₹${totalSales.toInt()}", color = FreshGold, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                                                    Text("${completedDeliveries} Counter Orders", color = Color.White.copy(alpha = 0.6f), fontSize = 9.sp)
                                                }
                                                Column {
                                                    Text("Milk Pouches & Loose", color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp)
                                                    Text("${String.format(Locale.US, "%.1f", milkSoldLiters)} L", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                                                    Text("Dispensed Today", color = GrassGreen, fontSize = 9.sp)
                                                }
                                                Column(horizontalAlignment = Alignment.End) {
                                                    Text("Cash & UPI Drawer", color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp)
                                                    Text("₹${todayCollectedPayments.toInt()}", color = DairyGreen, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                                                    Text("Register Balanced", color = Color.White.copy(alpha = 0.6f), fontSize = 9.sp)
                                                }
                                            }

                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Button(
                                                    onClick = { onNavigateToTab(1) },
                                                    shape = RoundedCornerShape(8.dp),
                                                    colors = ButtonDefaults.buttonColors(containerColor = RoyalBluePrimary),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                    modifier = Modifier.weight(1f).height(34.dp)
                                                ) {
                                                    Text("⚡ Quick POS", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                                Button(
                                                    onClick = onOpenPayments,
                                                    shape = RoundedCornerShape(8.dp),
                                                    colors = ButtonDefaults.buttonColors(containerColor = GrassGreen),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                    modifier = Modifier.weight(1f).height(34.dp)
                                                ) {
                                                    Text("🧾 Sales Khata", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                                Button(
                                                    onClick = onOpenInventory,
                                                    shape = RoundedCornerShape(8.dp),
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.15f)),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                    modifier = Modifier.weight(1f).height(34.dp)
                                                ) {
                                                    Text("📦 Inventory", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                                }
                                            }
                                        }
                                    }
                                }

                                "PROCESSING_UNIT" -> {
                                    Card(
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(containerColor = OceanMidnight),
                                        modifier = Modifier.fillMaxWidth().testTag("processor_intake_cockpit_card")
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Default.Kitchen, contentDescription = null, tint = FreshGold, modifier = Modifier.size(18.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("🧀 Raw Milk Intake & Batch Processing", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                }
                                                OutlinedButton(
                                                    onClick = onOpenInventory,
                                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = FreshGold),
                                                    border = androidx.compose.foundation.BorderStroke(1.dp, FreshGold),
                                                    shape = RoundedCornerShape(8.dp),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                    modifier = Modifier.height(28.dp)
                                                ) {
                                                    Text("Batches →", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }

                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Column {
                                                    Text("Raw Milk Intake", color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp)
                                                    Text("${String.format(Locale.US, "%.1f", totalCollectedLiters)} L", color = FreshGold, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                                                    Text("Avg FAT: ${String.format(Locale.US, "%.1f", avgProcureFat)}%", color = Color.White.copy(alpha = 0.6f), fontSize = 9.sp)
                                                }
                                                Column {
                                                    Text("In Processing Batches", color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp)
                                                    Text("${String.format(Locale.US, "%.1f", totalCollectedLiters * 0.85)} L", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                                                    Text("Paneer & Ghee Units", color = GrassGreen, fontSize = 9.sp)
                                                }
                                                Column(horizontalAlignment = Alignment.End) {
                                                    Text("Wholesale Deliveries", color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp)
                                                    Text("₹${totalSales.toInt()}", color = DairyGreen, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                                                    Text("${inventoryItems.size} Stock Items", color = Color.White.copy(alpha = 0.6f), fontSize = 9.sp)
                                                }
                                            }

                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Button(
                                                    onClick = onOpenInventory,
                                                    shape = RoundedCornerShape(8.dp),
                                                    colors = ButtonDefaults.buttonColors(containerColor = WarmHoney),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                    modifier = Modifier.weight(1f).height(34.dp)
                                                ) {
                                                    Text("🧀 Conversion Studio", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                                Button(
                                                    onClick = onOpenProfit,
                                                    shape = RoundedCornerShape(8.dp),
                                                    colors = ButtonDefaults.buttonColors(containerColor = RoyalBluePrimary),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                    modifier = Modifier.weight(1f).height(34.dp)
                                                ) {
                                                    Text("📊 Margins", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }

                                "FARMER_WHOLESALE" -> {
                                    Card(
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(containerColor = OceanMidnight),
                                        modifier = Modifier.fillMaxWidth().testTag("wholesale_cockpit_card")
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Default.LocalShipping, contentDescription = null, tint = FreshGold, modifier = Modifier.size(18.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("🚜 Farm Production & Center Supply", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                }
                                                OutlinedButton(
                                                    onClick = onOpenOrders,
                                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = FreshGold),
                                                    border = androidx.compose.foundation.BorderStroke(1.dp, FreshGold),
                                                    shape = RoundedCornerShape(8.dp),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                    modifier = Modifier.height(28.dp)
                                                ) {
                                                    Text("Slips →", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }

                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Column {
                                                    Text("Farm Milking Output", color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp)
                                                    Text("${String.format(Locale.US, "%.1f", herdDailyCap)} L", color = FreshGold, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                                                    Text("Morning + Evening", color = Color.White.copy(alpha = 0.6f), fontSize = 9.sp)
                                                }
                                                Column {
                                                    Text("Dispatched to Center", color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp)
                                                    Text("${String.format(Locale.US, "%.1f", totalDispatchedLiters)} L", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                                                    Text("₹${totalDispatchBilling.toInt()} Billed", color = GrassGreen, fontSize = 9.sp)
                                                }
                                                Column(horizontalAlignment = Alignment.End) {
                                                    Text("Center Dues Receivable", color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp)
                                                    Text("₹${totalReceivable.toInt()}", color = DairyGreen, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                                                    Text("Fortnightly Settlement", color = Color.White.copy(alpha = 0.6f), fontSize = 9.sp)
                                                }
                                            }
                                        }
                                    }
                                }

                                else -> {
                                    // TRADER, COLLECTION_CENTER, FARMER, INTEGRATED
                                    val cockpitTitle = when (currentMode) {
                                        "TRADER" -> "🚚 Farmer Procurement & Sourcing Desk"
                                        "COLLECTION_CENTER" -> "🏢 Village Farmer Collection Desk"
                                        else -> "🌐 Sourcing & Milk Intake Desk"
                                    }

                                    Card(
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(containerColor = OceanMidnight),
                                        modifier = Modifier.fillMaxWidth().testTag("sourcing_intake_cockpit_card")
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Default.LocalDrink, contentDescription = null, tint = FreshGold, modifier = Modifier.size(18.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(text = cockpitTitle, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                }
                                                OutlinedButton(
                                                    onClick = onOpenCollectionDesk,
                                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = FreshGold),
                                                    border = androidx.compose.foundation.BorderStroke(1.dp, FreshGold),
                                                    shape = RoundedCornerShape(8.dp),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                    modifier = Modifier.height(28.dp)
                                                ) {
                                                    Text("Open Desk →", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }

                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Column {
                                                    Text("Farmer Procured Intake", color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp)
                                                    Text(
                                                        text = "${String.format(Locale.US, "%.1f", totalCollectedLiters)} L",
                                                        color = FreshGold,
                                                        fontSize = 16.sp,
                                                        fontWeight = FontWeight.ExtraBold
                                                    )
                                                    Text("Avg FAT: ${String.format(Locale.US, "%.1f", avgProcureFat)}%", color = Color.White.copy(alpha = 0.6f), fontSize = 9.sp)
                                                }

                                                Column {
                                                    val middleTitle = if (currentMode == "TRADER") "Delivered / Sold" else "Tanker Dispatched"
                                                    val middleValue = if (currentMode == "TRADER") "${String.format(Locale.US, "%.1f", milkSoldLiters)} L" else "${String.format(Locale.US, "%.1f", totalDispatchedLiters)} L"
                                                    val middleSub = if (currentMode == "TRADER") "₹${String.format(Locale.US, "%.0f", totalSales)} sales" else "₹${String.format(Locale.US, "%.0f", totalDispatchBilling)} billed"
                                                    Text(middleTitle, color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp)
                                                    Text(text = middleValue, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                                                    Text(middleSub, color = GrassGreen, fontSize = 9.sp)
                                                }

                                                Column(horizontalAlignment = Alignment.End) {
                                                    Text("Farmer Payable Dues", color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp)
                                                    Text(
                                                        text = "₹${String.format(Locale.US, "%.0f", totalFarmerDues)}",
                                                        color = DangerRed,
                                                        fontSize = 16.sp,
                                                        fontWeight = FontWeight.ExtraBold
                                                    )
                                                    Text("${farmers.size} Farmers", color = Color.White.copy(alpha = 0.6f), fontSize = 9.sp)
                                                }
                                            }

                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Button(
                                                    onClick = onOpenCollectionDesk,
                                                    shape = RoundedCornerShape(8.dp),
                                                    colors = ButtonDefaults.buttonColors(containerColor = RoyalBluePrimary),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                    modifier = Modifier.weight(1f).height(34.dp)
                                                ) {
                                                    Text("🥛 Entry Desk", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                                Button(
                                                    onClick = onOpenCollectionDesk,
                                                    shape = RoundedCornerShape(8.dp),
                                                    colors = ButtonDefaults.buttonColors(containerColor = GrassGreen),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                    modifier = Modifier.weight(1f).height(34.dp)
                                                ) {
                                                    Text("👥 Farmers Khata", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                                Button(
                                                    onClick = onOpenProfit,
                                                    shape = RoundedCornerShape(8.dp),
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.15f)),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                    modifier = Modifier.weight(1f).height(34.dp)
                                                ) {
                                                    Text("📊 Margin Spread", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // 2. Mode-Specific Clearance or Stock Card
                        if (currentMode == "TRADER") {
                            item {
                                val procuredToday = todayCollections.sumOf { it.quantityLiters }
                                val deliveredToday = milkSoldLiters
                                val procurementCostToday = todayCollections.sumOf { it.totalAmount }
                                val deliveryRevenueToday = totalSales
                                val grossTradingMargin = deliveryRevenueToday - procurementCostToday

                                Card(
                                    modifier = Modifier.fillMaxWidth().testTag("trader_sourcing_reconciliation_card"),
                                    shape = RoundedCornerShape(18.dp),
                                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Text("🚚 Trading Procurement Clearance", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Sourced: ${String.format(Locale.US, "%.1fL", procuredToday)}", fontSize = 12.sp, color = RoyalBluePrimary, fontWeight = FontWeight.Bold)
                                            Text("Sold: ${String.format(Locale.US, "%.1fL", deliveredToday)}", fontSize = 12.sp, color = GrassGreen, fontWeight = FontWeight.Bold)
                                            Text("Margin: ₹${grossTradingMargin.toInt()}", fontSize = 12.sp, color = GoldenOrange, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }

                        // 3. Sourcing / Sales Analytics Chart
                        item {
                            val chartTitle = when (currentMode) {
                                "RETAIL_PARLOUR" -> "📊 Retail Counter Sales & Pouch Volume"
                                "PROCESSING_UNIT" -> "📊 Milk Intake & Processing Batch Volume"
                                "COLLECTION_CENTER" -> "📊 Village Procurement & Sourcing Volume"
                                "TRADER" -> "📊 Farmer Procurement vs Wholesale Sales"
                                else -> "📊 Daily Milk Production & Volume Analytics"
                            }
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text(chartTitle, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                                    InteractiveDualBarChart(
                                        dataPoints = productionDataPoints,
                                        selectedIndex = selectedDayIndex,
                                        onSelect = { selectedDayIndex = it },
                                        supportedMilkTypes = business?.supportedMilkTypes ?: "BOTH"
                                    )
                                }
                            }
                        }

                        // 4. Farmer Dues (Shown only for Procurement modes: TRADER, COLLECTION_CENTER, INTEGRATED)
                        if (currentMode == "TRADER" || currentMode == "COLLECTION_CENTER" || currentMode == "INTEGRATED") {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                                    elevation = CardDefaults.cardElevation(1.dp)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("👨‍🌾 Farmer Procurement Dues & Activity", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                                            TextButton(onClick = onOpenCollectionDesk) {
                                                Text("Farmers Hub →", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = RoyalBluePrimary)
                                            }
                                        }
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Surface(shape = RoundedCornerShape(10.dp), color = FreshGoldLight, modifier = Modifier.weight(1f)) {
                                                Column(modifier = Modifier.padding(10.dp)) {
                                                    Text("Total Farmer Dues", fontSize = 10.sp, color = TextSecondary)
                                                    Text("₹${farmers.sumOf { it.balancePayable }.toInt()}", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = WarmHoney)
                                                }
                                            }
                                            Surface(shape = RoundedCornerShape(10.dp), color = DairyGreenLight, modifier = Modifier.weight(1f)) {
                                                Column(modifier = Modifier.padding(10.dp)) {
                                                    Text("Registered Farmers", fontSize = 10.sp, color = TextSecondary)
                                                    Text("${farmers.size} Farmers", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = DairyGreen)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ================= SLIDE: HERD & BREEDING =================
                    "herd_breeding" -> {
                        item {
                            val scopedDashboardCattle = remember(cattleList, supportedTypes) {
                                when (supportedTypes) {
                                    "COW_ONLY" -> cattleList.filter { it.type.equals("COW", ignoreCase = true) }
                                    "BUFFALO_ONLY" -> cattleList.filter { it.type.equals("BUFFALO", ignoreCase = true) }
                                    else -> cattleList
                                }
                            }
                            val totalAnimals = scopedDashboardCattle.size
                            val lactating = scopedDashboardCattle.count { it.lactationStage == "LACTATING" }
                            val pregnant = scopedDashboardCattle.count { it.breedingStatus == "CONFIRMED_PREGNANT" }
                            val inseminated = scopedDashboardCattle.count { it.breedingStatus == "INSEMINATED" }
                            val herdMilkingCap = scopedDashboardCattle.filter { it.lactationStage == "LACTATING" }.sumOf { it.dailyYieldLiters }

                            val herdCardTitle = when (supportedTypes) {
                                "COW_ONLY" -> "🐄 Cow Herd & Breeding"
                                "BUFFALO_ONLY" -> "🐃 Buffalo Herd & Breeding"
                                else -> "🐄 Herd & Breeding Lifecycle"
                            }
                            val herdUnit = when (supportedTypes) {
                                "COW_ONLY" -> "Cows"
                                "BUFFALO_ONLY" -> "Buffaloes"
                                else -> "Cattle"
                            }

                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(2.dp),
                                modifier = Modifier.fillMaxWidth().testTag("dashboard_cattle_breeding_card")
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Surface(
                                                shape = CircleShape,
                                                color = GrassGreen.copy(alpha = 0.15f),
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(Icons.Default.Pets, contentDescription = null, tint = GrassGreen, modifier = Modifier.size(16.dp))
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(herdCardTitle, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                                                Text("Active Farm Producer Inputs", fontSize = 10.sp, color = TextSecondary)
                                            }
                                        }

                                        OutlinedButton(
                                            onClick = onOpenCattleBreeding,
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                            modifier = Modifier.height(28.dp)
                                        ) {
                                            Text("Breeding Desk →", fontSize = 11.sp, color = GrassGreen, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Column {
                                            Text("Herd Size", fontSize = 10.sp, color = TextSecondary)
                                            Text(text = "$totalAnimals $herdUnit", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                            Text("$lactating Milking", fontSize = 9.sp, color = FreshGold)
                                        }

                                        Column {
                                            Text("Est. Milking Yield", fontSize = 10.sp, color = TextSecondary)
                                            Text(
                                                text = "${String.format(Locale.US, "%.1f", herdMilkingCap)} L/d",
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = RoyalBluePrimary
                                            )
                                            Text("Vs ${String.format(Locale.US, "%.1f", milkSoldLiters)}L Sold", fontSize = 9.sp, color = TextSecondary)
                                        }

                                        Column(horizontalAlignment = Alignment.End) {
                                            Text("In-Calf / A.I.", fontSize = 10.sp, color = TextSecondary)
                                            Text(text = "$pregnant Pregnant", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = GrassGreen)
                                            Text("$inseminated Inseminated", fontSize = 9.sp, color = WarmHoney)
                                        }
                                    }

                                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))

                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = RoyalBlueLight,
                                            modifier = Modifier.weight(1f).clickable { onOpenCattleBreeding() }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
                                                horizontalArrangement = Arrangement.Center,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Default.Scale, contentDescription = null, tint = RoyalBluePrimary, modifier = Modifier.size(13.dp))
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Text("Weight/Girth", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = RoyalBluePrimary)
                                            }
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = DangerRed.copy(alpha = 0.1f),
                                            modifier = Modifier.weight(1f).clickable { onOpenCattleBreeding() }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
                                                horizontalArrangement = Arrangement.Center,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Default.Science, contentDescription = null, tint = DangerRed, modifier = Modifier.size(13.dp))
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Text("CMT Udder", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = DangerRed)
                                            }
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = WarmHoney.copy(alpha = 0.12f),
                                            modifier = Modifier.weight(1f).clickable { onOpenCattleBreeding() }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
                                                horizontalArrangement = Arrangement.Center,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Default.MonitorHeart, contentDescription = null, tint = WarmHoney, modifier = Modifier.size(13.dp))
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Text("BCS Score", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = WarmHoney)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Milking Production Analytics Chart for Herd
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text("🐄 Herd Daily Milking Production Yield", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                                    InteractiveStackedBarChart(
                                        dataPoints = productionDataPoints,
                                        selectedIndex = selectedDayIndex,
                                        onSelect = { selectedDayIndex = it },
                                        supportedMilkTypes = business?.supportedMilkTypes ?: "BOTH"
                                    )
                                }
                            }
                        }
                    }

                    // ================= SLIDE: OPERATIONS =================
                    "operations" -> {
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                val inventoryLabel = if (currentMode == "RETAIL_PARLOUR" || currentMode == "PROCESSING_UNIT") "📦 Product Inventory" else "🌾 Feed & Inventory"
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    FilterChip(
                                        selected = selectedOperationsSubTab == 0,
                                        onClick = { selectedOperationsSubTab = 0 },
                                        label = { Text("💳 Payment & Dues", fontSize = 11.sp) },
                                        modifier = Modifier.weight(1f)
                                    )
                                    FilterChip(
                                        selected = selectedOperationsSubTab == 1,
                                        onClick = { selectedOperationsSubTab = 1 },
                                        label = { Text("🧾 Expense", fontSize = 11.sp) },
                                        modifier = Modifier.weight(1f)
                                    )
                                    FilterChip(
                                        selected = selectedOperationsSubTab == 2,
                                        onClick = { selectedOperationsSubTab = 2 },
                                        label = { Text(inventoryLabel, fontSize = 11.sp) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                when (selectedOperationsSubTab) {
                                    0 -> {
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(16.dp),
                                            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                                            elevation = CardDefaults.cardElevation(1.5.dp)
                                        ) {
                                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text("💳 Khata Payment & Outstanding Dues", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                                                    TextButton(onClick = onOpenPayments) { Text("Payments Hub →", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = RoyalBluePrimary) }
                                                }

                                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    Surface(shape = RoundedCornerShape(10.dp), color = DairyGreenLight, modifier = Modifier.weight(1f)) {
                                                        Column(modifier = Modifier.padding(10.dp)) {
                                                            Text("Uncollected Receivables", fontSize = 10.sp, color = TextSecondary)
                                                            Text("₹${totalReceivable.toInt()}", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = DairyGreen)
                                                        }
                                                    }
                                                    Surface(shape = RoundedCornerShape(10.dp), color = DangerRed.copy(alpha = 0.1f), modifier = Modifier.weight(1f)) {
                                                        Column(modifier = Modifier.padding(10.dp)) {
                                                            Text("Farmer Payable Dues", fontSize = 10.sp, color = TextSecondary)
                                                            Text("₹${totalPayable.toInt()}", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = DangerRed)
                                                        }
                                                    }
                                                }

                                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    Button(
                                                        onClick = onOpenPayments,
                                                        colors = ButtonDefaults.buttonColors(containerColor = DairyGreen),
                                                        shape = RoundedCornerShape(8.dp),
                                                        modifier = Modifier.weight(1f).height(36.dp)
                                                    ) {
                                                        Text("💵 Record Payment", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                    Button(
                                                        onClick = onOpenCollectionDesk,
                                                        colors = ButtonDefaults.buttonColors(containerColor = RoyalBluePrimary),
                                                        shape = RoundedCornerShape(8.dp),
                                                        modifier = Modifier.weight(1f).height(36.dp)
                                                    ) {
                                                        Text("👨‍🌾 Pay Village Farmer", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    1 -> {
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(16.dp),
                                            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                                            elevation = CardDefaults.cardElevation(1.5.dp)
                                        ) {
                                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text("🧾 Dairy Operating Expenses & Fuel", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                                                    TextButton(onClick = onOpenExpenses) { Text("Expenses Ledger →", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = RoyalBluePrimary) }
                                                }

                                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    Surface(shape = RoundedCornerShape(10.dp), color = FreshGoldLight, modifier = Modifier.weight(1f)) {
                                                        Column(modifier = Modifier.padding(10.dp)) {
                                                            Text("Today Operating Expenses", fontSize = 10.sp, color = TextSecondary)
                                                            Text("₹${totalTodayExpenses.toInt()}", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = WarmHoney)
                                                        }
                                                    }
                                                    Surface(shape = RoundedCornerShape(10.dp), color = RoyalBlueLight, modifier = Modifier.weight(1f)) {
                                                        Column(modifier = Modifier.padding(10.dp)) {
                                                            Text("Personal / Misc Draw", fontSize = 10.sp, color = TextSecondary)
                                                            Text("₹${todayPersonalExpenses.toInt()}", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = RoyalBluePrimary)
                                                        }
                                                    }
                                                }

                                                Button(
                                                    onClick = onOpenExpenses,
                                                    colors = ButtonDefaults.buttonColors(containerColor = WarmHoney),
                                                    shape = RoundedCornerShape(8.dp),
                                                    modifier = Modifier.fillMaxWidth().height(36.dp)
                                                ) {
                                                    Text("➕ Log Dairy Expense", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                                }
                                            }
                                        }
                                    }

                                    2 -> {
                                        val stockTitle = if (currentMode == "RETAIL_PARLOUR" || currentMode == "PROCESSING_UNIT") "📦 Dairy Products & Packaging Stock" else "🌾 Cattle Feed, Fodder & Medicine Stock"
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(16.dp),
                                            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                                            elevation = CardDefaults.cardElevation(1.5.dp)
                                        ) {
                                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(stockTitle, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                                                    TextButton(onClick = onOpenInventory) { Text("Inventory Hub →", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = RoyalBluePrimary) }
                                                }

                                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    Surface(shape = RoundedCornerShape(10.dp), color = SurfaceBg, border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle), modifier = Modifier.weight(1f)) {
                                                        Column(modifier = Modifier.padding(10.dp)) {
                                                            Text("Tracked Stock Items", fontSize = 10.sp, color = TextSecondary)
                                                            Text("${inventoryItems.size} Items", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = RoyalBluePrimary)
                                                        }
                                                    }
                                                    Surface(
                                                        shape = RoundedCornerShape(10.dp),
                                                        color = if (lowStockCount > 0) DangerRed.copy(alpha = 0.1f) else DairyGreenLight,
                                                        modifier = Modifier.weight(1f)
                                                    ) {
                                                        Column(modifier = Modifier.padding(10.dp)) {
                                                            Text("Low Stock Alerts", fontSize = 10.sp, color = TextSecondary)
                                                            Text("$lowStockCount Reorders", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = if (lowStockCount > 0) DangerRed else DairyGreen)
                                                        }
                                                    }
                                                }

                                                Button(
                                                    onClick = onOpenInventory,
                                                    colors = ButtonDefaults.buttonColors(containerColor = RoyalBluePrimary),
                                                    shape = RoundedCornerShape(8.dp),
                                                    modifier = Modifier.fillMaxWidth().height(36.dp)
                                                ) {
                                                    Text("📦 Restock Feed & Inventory", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Master Operations Matrix (Contextually tailored to Business Type)
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("Master Operations Matrix", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    when (currentMode) {
                                        "RETAIL_PARLOUR" -> {
                                            QuickActionRound(
                                                title = "Counter POS",
                                                icon = Icons.Default.PointOfSale,
                                                badgeText = "Live POS",
                                                onClick = { onNavigateToTab(1) },
                                                modifier = Modifier.weight(1f)
                                            )
                                            QuickActionRound(
                                                title = "Inventory",
                                                icon = Icons.Default.Inventory2,
                                                badgeText = "${inventoryItems.size}",
                                                onClick = onOpenInventory,
                                                modifier = Modifier.weight(1f)
                                            )
                                            QuickActionRound(
                                                title = "Customers",
                                                icon = Icons.Default.People,
                                                badgeText = "${customers.size}",
                                                onClick = { onNavigateToTab(2) },
                                                modifier = Modifier.weight(1f)
                                            )
                                            QuickActionRound(
                                                title = "Expenses",
                                                icon = Icons.Default.ReceiptLong,
                                                badgeText = "₹${totalTodayExpenses.toInt()}",
                                                onClick = onOpenExpenses,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }

                                        "COLLECTION_CENTER" -> {
                                            QuickActionRound(
                                                title = "Collection",
                                                icon = Icons.Default.WaterDrop,
                                                badgeText = "${todayCollections.size}",
                                                onClick = { onNavigateToTab(1) },
                                                modifier = Modifier.weight(1f)
                                            )
                                            QuickActionRound(
                                                title = "Farmers",
                                                icon = Icons.Default.People,
                                                badgeText = "${farmers.size}",
                                                onClick = { onNavigateToTab(2) },
                                                modifier = Modifier.weight(1f)
                                            )
                                            QuickActionRound(
                                                title = "Dispatches",
                                                icon = Icons.Default.LocalShipping,
                                                badgeText = "${bulkDispatches.size}",
                                                onClick = { onNavigateToTab(3) },
                                                modifier = Modifier.weight(1f)
                                            )
                                            QuickActionRound(
                                                title = "Expenses",
                                                icon = Icons.Default.ReceiptLong,
                                                badgeText = "₹${totalTodayExpenses.toInt()}",
                                                onClick = onOpenExpenses,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }

                                        "TRADER" -> {
                                            QuickActionRound(
                                                title = "Procurement",
                                                icon = Icons.Default.AddShoppingCart,
                                                badgeText = "${todayCollections.size}",
                                                onClick = { onNavigateToTab(1) },
                                                modifier = Modifier.weight(1f)
                                            )
                                            QuickActionRound(
                                                title = "Routes",
                                                icon = Icons.Default.LocalShipping,
                                                badgeText = "$completedDeliveries",
                                                onClick = { onNavigateToTab(2) },
                                                modifier = Modifier.weight(1f)
                                            )
                                            QuickActionRound(
                                                title = "Ledger",
                                                icon = Icons.Default.ReceiptLong,
                                                badgeText = "₹${totalReceivable.toInt()}",
                                                onClick = { onNavigateToTab(3) },
                                                modifier = Modifier.weight(1f)
                                            )
                                            QuickActionRound(
                                                title = "Expenses",
                                                icon = Icons.Default.ReceiptLong,
                                                badgeText = "₹${totalTodayExpenses.toInt()}",
                                                onClick = onOpenExpenses,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }

                                        else -> {
                                            // FARMER, GAUSHALA, INTEGRATED, FARMER_WHOLESALE
                                            QuickActionRound(
                                                title = "Deliveries",
                                                icon = Icons.Default.LocalShipping,
                                                badgeText = "$completedDeliveries/$totalCustomerTarget",
                                                onClick = { onNavigateToTab(1) },
                                                modifier = Modifier.weight(1f)
                                            )
                                            QuickActionRound(
                                                title = "Customers",
                                                icon = Icons.Default.People,
                                                badgeText = "${customers.size}",
                                                onClick = { onNavigateToTab(2) },
                                                modifier = Modifier.weight(1f)
                                            )
                                            QuickActionRound(
                                                title = "Payments",
                                                icon = Icons.Default.Payment,
                                                badgeText = "₹${totalReceivable.toInt()}",
                                                onClick = { onNavigateToTab(3) },
                                                modifier = Modifier.weight(1f)
                                            )
                                            QuickActionRound(
                                                title = "Expenses",
                                                icon = Icons.Default.ReceiptLong,
                                                badgeText = "₹${totalTodayExpenses.toInt()}",
                                                onClick = onOpenExpenses,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ================= SLIDE: FINANCIALS =================
                    "financials" -> {
                        // 1. Financial Health Hero Card
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = RoyalBluePrimary),
                                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                            ) {
                                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Box(
                                                modifier = Modifier.size(32.dp).background(Color.White.copy(alpha = 0.2f), shape = CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(Icons.Default.AccountBalance, contentDescription = null, tint = FreshGold, modifier = Modifier.size(18.dp))
                                            }
                                            Column {
                                                Text("Financial Health", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                                                Text("Real-time Dairy Ledger Status", fontSize = 10.sp, color = Color.White.copy(alpha = 0.8f))
                                            }
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = Color.White.copy(alpha = 0.2f),
                                            modifier = Modifier.clickable { onOpenProfit() }
                                        ) {
                                            Text("P&L Spread →", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Bottom
                                    ) {
                                        Column {
                                            Text(
                                                text = String.format(Locale.US, "₹%.0f", netCashMovement),
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color.White,
                                                fontSize = 32.sp
                                            )
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Icon(
                                                    imageVector = if (netCashMovement >= 0) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                                                    contentDescription = null,
                                                    tint = if (netCashMovement >= 0) Color(0xFF4ADE80) else Color(0xFFF87171),
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Text(
                                                    text = if (netCashMovement >= 0) "Profitable Operating Margin" else "High Expense Session",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = if (netCashMovement >= 0) Color(0xFF4ADE80) else Color(0xFFFCA5A5)
                                                )
                                            }
                                        }

                                        Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                                            Canvas(modifier = Modifier.size(44.dp)) {
                                                drawArc(
                                                    color = Color.White.copy(alpha = 0.25f),
                                                    startAngle = -90f,
                                                    sweepAngle = 360f,
                                                    useCenter = false,
                                                    style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
                                                )
                                                drawArc(
                                                    color = Color(0xFF4ADE80),
                                                    startAngle = -90f,
                                                    sweepAngle = (completionPercent / 100f) * 360f,
                                                    useCenter = false,
                                                    style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
                                                )
                                            }
                                            Text(text = "$completionPercent%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        }
                                    }
                                }
                            }
                        }

                        // 2. Revenue & Financial P&L Margin Distribution Chart
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text("📊 Revenue & Financial P&L Margin Distribution", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                                    InteractiveProductionDonut(
                                        cowLiters = cowSoldLiters,
                                        buffLiters = buffSoldLiters,
                                        morningLiters = milkSoldLiters * 0.55,
                                        eveningLiters = milkSoldLiters * 0.45,
                                        cowRate = business?.cowMilkRate ?: 55.0,
                                        buffRate = business?.buffaloMilkRate ?: 75.0,
                                        supportedMilkTypes = business?.supportedMilkTypes ?: "BOTH"
                                    )
                                }
                            }
                        }

                        // 3. Real-time Balance Sheet
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                                elevation = CardDefaults.cardElevation(1.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text("💳 Ledger Balance Sheet", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        DrillDownStatPill(
                                            title = "Receivables (In)",
                                            value = "₹${totalReceivable.toInt()}",
                                            subtitle = "${customers.count { it.outstandingBalance > 0 }} parties pending",
                                            accentColor = DangerRed,
                                            modifier = Modifier.weight(1f)
                                        )
                                        DrillDownStatPill(
                                            title = "Farmer Payables",
                                            value = "₹${totalPayable.toInt()}",
                                            subtitle = "${customers.count { it.outstandingBalance < 0 }} payouts due",
                                            accentColor = FreshGold,
                                            modifier = Modifier.weight(1f)
                                        )
                                        DrillDownStatPill(
                                            title = "Collected Today",
                                            value = "₹${todayCollectedPayments.toInt()}",
                                            subtitle = "Cash & UPI logged",
                                            accentColor = DairyGreen,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
"""

updated_content = content[:m.start(1)] + today_formatted_part + new_body + "\n\n" + custom_formula_part + content[m.end(2):]

with open('app/src/main/java/com/example/ui/screens/DashboardScreen.kt', 'w') as f:
    f.write(updated_content)

print("DashboardScreen.kt updated successfully!")
