package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.BusinessEntity
import com.example.data.local.entity.CustomerEntity
import com.example.data.local.entity.DeliveryEntity
import com.example.data.local.entity.InventoryItemEntity
import com.example.data.remote.gemini.*
import com.example.ui.MilkMateViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeminiChatScreen(
    viewModel: MilkMateViewModel,
    onBack: () -> Unit,
    initialPrompt: String? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val geminiService = remember { GeminiService() }

    // Chat state
    val messages = remember {
        mutableStateListOf<ChatMessage>().apply {
            add(
                ChatMessage(
                    role = "model",
                    content = "Hello! I am **MilkMate AI**, your dedicated Dairy Operations and Veterinary Consultant.\n\n" +
                            "I can assist you with:\n" +
                            "• **Cattle Health & Nutrition**: TMR feed formulation, bypass protein, increasing fat & SNF.\n" +
                            "• **Live Market Intelligence**: Real-time milk rates & feed wholesale prices via **Google Search Grounding**.\n" +
                            "• **Veterinary & Infrastructure**: Locating nearby veterinary clinics, AI centers, and chilling plants via **Google Maps Grounding**.\n" +
                            "• **Farm Economics**: Break-even analysis, billing, and route delivery optimization.\n\n" +
                            "How can I help your dairy farm today?",
                    modelUsed = "gemini-3.5-flash"
                )
            )
        }
    }

    var inputText by remember { mutableStateOf(initialPrompt ?: "") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Model & Grounding tools selection
    var selectedModel by remember { mutableStateOf(GeminiModel.FLASH_35) }
    var enableSearch by remember { mutableStateOf(true) }
    var enableMaps by remember { mutableStateOf(false) }
    var includeDairyContext by remember { mutableStateOf(true) }
    var showModelMenu by remember { mutableStateOf(false) }

    // Live farm context from ViewModel
    val currentBusiness by viewModel.currentBusiness.collectAsStateWithLifecycle()
    val allCustomers by viewModel.customers.collectAsStateWithLifecycle()
    val todayDeliveries by viewModel.todayDeliveries.collectAsStateWithLifecycle()
    val inventoryItems by viewModel.inventoryItems.collectAsStateWithLifecycle()

    val farmContext = remember(currentBusiness, allCustomers, todayDeliveries, inventoryItems, includeDairyContext) {
        if (!includeDairyContext) null
        else {
            val totalCust = allCustomers.size
            val cowCust = allCustomers.count { it.milkType == "COW" || it.milkType == "BOTH" }
            val buffCust = allCustomers.count { it.milkType == "BUFFALO" || it.milkType == "BOTH" }
            val deliveredToday = todayDeliveries.filter { it.isDelivered }.sumOf { it.quantityLiters }
            val pendingDues = allCustomers.sumOf { it.outstandingBalance }
            val feedStock = inventoryItems.joinToString { "${it.itemName}: ${it.currentStock} ${it.unit}" }

            val supportedTypes = currentBusiness?.supportedMilkTypes ?: "BOTH"
            val rateInfo = when (supportedTypes) {
                "COW_ONLY" -> "Base Cow Milk Rate: ₹${currentBusiness?.cowMilkRate ?: 50.0}/L"
                "BUFFALO_ONLY" -> "Base Buffalo Milk Rate: ₹${currentBusiness?.buffaloMilkRate ?: 65.0}/L"
                else -> "Base Cow Milk Rate: ₹${currentBusiness?.cowMilkRate ?: 50.0}/L, Base Buffalo Milk Rate: ₹${currentBusiness?.buffaloMilkRate ?: 65.0}/L"
            }
            val custInfo = when (supportedTypes) {
                "COW_ONLY" -> "Total Active Customers: $totalCust"
                "BUFFALO_ONLY" -> "Total Active Customers: $totalCust"
                else -> "Total Active Customers: $totalCust (Cow: $cowCust, Buffalo: $buffCust)"
            }

            """
            Dairy Business: ${currentBusiness?.businessName ?: "MilkMate Dairy"}
            Owner: ${currentBusiness?.ownerName ?: "Manager"} (Phone: ${currentBusiness?.phone ?: "N/A"})
            $rateInfo
            $custInfo
            Milk Delivered Today: ${String.format(java.util.Locale.US, "%.1f", deliveredToday)} Liters
            Total Outstanding Customer Dues: ₹${pendingDues.toInt()}
            Current Warehouse Feed & Medicine Stock: $feedStock
            """.trimIndent()
        }
    }

    fun handleSend(textToSend: String = inputText) {
        val trimmed = textToSend.trim()
        if (trimmed.isBlank() || isLoading) return

        val userMsg = ChatMessage(role = "user", content = trimmed)
        messages.add(userMsg)
        inputText = ""
        isLoading = true
        errorMessage = null

        coroutineScope.launch {
            listState.animateScrollToItem(messages.size - 1)
            val result = geminiService.sendMessage(
                history = messages.dropLast(1),
                userMessage = trimmed,
                model = selectedModel,
                enableSearch = enableSearch,
                enableMaps = enableMaps,
                businessContext = farmContext
            )

            isLoading = false
            result.onSuccess { aiMsg ->
                messages.add(aiMsg)
                listState.animateScrollToItem(messages.size - 1)
            }.onFailure { err ->
                val errText = err.localizedMessage ?: "Failed to get AI response. Please verify your connection."
                errorMessage = errText
                messages.add(
                    ChatMessage(
                        role = "model",
                        content = "⚠️ **Error**: $errText\n\nPlease check that your Gemini API key is valid in the Secrets panel, or tap Retry.",
                        isError = true
                    )
                )
                listState.animateScrollToItem(messages.size - 1)
            }
        }
    }

    LaunchedEffect(initialPrompt) {
        if (!initialPrompt.isNullOrBlank()) {
            handleSend(initialPrompt)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(RoyalBluePrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "AI",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "MilkMate AI",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = RoyalBlueLight
                                ) {
                                    Text(
                                        text = selectedModel.badge,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = RoyalBluePrimary,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            Text(
                                text = if (isLoading) "Thinking..." else "Powered by Gemini & Google Grounding",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Model Switcher
                    Box {
                        IconButton(onClick = { showModelMenu = true }) {
                            Icon(Icons.Default.Tune, contentDescription = "Select Gemini Model", tint = RoyalBluePrimary)
                        }

                        DropdownMenu(
                            expanded = showModelMenu,
                            onDismissRequest = { showModelMenu = false }
                        ) {
                            Text(
                                text = "Select Gemini Engine",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                            HorizontalDivider()
                            GeminiModel.values().forEach { model ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = model.displayName,
                                                    fontWeight = if (selectedModel == model) FontWeight.Bold else FontWeight.Normal,
                                                    fontSize = 13.sp
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "(${model.badge})",
                                                    fontSize = 10.sp,
                                                    color = RoyalBluePrimary
                                                )
                                            }
                                            Text(
                                                text = model.description,
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    },
                                    onClick = {
                                        selectedModel = model
                                        showModelMenu = false
                                    },
                                    leadingIcon = {
                                        if (selectedModel == model) {
                                            Icon(Icons.Default.Check, contentDescription = null, tint = RoyalBluePrimary)
                                        }
                                    }
                                )
                            }
                        }
                    }

                    // Clear Chat
                    IconButton(
                        onClick = {
                            messages.clear()
                            messages.add(
                                ChatMessage(
                                    role = "model",
                                    content = "Chat cleared! How can I assist you with your dairy farm, cattle nutrition, or milk sales?",
                                    modelUsed = selectedModel.modelId
                                )
                            )
                        }
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Clear Chat", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Grounding & Farm Context Control Bar
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Google Search Grounding Chip
                    FilterChip(
                        selected = enableSearch,
                        onClick = { enableSearch = !enableSearch },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = if (enableSearch) RoyalBluePrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        label = {
                            Text(
                                text = "Live Search",
                                fontSize = 11.sp,
                                fontWeight = if (enableSearch) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )

                    // Google Maps Grounding Chip
                    FilterChip(
                        selected = enableMaps,
                        onClick = { enableMaps = !enableMaps },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Place,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = if (enableMaps) DangerRed else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        label = {
                            Text(
                                text = "Nearby Maps",
                                fontSize = 11.sp,
                                fontWeight = if (enableMaps) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )

                    // Include My Farm Data Chip
                    FilterChip(
                        selected = includeDairyContext,
                        onClick = { includeDairyContext = !includeDairyContext },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Assessment,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = if (includeDairyContext) DairyGreen else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        label = {
                            Text(
                                text = "Farm Stats Sync",
                                fontSize = 11.sp,
                                fontWeight = if (includeDairyContext) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            // Quick Topic Prompt Suggestions (Horizontal Scroll)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val promptTargetCattle = when (currentBusiness?.supportedMilkTypes) {
                    "COW_ONLY" -> "dairy cows"
                    "BUFFALO_ONLY" -> "dairy buffaloes"
                    else -> "dairy cows and buffaloes"
                }
                QuickTopicChip(
                    text = "🥛 Boost Milk Fat & SNF",
                    onClick = { handleSend("What is the best cattle feed formula to naturally increase milk fat and SNF in $promptTargetCattle?") }
                )
                QuickTopicChip(
                    text = "🔍 Live Feed Market Rates",
                    onClick = {
                        enableSearch = true
                        handleSend("What are the current market wholesale prices for cattle feed pellets, cotton seed cake (kapasiya khali), and mustard de-oiled cake in India?")
                    }
                )
                QuickTopicChip(
                    text = "📍 Nearby Vet Clinics & AI",
                    onClick = {
                        enableMaps = true
                        handleSend("Find the nearest government veterinary hospitals, artificial insemination centers, and milk chilling plants near me.")
                    }
                )
                QuickTopicChip(
                    text = "📊 Analyze My Farm Profit",
                    onClick = {
                        includeDairyContext = true
                        handleSend("Analyze my current dairy numbers: look at my customers, today's delivered liters, dues, and feed inventory. Suggest 3 specific actions to boost profitability.")
                    }
                )
                QuickTopicChip(
                    text = "🩺 Mastitis & Disease Care",
                    onClick = { handleSend("What are the early clinical signs of subclinical mastitis in dairy cattle, and what immediate sanitary measures and treatment protocol should be followed?") }
                )
            }

            // Message List
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(messages, key = { it.id }) { msg ->
                    ChatBubble(
                        message = msg,
                        onOpenUrl = { url ->
                            try {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                            } catch (e: Exception) {
                                Toast.makeText(context, "Cannot open browser link", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onCopy = { content ->
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("MilkMate AI", content))
                            Toast.makeText(context, "Copied to clipboard ✓", Toast.LENGTH_SHORT).show()
                        }
                    )
                }

                if (isLoading) {
                    item {
                        ThinkingIndicator(modelName = selectedModel.displayName)
                    }
                }
            }

            // Error Notice Banner
            if (errorMessage != null) {
                Surface(
                    color = DangerRed.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = DangerRed, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = errorMessage ?: "",
                            fontSize = 11.sp,
                            color = DangerRed,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Input Bar
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = {
                            Text(
                                text = if (enableSearch) "Ask dairy AI (Live Search active)..." else "Ask MilkMate AI...",
                                fontSize = 13.sp
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp, max = 110.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RoyalBluePrimary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        ),
                        singleLine = false,
                        maxLines = 4
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = { handleSend() },
                        enabled = inputText.isNotBlank() && !isLoading,
                        modifier = Modifier
                            .size(46.dp)
                            .background(
                                color = if (inputText.isNotBlank() && !isLoading) RoyalBluePrimary else Color.LightGray,
                                shape = CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun QuickTopicChip(
    text: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = RoyalBluePrimary,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

@Composable
fun ChatBubble(
    message: ChatMessage,
    onOpenUrl: (String) -> Unit,
    onCopy: (String) -> Unit
) {
    val isUser = message.role == "user"

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(if (isUser) 0.85f else 0.95f),
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
        ) {
            if (!isUser) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(RoyalBluePrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "AI",
                        tint = Color.White,
                        modifier = Modifier.size(15.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
            }

            Card(
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (isUser) 16.dp else 4.dp,
                    bottomEnd = if (isUser) 4.dp else 16.dp
                ),
                colors = CardDefaults.cardColors(
                    containerColor = when {
                        message.isError -> DangerRed.copy(alpha = 0.12f)
                        isUser -> RoyalBluePrimary
                        else -> MaterialTheme.colorScheme.surface
                    }
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = if (isUser) 0.dp else 1.dp),
                modifier = Modifier.border(
                    width = if (isUser || message.isError) 0.dp else 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(16.dp)
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    // Content text
                    Text(
                        text = message.content,
                        color = if (isUser) Color.White else MaterialTheme.colorScheme.onSurface,
                        fontSize = 13.sp,
                        lineHeight = 19.sp
                    )

                    // Grounding Sources (Search & Maps)
                    if (message.searchSources.isNotEmpty() || message.mapSources.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        Spacer(modifier = Modifier.height(6.dp))

                        if (message.searchSources.isNotEmpty()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Language, contentDescription = null, tint = RoyalBluePrimary, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Live Google Search Sources:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = RoyalBluePrimary)
                            }
                            Spacer(modifier = Modifier.height(4.dp))

                            FlowRowLayout(
                                spacing = 4.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                message.searchSources.take(4).forEach { src ->
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = RoyalBlueLight,
                                        modifier = Modifier.clickable { onOpenUrl(src.uri) }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.OpenInNew, contentDescription = null, tint = RoyalBluePrimary, modifier = Modifier.size(10.dp))
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = src.title,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = RoyalBluePrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (message.mapSources.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Place, contentDescription = null, tint = DangerRed, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Google Maps Locations:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = DangerRed)
                            }
                            Spacer(modifier = Modifier.height(4.dp))

                            message.mapSources.take(3).forEach { map ->
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = DangerRed.copy(alpha = 0.08f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp)
                                        .clickable {
                                            val query = map.uri.ifBlank { "https://www.google.com/maps/search/?api=1&query=${java.net.URLEncoder.encode(map.title + " " + map.address, "UTF-8")}" }
                                            onOpenUrl(query)
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = DangerRed, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Column {
                                            Text(text = map.title, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            if (map.address.isNotBlank()) {
                                                Text(text = map.address, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Footer with model badge and copy button for AI messages
                    if (!isUser) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (message.modelUsed.isNotBlank()) message.modelUsed else "Gemini",
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            IconButton(
                                onClick = { onCopy(message.content) },
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy text", modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FlowRowLayout(
    spacing: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(spacing)
    ) {
        content()
    }
}

@Composable
fun ThinkingIndicator(modelName: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(RoyalBluePrimary),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
        }
        Spacer(modifier = Modifier.width(8.dp))
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 2.dp,
                    color = RoyalBluePrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Consulting $modelName...",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
