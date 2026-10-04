package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.MilkMateViewModel
import com.example.ui.theme.*

@Composable
fun SubscriptionScreen(viewModel: MilkMateViewModel, onBack: (() -> Unit)? = null) {
    val business by viewModel.currentBusiness.collectAsStateWithLifecycle()
    val customers by viewModel.customers.collectAsStateWithLifecycle()

    val isPro = business?.subscriptionPlan == "PRO"
    val individualCount = customers.count { it.type == "INDIVIDUAL" }
    val bulkCount = customers.count { it.type == "BULK_BUYER" }
    val supplierCount = customers.count { it.type == "SUPPLIER" }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (onBack != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
                Text(
                    text = "Subscription & Plans",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        // Status Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isPro) FreshGoldLight else RoyalBlueLight
            )
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isPro) "PRO SUBSCRIPTION ACTIVE" else "FREE TIER ACTIVE",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isPro) Color(0xFFE65100) else RoyalBluePrimary
                        )
                        Text(
                            text = if (isPro) "All customer limits unlocked" else "Full app features included with standard limits",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Icon(
                        imageVector = if (isPro) Icons.Default.Verified else Icons.Default.Shield,
                        contentDescription = null,
                        tint = if (isPro) FreshGold else RoyalBluePrimary,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(text = "Current Usage:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text("• Individual Customers: $individualCount / ${if (isPro) "∞" else "25"}")
                Text("• Bulk Buyers: $bulkCount / ${if (isPro) "∞" else "5"}")
                Text("• Suppliers: $supplierCount / ${if (isPro) "∞" else "5"}")
            }
        }

        // Pro Monthly Plan Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "MilkMate Pro Monthly",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Billed monthly • Cancel anytime",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Text(
                        text = "₹49",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = RoyalBluePrimary
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                ProFeatureItem("Unlimited Individual Customers (overcome 25 limit)")
                ProFeatureItem("Unlimited Bulk Buyers (overcome 5 limit)")
                ProFeatureItem("Unlimited Milk Suppliers (overcome 5 limit)")
                ProFeatureItem("All reports, FAT/SNF, PDF & Excel export included")

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { viewModel.upgradeToPro("MONTHLY") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("subscribe_monthly_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = RoyalBluePrimary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(if (isPro) "Renew Monthly Plan (₹49)" else "Upgrade Monthly (₹49)")
                }
            }
        }

        // Pro Yearly Plan Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "MilkMate Pro Yearly",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(shape = RoundedCornerShape(4.dp), color = DairyGreenLight) {
                                Text(
                                    text = "SAVE 15%",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DairyGreen,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "Full year of unlimited dairy scaling",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Text(
                        text = "₹499",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = DairyGreen
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                ProFeatureItem("Unlimited Individual Customers")
                ProFeatureItem("Unlimited Bulk Buyers & Suppliers")
                ProFeatureItem("Priority cloud backup and multi-device sync")

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { viewModel.upgradeToPro("YEARLY") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("subscribe_yearly_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = DairyGreen),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(if (isPro) "Renew Yearly Plan (₹499)" else "Upgrade Yearly (₹499)")
                }
            }
        }

        // Policy & Transparency Note
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "MilkMate Entitlement Promise",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Even if a subscription expires, existing customer records remain 100% accessible. Deliveries, billing, reports, and backup continue to work without disruption.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ProFeatureItem(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = null,
            tint = DairyGreen,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = text, style = MaterialTheme.typography.bodyMedium)
    }
}
