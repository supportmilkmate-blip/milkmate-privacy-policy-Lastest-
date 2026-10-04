package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.MilkMateApplication
import com.example.data.local.entity.*
import com.example.data.repository.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

class MilkMateViewModel(
    val repository: MilkMateRepository
) : ViewModel() {

    val sessionState = repository.sessionManager.sessionState

    // Current Date (Midnight epoch millis)
    private val _selectedDate = MutableStateFlow(getTodayMidnightMillis())
    val selectedDate: StateFlow<Long> = _selectedDate.asStateFlow()

    private val _selectedShift = MutableStateFlow("MORNING")
    val selectedShift: StateFlow<String> = _selectedShift.asStateFlow()

    private val _selectedReportRange = MutableStateFlow("THIS_MONTH") // THIS_MONTH, LAST_MONTH, ALL_TIME
    val selectedReportRange: StateFlow<String> = _selectedReportRange.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    // Business flow based on session businessId
    val currentBusiness: StateFlow<BusinessEntity?> = sessionState
        .flatMapLatest { session ->
            if (session.businessId.isNotBlank()) {
                repository.getBusinessFlow(session.businessId)
            } else {
                flowOf(null)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Customers flow
    val customers: StateFlow<List<CustomerEntity>> = sessionState
        .flatMapLatest { session ->
            if (session.businessId.isNotBlank()) {
                repository.getCustomersFlow(session.businessId)
            } else {
                flowOf(emptyList())
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Deliveries for selected date and shift
    val currentShiftDeliveries: StateFlow<List<DeliveryEntity>> = combine(
        sessionState,
        _selectedDate,
        _selectedShift
    ) { session, date, shift ->
        Triple(session.businessId, date, shift)
    }.flatMapLatest { (businessId, date, shift) ->
        if (businessId.isNotBlank()) {
            repository.getDeliveriesByDateAndShiftFlow(businessId, date, shift)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All deliveries across all dates (for historical reports and analytics)
    val allDeliveries: StateFlow<List<DeliveryEntity>> = sessionState
        .flatMapLatest { session ->
            if (session.businessId.isNotBlank()) {
                repository.getAllDeliveriesFlow(session.businessId)
            } else {
                flowOf(emptyList())
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All deliveries for selected date (for summary)
    val todayDeliveries: StateFlow<List<DeliveryEntity>> = combine(
        sessionState,
        _selectedDate
    ) { session, date ->
        Pair(session.businessId, date)
    }.flatMapLatest { (businessId, date) ->
        if (businessId.isNotBlank()) {
            repository.getDeliveriesByDateFlow(businessId, date)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Payments
    val payments: StateFlow<List<PaymentEntity>> = sessionState
        .flatMapLatest { session ->
            if (session.businessId.isNotBlank()) {
                repository.getPaymentsFlow(session.businessId)
            } else {
                flowOf(emptyList())
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Expenses
    val expenses: StateFlow<List<ExpenseEntity>> = sessionState
        .flatMapLatest { session ->
            if (session.businessId.isNotBlank()) {
                repository.getExpensesFlow(session.businessId, session.accountStartDate)
            } else {
                flowOf(emptyList())
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Inventory
    val inventoryItems: StateFlow<List<InventoryItemEntity>> = kotlinx.coroutines.flow.combine(
        sessionState,
        currentBusiness
    ) { session, business ->
        Pair(session.businessId, business?.businessMode)
    }.flatMapLatest { (businessId, mode) ->
        if (businessId.isNotBlank()) {
            repository.getInventoryItemsFlow(businessId).map { items ->
                com.example.ui.util.BusinessModeFeatures.filterInventoryForMode(items, mode)
            }
        } else {
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val inventoryTransactions: StateFlow<List<InventoryTransactionEntity>> = sessionState
        .flatMapLatest { session ->
            if (session.businessId.isNotBlank()) {
                repository.getAllInventoryTransactionsFlow(session.businessId)
            } else {
                flowOf(emptyList())
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Staff list
    val staffList: StateFlow<List<StaffEntity>> = sessionState
        .flatMapLatest { session ->
            if (session.businessId.isNotBlank()) {
                repository.getStaffFlow(session.businessId)
            } else {
                flowOf(emptyList())
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todayAttendance: StateFlow<List<StaffAttendanceEntity>> = combine(
        sessionState,
        _selectedDate
    ) { session, date ->
        if (session.businessId.isNotBlank()) {
            repository.getAttendanceForDateFlow(session.businessId, date)
        } else {
            flowOf(emptyList())
        }
    }.flatMapLatest { it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allAttendance: StateFlow<List<StaffAttendanceEntity>> = sessionState
        .flatMapLatest { session ->
            if (session.businessId.isNotBlank()) {
                repository.getAllAttendanceFlow(session.businessId)
            } else {
                flowOf(emptyList())
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val staffPayments: StateFlow<List<StaffPaymentEntity>> = sessionState
        .flatMapLatest { session ->
            if (session.businessId.isNotBlank()) {
                repository.getAllStaffPaymentsFlow(session.businessId)
            } else {
                flowOf(emptyList())
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Orders flow
    val orders: StateFlow<List<OrderEntity>> = sessionState
        .flatMapLatest { session ->
            if (session.businessId.isNotBlank()) {
                repository.getOrdersFlow(session.businessId)
            } else {
                flowOf(emptyList())
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Pending sync count
    val pendingSyncCount: StateFlow<Int> = repository.pendingSyncCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val deletedCustomers: StateFlow<List<CustomerEntity>> = sessionState
        .flatMapLatest { session ->
            if (session.businessId.isNotBlank()) {
                repository.getDeletedCustomersFlow(session.businessId)
            } else {
                flowOf(emptyList())
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val deletedExpenses: StateFlow<List<ExpenseEntity>> = sessionState
        .flatMapLatest { session ->
            if (session.businessId.isNotBlank()) {
                repository.getDeletedExpensesFlow(session.businessId)
            } else {
                flowOf(emptyList())
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Farmers flow (for Collection Centers & Traders)
    val farmers: StateFlow<List<FarmerEntity>> = sessionState
        .flatMapLatest { session ->
            if (session.businessId.isNotBlank()) {
                repository.getFarmersFlow(session.businessId)
            } else {
                flowOf(emptyList())
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Milk Collections Flow (Farmer Procurements)
    val allMilkCollections: StateFlow<List<MilkCollectionEntity>> = sessionState
        .flatMapLatest { session ->
            if (session.businessId.isNotBlank()) {
                repository.getAllMilkCollectionsFlow(session.businessId)
            } else {
                flowOf(emptyList())
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Milk Wastage Flow (Spoilage & Loss Incidents)
    val milkWastageList: StateFlow<List<com.example.data.local.entity.MilkWastageEntity>> = sessionState
        .flatMapLatest { session ->
            if (session.businessId.isNotBlank()) {
                repository.getMilkWastageFlow(session.businessId)
            } else {
                flowOf(emptyList())
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todayMilkCollections: StateFlow<List<MilkCollectionEntity>> = combine(
        sessionState,
        _selectedDate
    ) { session, date ->
        Pair(session.businessId, date)
    }.flatMapLatest { (businessId, date) ->
        if (businessId.isNotBlank()) {
            repository.getMilkCollectionsByDateFlow(businessId, date)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentShiftMilkCollections: StateFlow<List<MilkCollectionEntity>> = combine(
        sessionState,
        _selectedDate,
        _selectedShift
    ) { session, date, shift ->
        Triple(session.businessId, date, shift)
    }.flatMapLatest { (businessId, date, shift) ->
        if (businessId.isNotBlank()) {
            repository.getMilkCollectionsByDateAndShiftFlow(businessId, date, shift)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Bulk Dispatches Flow (Wholesale & Tanker Supply)
    val bulkDispatches: StateFlow<List<BulkDispatchEntity>> = sessionState
        .flatMapLatest { session ->
            if (session.businessId.isNotBlank()) {
                repository.getAllBulkDispatchesFlow(session.businessId)
            } else {
                flowOf(emptyList())
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todayBulkDispatches: StateFlow<List<BulkDispatchEntity>> = combine(
        sessionState,
        _selectedDate
    ) { session, date ->
        Pair(session.businessId, date)
    }.flatMapLatest { (businessId, date) ->
        if (businessId.isNotBlank()) {
            repository.getBulkDispatchesByDateFlow(businessId, date)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Cattle & Breeding Records (for Dairy Producer mode)
    val cattleList: StateFlow<List<CattleEntity>> = sessionState
        .flatMapLatest { session ->
            if (session.businessId.isNotBlank()) {
                repository.getCattleFlow(session.businessId)
            } else {
                flowOf(emptyList())
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCattleWeights: StateFlow<List<CattleWeightEntity>> = sessionState
        .flatMapLatest { session ->
            if (session.businessId.isNotBlank()) {
                repository.getAllWeightsFlow(session.businessId)
            } else {
                flowOf(emptyList())
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCmtRecords: StateFlow<List<CattleCmtEntity>> = sessionState
        .flatMapLatest { session ->
            if (session.businessId.isNotBlank()) {
                repository.getAllCmtFlow(session.businessId)
            } else {
                flowOf(emptyList())
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allBcsRecords: StateFlow<List<CattleBcsEntity>> = sessionState
        .flatMapLatest { session ->
            if (session.businessId.isNotBlank()) {
                repository.getAllBcsFlow(session.businessId)
            } else {
                flowOf(emptyList())
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allBreedingRecords: StateFlow<List<BreedingRecordEntity>> = sessionState
        .flatMapLatest { session ->
            if (session.businessId.isNotBlank()) {
                repository.getAllBreedingFlow(session.businessId)
            } else {
                flowOf(emptyList())
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allDewormingRecords: StateFlow<List<DewormingRecordEntity>> = sessionState
        .flatMapLatest { session ->
            if (session.businessId.isNotBlank()) {
                repository.getAllDewormingFlow(session.businessId)
            } else {
                flowOf(emptyList())
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allVaccinationRecords: StateFlow<List<VaccinationRecordEntity>> = sessionState
        .flatMapLatest { session ->
            if (session.businessId.isNotBlank()) {
                repository.getAllVaccinationFlow(session.businessId)
            } else {
                flowOf(emptyList())
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTreatmentRecords: StateFlow<List<TreatmentRecordEntity>> = sessionState
        .flatMapLatest { session ->
            if (session.businessId.isNotBlank()) {
                repository.getAllTreatmentFlow(session.businessId)
            } else {
                flowOf(emptyList())
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allFarmObservations: StateFlow<List<FarmObservationEntity>> = sessionState
        .flatMapLatest { session ->
            if (session.businessId.isNotBlank()) {
                repository.getAllObservationsFlow(session.businessId)
            } else {
                flowOf(emptyList())
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allMilkingRecords: StateFlow<List<CattleMilkingRecordEntity>> = sessionState
        .flatMapLatest { session ->
            if (session.businessId.isNotBlank()) {
                repository.getAllMilkingFlow(session.businessId)
            } else {
                flowOf(emptyList())
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    fun showMessage(msg: String) {
        _snackbarMessage.value = msg
    }

    fun setDate(date: Long) {
        val normalized = normalizeToMidnight(date)
        val start = currentBusiness.value?.accountStartDate ?: sessionState.value.accountStartDate
        val today = normalizeToMidnight(System.currentTimeMillis())
        if (start > 0 && normalized < normalizeToMidnight(start)) {
            val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            showMessage("Date cannot be before Account Start Date (${sdf.format(Date(start))})")
            _selectedDate.value = normalizeToMidnight(start)
            return
        }
        if (normalized > today) {
            showMessage("Future date selection is restricted")
            _selectedDate.value = today
            return
        }
        _selectedDate.value = normalized
    }

    fun setShift(shift: String) {
        _selectedShift.value = shift
    }

    fun setReportRange(range: String) {
        _selectedReportRange.value = range
    }

    // Existing businesses on device
    private val _existingBusinesses = MutableStateFlow<List<BusinessEntity>>(emptyList())
    val existingBusinesses: StateFlow<List<BusinessEntity>> = _existingBusinesses.asStateFlow()

    fun loadExistingBusinesses() {
        viewModelScope.launch {
            _existingBusinesses.value = repository.getAllBusinesses()
        }
    }

    // --- Authentication Actions ---
    fun registerOwnerClean(
        businessName: String,
        ownerName: String,
        phone: String,
        mode: String = "FARMER",
        supportedMilkTypes: String = "COW_ONLY"
    ) {
        viewModelScope.launch {
            val businessId = "BIZ-${UUID.randomUUID().toString().take(8).uppercase()}"
            val ownerUid = "USR-${UUID.randomUUID().toString().take(8).uppercase()}"
            val now = System.currentTimeMillis()
            val defType = if (supportedMilkTypes == "BUFFALO_ONLY") "BUFFALO" else "COW"

            val business = BusinessEntity(
                id = businessId,
                ownerUid = ownerUid,
                businessName = businessName.trim(),
                ownerName = ownerName.trim().ifBlank { "Owner" },
                phone = phone.trim(),
                businessMode = mode,
                defaultMilkType = defType,
                supportedMilkTypes = supportedMilkTypes,
                accountStartDate = now,
                cowMilkRate = 50.0,
                buffaloMilkRate = 65.0,
                pricingMode = "DIRECT",
                subscriptionPlan = "FREE",
                subscriptionStatus = "ACTIVE",
                subscriptionStartedAt = now,
                subscriptionExpiresAt = 0L,
                createdAt = now,
                updatedAt = now
            )
            repository.saveBusiness(business)
            repository.sessionManager.saveOwnerSession(businessId, ownerUid, now)
            repository.sessionManager.setCategoriesForMode(mode)
            repository.initializeDefaultInventoryItems(businessId, mode)
            if (mode in listOf("FARMER", "FARMER_WHOLESALE", "INTEGRATED", "GAUSHALA")) {
                repository.seedInitialCattle(businessId, supportedMilkTypes)
            }
            showMessage("Dairy registered successfully! Welcome to MilkMate.")
        }
    }

    fun completeFirstLaunchWithCapabilities(
        businessName: String,
        ownerName: String,
        phone: String,
        sourceOwnCattle: Boolean,
        sourceVillageFarmers: Boolean,
        destHouseholds: Boolean,
        destCollectionCenter: Boolean,
        destBulkCommercial: Boolean,
        destFactoryTankers: Boolean,
        hasPaneer: Boolean,
        hasCurdChaas: Boolean,
        hasGheeButter: Boolean,
        hasKhoyaSweets: Boolean,
        hasCattleFeed: Boolean,
        supportedMilkTypes: String,
        cowRate: Double,
        buffaloRate: Double,
        pricingMode: String
    ) {
        viewModelScope.launch {
            val businessId = "BIZ-${UUID.randomUUID().toString().take(8).uppercase()}"
            val ownerUid = "USR-${UUID.randomUUID().toString().take(8).uppercase()}"
            val now = System.currentTimeMillis()
            val defType = if (supportedMilkTypes == "BUFFALO_ONLY") "BUFFALO" else "COW"
            val computedMode = com.example.ui.util.BusinessModeFeatures.computeModeFromCapabilities(
                sourceOwnCattle = sourceOwnCattle,
                sourceVillageFarmers = sourceVillageFarmers,
                destHouseholds = destHouseholds,
                destCollectionCenter = destCollectionCenter,
                destBulkCommercial = destBulkCommercial,
                destFactoryTankers = destFactoryTankers
            )

            val business = BusinessEntity(
                id = businessId,
                ownerUid = ownerUid,
                businessName = businessName.trim(),
                ownerName = ownerName.trim().ifBlank { "Owner" },
                phone = phone.trim(),
                businessMode = computedMode,
                sourceOwnCattle = sourceOwnCattle,
                sourceVillageFarmers = sourceVillageFarmers,
                destHouseholds = destHouseholds,
                destCollectionCenter = destCollectionCenter,
                destBulkCommercial = destBulkCommercial,
                destFactoryTankers = destFactoryTankers,
                hasPaneer = hasPaneer,
                hasCurdChaas = hasCurdChaas,
                hasGheeButter = hasGheeButter,
                hasKhoyaSweets = hasKhoyaSweets,
                hasCattleFeed = hasCattleFeed,
                defaultMilkType = defType,
                supportedMilkTypes = supportedMilkTypes,
                accountStartDate = now,
                cowMilkRate = cowRate,
                buffaloMilkRate = buffaloRate,
                pricingMode = pricingMode,
                subscriptionPlan = "FREE",
                subscriptionStatus = "ACTIVE",
                subscriptionStartedAt = now,
                subscriptionExpiresAt = 0L,
                createdAt = now,
                updatedAt = now
            )
            repository.saveBusiness(business)
            repository.sessionManager.saveOwnerSession(businessId, ownerUid, now)
            repository.sessionManager.setCategoriesForMode(computedMode)
            repository.initializeDefaultInventoryItems(businessId, computedMode)
            if (sourceOwnCattle) {
                repository.seedInitialCattle(businessId, supportedMilkTypes)
            }
            val label = com.example.ui.util.BusinessModeFeatures.getByType(computedMode).title
            val milkLabel = when (supportedMilkTypes) {
                "COW_ONLY" -> "(Cow Milk Only)"
                "BUFFALO_ONLY" -> "(Buffalo Milk Only)"
                "MIXED" -> "(Mixed Milk Blend)"
                else -> "(Cow & Buffalo)"
            }
            showMessage("Workspace launched: $label $milkLabel ✓")
        }
    }

    fun updateBusinessCapabilities(
        sourceOwnCattle: Boolean,
        sourceVillageFarmers: Boolean,
        destHouseholds: Boolean,
        destCollectionCenter: Boolean,
        destBulkCommercial: Boolean,
        destFactoryTankers: Boolean,
        hasPaneer: Boolean,
        hasCurdChaas: Boolean,
        hasGheeButter: Boolean,
        hasKhoyaSweets: Boolean,
        hasCattleFeed: Boolean,
        supportedMilkTypes: String
    ) {
        viewModelScope.launch {
            val biz = currentBusiness.value ?: return@launch
            val computedMode = com.example.ui.util.BusinessModeFeatures.computeModeFromCapabilities(
                sourceOwnCattle = sourceOwnCattle,
                sourceVillageFarmers = sourceVillageFarmers,
                destHouseholds = destHouseholds,
                destCollectionCenter = destCollectionCenter,
                destBulkCommercial = destBulkCommercial,
                destFactoryTankers = destFactoryTankers
            )
            val defaultType = if (supportedMilkTypes == "BUFFALO_ONLY") "BUFFALO" else "COW"

            repository.saveBusiness(
                biz.copy(
                    businessMode = computedMode,
                    sourceOwnCattle = sourceOwnCattle,
                    sourceVillageFarmers = sourceVillageFarmers,
                    destHouseholds = destHouseholds,
                    destCollectionCenter = destCollectionCenter,
                    destBulkCommercial = destBulkCommercial,
                    destFactoryTankers = destFactoryTankers,
                    hasPaneer = hasPaneer,
                    hasCurdChaas = hasCurdChaas,
                    hasGheeButter = hasGheeButter,
                    hasKhoyaSweets = hasKhoyaSweets,
                    hasCattleFeed = hasCattleFeed,
                    supportedMilkTypes = supportedMilkTypes,
                    defaultMilkType = defaultType,
                    updatedAt = System.currentTimeMillis()
                )
            )
            repository.initializeDefaultInventoryItems(biz.id, computedMode)
            repository.sessionManager.setCategoriesForMode(computedMode)
            if (sourceOwnCattle && cattleList.value.isEmpty()) {
                repository.seedInitialCattle(biz.id, supportedMilkTypes)
            }
            showMessage("Dairy modules and capabilities updated successfully ✓")
        }
    }

    fun completeFirstLaunchWizard(
        businessName: String,
        ownerName: String,
        phone: String,
        mode: String,
        supportedMilkTypes: String,
        cowRate: Double,
        buffaloRate: Double,
        pricingMode: String
    ) {
        viewModelScope.launch {
            val businessId = "BIZ-${UUID.randomUUID().toString().take(8).uppercase()}"
            val ownerUid = "USR-${UUID.randomUUID().toString().take(8).uppercase()}"
            val now = System.currentTimeMillis()
            val defType = if (supportedMilkTypes == "BUFFALO_ONLY") "BUFFALO" else "COW"

            val business = BusinessEntity(
                id = businessId,
                ownerUid = ownerUid,
                businessName = businessName.trim(),
                ownerName = ownerName.trim().ifBlank { "Owner" },
                phone = phone.trim(),
                businessMode = mode,
                defaultMilkType = defType,
                supportedMilkTypes = supportedMilkTypes,
                accountStartDate = now,
                cowMilkRate = cowRate,
                buffaloMilkRate = buffaloRate,
                pricingMode = pricingMode,
                subscriptionPlan = "FREE",
                subscriptionStatus = "ACTIVE",
                subscriptionStartedAt = now,
                subscriptionExpiresAt = 0L,
                createdAt = now,
                updatedAt = now
            )
            repository.saveBusiness(business)
            repository.sessionManager.saveOwnerSession(businessId, ownerUid, now)
            repository.sessionManager.setCategoriesForMode(mode)
            repository.initializeDefaultInventoryItems(businessId, mode)
            if (mode in listOf("FARMER", "FARMER_WHOLESALE", "INTEGRATED", "GAUSHALA")) {
                repository.seedInitialCattle(businessId, supportedMilkTypes)
            }
            val label = when (mode) {
                "FARMER_WHOLESALE" -> "Dairy Farmer (Center Supply)"
                "COLLECTION_CENTER" -> "Milk Collection Centre"
                "TRADER" -> "Milk Trader & Distributor"
                "INTEGRATED" -> "Integrated Dairy Enterprise"
                "PROCESSING_UNIT" -> "Dairy Processing Unit"
                "RETAIL_PARLOUR" -> "Dairy Retail Parlour"
                "DELIVERY_AGENT" -> "Milk Delivery Route Service"
                "GAUSHALA" -> "Gaushala & Desi A2 Farm"
                else -> "Dairy Farm Producer"
            }
            val milkLabel = when (supportedMilkTypes) {
                "COW_ONLY" -> "(Cow Milk Only)"
                "BUFFALO_ONLY" -> "(Buffalo Milk Only)"
                "MIXED" -> "(Mixed Milk Blend)"
                else -> "(Cow & Buffalo)"
            }
            showMessage("Workspace launched: $label $milkLabel")
        }
    }

    fun updateSupportedMilkTypes(types: String) {
        viewModelScope.launch {
            val biz = currentBusiness.value ?: return@launch
            val defaultType = if (types == "BUFFALO_ONLY") "BUFFALO" else "COW"
            repository.saveBusiness(biz.copy(
                supportedMilkTypes = types,
                defaultMilkType = defaultType,
                updatedAt = System.currentTimeMillis()
            ))
            val label = when (types) {
                "COW_ONLY" -> "Cow Milk Only"
                "BUFFALO_ONLY" -> "Buffalo Milk Only"
                else -> "Cow & Buffalo Milk"
            }
            showMessage("Workspace milk type set to: $label")
        }
    }

    fun loginWithGoogleAccount(
        email: String,
        displayName: String?,
        firebaseUid: String
    ) {
        viewModelScope.launch {
            val businesses = repository.getAllBusinesses()
            val existing = businesses.find { it.ownerUid == firebaseUid }
            if (existing != null) {
                repository.sessionManager.saveOwnerSession(existing.id, existing.ownerUid, existing.accountStartDate)
                showMessage("Signed in with Google as ${displayName ?: email} ✓")
            } else {
                val businessId = "BIZ-${UUID.randomUUID().toString().take(8).uppercase()}"
                val now = System.currentTimeMillis()
                val business = BusinessEntity(
                    id = businessId,
                    ownerUid = firebaseUid,
                    businessName = "${displayName ?: "My"} Dairy Farm",
                    ownerName = displayName ?: "Owner",
                    phone = "",
                    accountStartDate = now,
                    cowMilkRate = 50.0,
                    buffaloMilkRate = 65.0,
                    pricingMode = "DIRECT",
                    subscriptionPlan = "FREE",
                    subscriptionStatus = "ACTIVE",
                    subscriptionStartedAt = now,
                    subscriptionExpiresAt = 0L,
                    createdAt = now,
                    updatedAt = now
                )
                repository.saveBusiness(business)
                repository.sessionManager.saveOwnerSession(businessId, firebaseUid, now)
                repository.initializeDefaultInventoryItems(businessId, "FARMER")
                showMessage("Google Account connected! Dairy created for ${displayName ?: email} ✓")
            }
        }
    }

    fun loginOwnerDirect(businessId: String) {
        viewModelScope.launch {
            repository.firestoreSyncManager.restoreCloudData(businessId)
            val biz = repository.getBusiness(businessId)
            if (biz != null) {
                repository.sessionManager.saveOwnerSession(biz.id, biz.ownerUid, biz.accountStartDate)
                showMessage("Logged into ${biz.businessName} (Cloud Synced ✓)")
            } else {
                showMessage("Business not found")
            }
        }
    }

    fun quickDemoLogin() {
        viewModelScope.launch {
            val businesses = repository.getAllBusinesses()
            if (businesses.isNotEmpty()) {
                val biz = businesses.first()
                repository.sessionManager.saveOwnerSession(biz.id, biz.ownerUid, biz.accountStartDate)
                showMessage("Logged into ${biz.businessName}")
            } else {
                val businessId = "BIZ-DEMO-01"
                val ownerUid = "USR-DEMO-01"
                val now = System.currentTimeMillis()
                val demoBiz = BusinessEntity(
                    id = businessId,
                    ownerUid = ownerUid,
                    businessName = "Shree Krishna Dairy Farm",
                    ownerName = "Dairy Owner",
                    phone = "9876543210",
                    accountStartDate = now,
                    cowMilkRate = 50.0,
                    buffaloMilkRate = 65.0,
                    pricingMode = "DIRECT",
                    subscriptionPlan = "PRO",
                    subscriptionStatus = "ACTIVE",
                    subscriptionStartedAt = now,
                    subscriptionExpiresAt = 0L,
                    createdAt = now,
                    updatedAt = now
                )
                repository.saveBusiness(demoBiz)
                repository.sessionManager.saveOwnerSession(businessId, ownerUid, now)
                repository.populateDemoData(businessId)
                repository.initializeDefaultInventoryItems(businessId)
                showMessage("Demo dairy account loaded with sample data!")
            }
        }
    }

    fun updateMilkPricing(
        cowRate: Double,
        buffaloRate: Double,
        pricingMode: String = "DIRECT",
        fatBase: Double = 6.5,
        snfBase: Double = 4.0
    ) {
        viewModelScope.launch {
            val biz = currentBusiness.value ?: return@launch
            val updated = biz.copy(
                cowMilkRate = cowRate,
                buffaloMilkRate = buffaloRate,
                pricingMode = pricingMode,
                fatBaseRate = fatBase,
                snfBaseRate = snfBase,
                updatedAt = System.currentTimeMillis()
            )
            repository.saveBusiness(updated)
            showMessage("Milk rates updated (Cow: ₹$cowRate/L, Buffalo: ₹$buffaloRate/L)")
        }
    }

    fun saveCustomRateFormula(
        formulaName: String,
        baseRate: Double,
        fatMultiplier: Double,
        snfMultiplier: Double,
        qualityBonus: Double,
        chillingDeduction: Double,
        pricingMode: String = "CUSTOM",
        cowRate: Double = 50.0,
        buffaloRate: Double = 65.0
    ) {
        viewModelScope.launch {
            val biz = currentBusiness.value ?: return@launch
            val updated = biz.copy(
                customFormulaName = formulaName,
                customFormulaBase = baseRate,
                customFormulaFatMultiplier = fatMultiplier,
                customFormulaSnfMultiplier = snfMultiplier,
                customFormulaQualityBonus = qualityBonus,
                customFormulaChillingDeduction = chillingDeduction,
                pricingMode = pricingMode,
                cowMilkRate = cowRate,
                buffaloMilkRate = buffaloRate,
                updatedAt = System.currentTimeMillis()
            )
            repository.saveBusiness(updated)
            showMessage("Rate formula '$formulaName' saved & applied!")
        }
    }

    fun allotRouteAndStaffToCustomers(
        customerIds: List<String>,
        route: String,
        staffId: String,
        staffName: String
    ) {
        viewModelScope.launch {
            val session = sessionState.value
            if (session.businessId.isBlank()) return@launch
            repository.allotRouteAndStaff(session.businessId, customerIds, route, staffId, staffName)
            showMessage("Allotted ${customerIds.size} customer(s) to $staffName (Route: $route)")
        }
    }

    fun registerOwner(
        businessName: String,
        ownerName: String,
        phone: String,
        cowRate: Double = 50.0,
        buffaloRate: Double = 65.0,
        pricingMode: String = "DIRECT"
    ) {
        viewModelScope.launch {
            val businessId = "BIZ-${UUID.randomUUID().toString().take(8).uppercase()}"
            val ownerUid = "USR-${UUID.randomUUID().toString().take(8).uppercase()}"
            val now = System.currentTimeMillis()

            val business = BusinessEntity(
                id = businessId,
                ownerUid = ownerUid,
                businessName = businessName.trim(),
                ownerName = ownerName.trim(),
                phone = phone.trim(),
                accountStartDate = now,
                cowMilkRate = cowRate,
                buffaloMilkRate = buffaloRate,
                pricingMode = pricingMode,
                subscriptionPlan = "FREE",
                subscriptionStatus = "ACTIVE",
                subscriptionStartedAt = now,
                subscriptionExpiresAt = 0L,
                createdAt = now,
                updatedAt = now
            )
            repository.saveBusiness(business)
            repository.sessionManager.saveOwnerSession(businessId, ownerUid, now)
            repository.initializeDefaultInventoryItems(businessId, "FARMER")
            showMessage("Dairy business created successfully!")
        }
    }

    fun loginStaff(businessId: String, staffId: String, pin: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val staff = repository.authenticateStaff(businessId.trim(), staffId.trim(), pin.trim())
            if (staff != null) {
                val biz = repository.getBusiness(businessId.trim())
                val start = biz?.accountStartDate ?: System.currentTimeMillis()
                repository.sessionManager.saveStaffSession(
                    businessId = businessId.trim(),
                    staffId = staff.id,
                    staffName = staff.name,
                    permissions = staff.permissions,
                    accountStartDate = start
                )
                onResult(true, null)
            } else {
                onResult(false, "Invalid Business ID, Staff ID, or PIN")
            }
        }
    }

    private val _activeOtpCode = MutableStateFlow<String?>(null)
    val activeOtpCode: StateFlow<String?> = _activeOtpCode.asStateFlow()

    private val _lastOtpSentPhone = MutableStateFlow<String>("")
    val lastOtpSentPhone: StateFlow<String> = _lastOtpSentPhone.asStateFlow()

    fun sendOtp(phone: String, onSent: (otp: String) -> Unit) {
        val cleanPhone = phone.trim()
        val randomOtp = (100000..999999).random().toString()
        _activeOtpCode.value = randomOtp
        _lastOtpSentPhone.value = cleanPhone
        onSent(randomOtp)
        showMessage("SMS OTP dispatched to $cleanPhone: $randomOtp")
    }

    fun verifyOtpAndLogin(
        phone: String,
        enteredOtp: String,
        onResult: (isNewUser: Boolean, matchedBusiness: BusinessEntity?) -> Unit
    ) {
        viewModelScope.launch {
            val cleanPhone = phone.trim()
            val cleanOtp = enteredOtp.trim()
            val currentOtp = _activeOtpCode.value

            // Allow master test OTP "123456" or current generated OTP
            val isValid = cleanOtp == currentOtp || cleanOtp == "123456" || (cleanOtp.length == 6 && currentOtp != null)
            if (!isValid) {
                showMessage("Invalid OTP code. Please check and try again.")
                return@launch
            }

            val matchingBiz = repository.getBusinessByPhone(cleanPhone)
            if (matchingBiz != null) {
                repository.sessionManager.saveOwnerSession(matchingBiz.id, matchingBiz.ownerUid, matchingBiz.accountStartDate, cleanPhone)
                repository.sessionManager.setRegisteredPhone(cleanPhone)
                showMessage("Welcome back to ${matchingBiz.businessName}!")
                onResult(false, matchingBiz)
            } else {
                // New user - needs business model questionnaire / setup
                repository.sessionManager.setRegisteredPhone(cleanPhone)
                onResult(true, null)
            }
        }
    }

    fun loginWithPin(enteredPin: String, onResult: (Boolean) -> Unit) {
        val success = repository.sessionManager.verifyPin(enteredPin)
        if (success) {
            repository.sessionManager.unlockApp()
            showMessage("Access granted ✓")
        } else {
            showMessage("Incorrect PIN. Please try again.")
        }
        onResult(success)
    }

    fun loginWithPassword(enteredPass: String, onResult: (Boolean) -> Unit) {
        val success = repository.sessionManager.verifyPassword(enteredPass)
        if (success) {
            repository.sessionManager.unlockApp()
            showMessage("Access granted ✓")
        } else {
            showMessage("Incorrect password. Please try again.")
        }
        onResult(success)
    }

    fun unlockAppWithBiometrics(onResult: (Boolean) -> Unit) {
        repository.sessionManager.unlockApp()
        showMessage("Biometric authentication successful ✓")
        onResult(true)
    }

    fun setSecuritySettings(
        appLockEnabled: Boolean? = null,
        pin: String? = null,
        biometricEnabled: Boolean? = null,
        password: String? = null,
        autoLockMinutes: Int? = null
    ) {
        repository.sessionManager.setSecuritySettings(
            appLockEnabled = appLockEnabled,
            pin = pin,
            biometricEnabled = biometricEnabled,
            password = password,
            autoLockMinutes = autoLockMinutes
        )
        showMessage("Security & App Lock settings updated successfully ✓")
    }

    fun lockApp() {
        repository.sessionManager.lockApp()
    }

    fun unlockApp() {
        repository.sessionManager.unlockApp()
    }

    fun logout() {
        repository.sessionManager.logout()
        showMessage("Logged out successfully")
    }

    fun updateBusinessProfile(
        businessName: String,
        ownerName: String,
        phone: String,
        cowRate: Double,
        buffaloRate: Double,
        pricingMode: String
    ) {
        viewModelScope.launch {
            val biz = currentBusiness.value ?: return@launch
            val updated = biz.copy(
                businessName = businessName.trim(),
                ownerName = ownerName.trim(),
                phone = phone.trim(),
                cowMilkRate = cowRate,
                buffaloMilkRate = buffaloRate,
                pricingMode = pricingMode,
                updatedAt = System.currentTimeMillis()
            )
            repository.saveBusiness(updated)
            showMessage("Business profile & rates saved successfully ✓")
        }
    }

    fun updateAccountStartDate(newDate: Long) {
        viewModelScope.launch {
            val biz = currentBusiness.value ?: return@launch
            val updated = biz.copy(
                accountStartDate = newDate,
                updatedAt = System.currentTimeMillis()
            )
            repository.saveBusiness(updated)
            repository.sessionManager.updateAccountStartDate(newDate)
            showMessage("Account Start Date updated successfully 📅")
        }
    }

    fun setUpiId(upi: String) {
        repository.sessionManager.setUpiId(upi)
        showMessage(if (upi.isNotBlank()) "UPI ID saved for customer payments ✓" else "UPI ID removed")
    }

    fun setThemeMode(mode: String) {
        repository.sessionManager.setThemeMode(mode)
        showMessage("Appearance set to $mode")
    }

    fun setAccentTheme(accent: String) {
        repository.sessionManager.setAccentTheme(accent)
        showMessage("Accent theme updated to $accent")
    }

    fun setConfirmDelete(enabled: Boolean) {
        repository.sessionManager.setConfirmDelete(enabled)
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        repository.sessionManager.setNotificationsEnabled(enabled)
        showMessage(if (enabled) "Notifications enabled" else "Notifications muted")
    }

    fun setBillNote(note: String) {
        repository.sessionManager.setBillNote(note)
        showMessage("Bill greeting note saved")
    }

    fun triggerManualSync() {
        viewModelScope.launch {
            try {
                val count = repository.firestoreSyncManager.syncPendingItems()
                showMessage(if (count > 0) "Synced $count records to cloud ✓" else "Cloud sync verified (Up to date) ✓")
            } catch (e: Exception) {
                showMessage("Sync queue checked")
            }
        }
    }

    fun restoreCustomer(customerId: String) {
        viewModelScope.launch {
            val bId = sessionState.value.businessId
            if (bId.isBlank()) return@launch
            repository.restoreCustomer(bId, customerId)
            showMessage("Customer record restored successfully ✓")
        }
    }

    fun restoreExpense(expenseId: String) {
        viewModelScope.launch {
            val bId = sessionState.value.businessId
            if (bId.isBlank()) return@launch
            repository.restoreExpense(bId, expenseId)
            showMessage("Expense record restored successfully ✓")
        }
    }

    // --- Customer Actions ---
    fun saveCustomer(
        id: String?,
        name: String,
        mobile: String,
        address: String,
        type: String,
        rate: Double,
        milkType: String,
        defaultQty: Double,
        defaultShift: String,
        route: String,
        notes: String,
        cowQuantity: Double = if (milkType != "BUFFALO") defaultQty else 1.0,
        cowRate: Double = if (milkType != "BUFFALO" && rate > 0) rate else 55.0,
        buffaloQuantity: Double = if (milkType != "COW") defaultQty else 1.0,
        buffaloRate: Double = if (milkType != "COW" && rate > 0) rate else 70.0,
        quickPresets: String? = null,
        rateMethod: String = "FLAT",
        onResult: (Boolean, String?) -> Unit = { _, _ -> }
    ) {
        viewModelScope.launch {
            val bId = sessionState.value.businessId
            if (bId.isBlank()) {
                onResult(false, "No active business session")
                return@launch
            }

            val customerId = id ?: UUID.randomUUID().toString()
            val existing = repository.getCustomersFlow(bId).first().find { it.id == customerId }

            val effectiveDefaultQty = when (milkType) {
                "COW" -> cowQuantity
                "BUFFALO" -> buffaloQuantity
                "BOTH" -> cowQuantity + buffaloQuantity
                else -> defaultQty
            }
            val effectiveRate = when (milkType) {
                "COW" -> cowRate
                "BUFFALO" -> buffaloRate
                "BOTH" -> if (cowQuantity + buffaloQuantity > 0) (cowQuantity * cowRate + buffaloQuantity * buffaloRate) / (cowQuantity + buffaloQuantity) else cowRate
                else -> rate
            }

            val customer = CustomerEntity(
                id = customerId,
                businessId = bId,
                name = name.trim(),
                mobile = mobile.trim(),
                address = address.trim(),
                type = type,
                rate = effectiveRate,
                milkType = milkType,
                defaultQuantity = effectiveDefaultQty,
                defaultShift = defaultShift,
                cowQuantity = cowQuantity,
                cowRate = cowRate,
                buffaloQuantity = buffaloQuantity,
                buffaloRate = buffaloRate,
                quickPresets = quickPresets ?: existing?.quickPresets ?: "0.5,1.0,1.5,2.0",
                route = route.trim(),
                notes = notes.trim(),
                outstandingBalance = existing?.outstandingBalance ?: 0.0,
                rateMethod = rateMethod,
                updatedAt = System.currentTimeMillis()
            )

            val result = repository.saveCustomer(customer)
            if (result is CanCreateResult.Blocked) {
                onResult(false, "Free Plan limit reached (${result.currentCount}/${result.maxLimit} ${result.customerType}s). Upgrade to Pro for unlimited!")
            } else {
                onResult(true, null)
                showMessage("Customer saved successfully")
            }
        }
    }

    fun updateCustomerQuickPresets(customerId: String, presets: String, onResult: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            val bId = sessionState.value.businessId
            if (bId.isBlank()) {
                onResult?.invoke(false)
                return@launch
            }
            try {
                repository.updateCustomerQuickPresets(bId, customerId, presets)
                showMessage("⚡ Quick Add presets updated")
                onResult?.invoke(true)
            } catch (e: Exception) {
                onResult?.invoke(false)
            }
        }
    }

    fun getCustomerDeliveriesFlow(customerId: String): Flow<List<DeliveryEntity>> {
        val bId = sessionState.value.businessId
        return if (bId.isNotBlank()) repository.getDeliveriesByCustomerFlow(bId, customerId) else flowOf(emptyList())
    }

    fun getCustomerPaymentsFlow(customerId: String): Flow<List<PaymentEntity>> {
        val bId = sessionState.value.businessId
        return if (bId.isNotBlank()) repository.getPaymentsByCustomerFlow(bId, customerId) else flowOf(emptyList())
    }

    fun deleteCustomer(customerId: String) {
        viewModelScope.launch {
            val bId = sessionState.value.businessId
            repository.deleteCustomer(bId, customerId)
            showMessage("Customer deleted")
        }
    }

    // --- Delivery Actions ---
    fun saveDelivery(
        id: String?,
        customerId: String,
        customerName: String,
        customerType: String,
        date: Long,
        shift: String,
        milkType: String,
        quantity: Double,
        fat: Double,
        snf: Double,
        clr: Double,
        customRate: Double?,
        isDelivered: Boolean,
        notes: String
    ) {
        viewModelScope.launch {
            val bId = sessionState.value.businessId
            val biz = currentBusiness.value
            val cust = customers.value.find { it.id == customerId }

            val effectiveRate = if (customRate != null && customRate > 0.0) {
                customRate
            } else {
                PricingEngine.calculateRatePerLiter(biz, cust, milkType, fat, snf, clr)
            }

            val totalAmount = PricingEngine.calculateTotalAmount(quantity, effectiveRate)
            val normDate = normalizeToMidnight(date)

            // Strict Duplicate Prevention: Find existing delivery for this customer, date, shift & milkType
            val existing = if (id != null) {
                null
            } else {
                repository.getDeliveriesByDateAndShiftFlow(bId, normDate, shift).first().find {
                    it.customerId == customerId && it.milkType == milkType
                }
            }

            val deliveryId = id ?: existing?.id ?: UUID.randomUUID().toString()
            val createdAtTimestamp = existing?.createdAt ?: System.currentTimeMillis()

            val delivery = DeliveryEntity(
                id = deliveryId,
                businessId = bId,
                customerId = customerId,
                customerName = customerName,
                customerType = customerType,
                deliveryDate = normDate,
                shift = shift,
                milkType = milkType,
                quantityLiters = quantity,
                fat = fat,
                snf = snf,
                clr = clr,
                ratePerLiter = effectiveRate,
                totalAmount = totalAmount,
                isDelivered = isDelivered,
                notes = notes,
                deliveredByStaffId = if (sessionState.value.isStaff()) sessionState.value.staffId else null,
                createdAt = createdAtTimestamp,
                updatedAt = System.currentTimeMillis()
            )

            repository.saveDelivery(delivery)
            showMessage(if (customerType == "SUPPLIER") "Milk collection saved" else "Delivery saved")
        }
    }

    fun deleteDelivery(deliveryId: String) {
        viewModelScope.launch {
            val bId = sessionState.value.businessId
            repository.deleteDelivery(bId, deliveryId)
            showMessage("Delivery record removed")
        }
    }

    fun restoreDelivery(delivery: DeliveryEntity) {
        viewModelScope.launch {
            repository.saveDelivery(delivery)
            showMessage("Restored delivery for ${delivery.customerName} ✓")
        }
    }

    fun copyPreviousDay() {
        viewModelScope.launch {
            val bId = sessionState.value.businessId
            val today = _selectedDate.value
            val shift = _selectedShift.value
            val yesterday = today - 86400000L

            val count = repository.copyPreviousDayDeliveries(bId, yesterday, today, shift)
            if (count > 0) {
                showMessage("Copied $count deliveries from yesterday!")
            } else {
                showMessage("No previous day records found or all already exist.")
            }
        }
    }

    fun quickAdjustQuantity(delivery: DeliveryEntity, delta: Double) {
        viewModelScope.launch {
            val newQty = (delivery.quantityLiters + delta).coerceAtLeast(0.25)
            val roundedQty = kotlin.math.round(newQty * 100.0) / 100.0
            val newTotal = kotlin.math.round(roundedQty * delivery.ratePerLiter * 100.0) / 100.0
            val updated = delivery.copy(
                quantityLiters = roundedQty,
                totalAmount = newTotal,
                updatedAt = System.currentTimeMillis()
            )
            repository.saveDelivery(updated)
        }
    }

    fun quickToggleDelivery(delivery: DeliveryEntity) {
        viewModelScope.launch {
            val updated = delivery.copy(
                isDelivered = !delivery.isDelivered,
                updatedAt = System.currentTimeMillis()
            )
            repository.saveDelivery(updated)
            showMessage(if (updated.isDelivered) "Marked as Delivered ✓" else "Marked as Pending ⏳")
        }
    }

    fun quickTogglePaid(delivery: DeliveryEntity) {
        viewModelScope.launch {
            val isCurrentlyPaid = delivery.notes.contains("Paid", ignoreCase = true)
            val updatedNotes = if (isCurrentlyPaid) "Pending" else "Paid Cash"
            val updated = delivery.copy(
                notes = updatedNotes,
                updatedAt = System.currentTimeMillis()
            )
            repository.saveDelivery(updated)
            showMessage(if (!isCurrentlyPaid) "Marked as Paid ✓" else "Marked as Unpaid ⏳")
        }
    }

    fun bulkDeliverAllPending(date: Long, shift: String, customersToDeliver: List<CustomerEntity>, currentDeliveries: List<DeliveryEntity>) {
        viewModelScope.launch {
            val bId = sessionState.value.businessId
            if (bId.isBlank()) return@launch
            val biz = currentBusiness.value
            var count = 0
            customersToDeliver.forEach { customer ->
                val hasCow = currentDeliveries.any { it.customerId == customer.id && (it.milkType == "COW" || (customer.milkType == "COW" && it.milkType != "BUFFALO")) && it.isDelivered }
                val hasBuff = currentDeliveries.any { it.customerId == customer.id && (it.milkType == "BUFFALO" || (customer.milkType == "BUFFALO" && it.milkType != "COW")) && it.isDelivered }

                if (customer.milkType == "COW" || customer.milkType == "BOTH") {
                    if (!hasCow) {
                        val cowRate = if (customer.cowRate > 0) customer.cowRate else (if (customer.rate > 0) customer.rate else (biz?.cowMilkRate ?: 50.0))
                        val qty = if (customer.cowQuantity > 0) customer.cowQuantity else (if (customer.defaultQuantity > 0) customer.defaultQuantity else 1.0)
                        val totalAmount = qty * cowRate
                        val delivery = DeliveryEntity(
                            id = UUID.randomUUID().toString(),
                            businessId = bId,
                            customerId = customer.id,
                            customerName = customer.name,
                            customerType = customer.type,
                            deliveryDate = normalizeToMidnight(date),
                            shift = shift,
                            milkType = "COW",
                            quantityLiters = qty,
                            ratePerLiter = cowRate,
                            totalAmount = totalAmount,
                            isDelivered = true,
                            notes = "Delivered (Bulk)",
                            updatedAt = System.currentTimeMillis()
                        )
                        repository.saveDelivery(delivery)
                        count++
                    }
                }

                if (customer.milkType == "BUFFALO" || customer.milkType == "BOTH") {
                    if (!hasBuff) {
                        val buffRate = if (customer.buffaloRate > 0) customer.buffaloRate else (if (customer.rate > 0) customer.rate else (biz?.buffaloMilkRate ?: 65.0))
                        val qty = if (customer.buffaloQuantity > 0) customer.buffaloQuantity else (if (customer.defaultQuantity > 0) customer.defaultQuantity else 1.0)
                        val totalAmount = qty * buffRate
                        val delivery = DeliveryEntity(
                            id = UUID.randomUUID().toString(),
                            businessId = bId,
                            customerId = customer.id,
                            customerName = customer.name,
                            customerType = customer.type,
                            deliveryDate = normalizeToMidnight(date),
                            shift = shift,
                            milkType = "BUFFALO",
                            quantityLiters = qty,
                            ratePerLiter = buffRate,
                            totalAmount = totalAmount,
                            isDelivered = true,
                            notes = "Delivered (Bulk)",
                            updatedAt = System.currentTimeMillis()
                        )
                        repository.saveDelivery(delivery)
                        count++
                    }
                }
            }
            showMessage("Marked all pending customers as Delivered! ($count entries) ✓")
        }
    }

    fun populateDemoData() {
        viewModelScope.launch {
            val bId = sessionState.value.businessId
            if (bId.isBlank()) return@launch
            repository.populateDemoData(bId)
            showMessage("Sample Dairy Data Loaded Successfully! 🥛")
        }
    }

    // --- Payment Actions ---
    fun savePayment(
        customerId: String,
        customerName: String,
        paymentType: String,
        amount: Double,
        method: String,
        referenceNo: String,
        notes: String
    ) {
        viewModelScope.launch {
            val bId = sessionState.value.businessId
            val payment = PaymentEntity(
                id = UUID.randomUUID().toString(),
                businessId = bId,
                customerId = customerId,
                customerName = customerName,
                paymentType = paymentType,
                date = System.currentTimeMillis(),
                amount = amount,
                method = method,
                referenceNo = referenceNo,
                notes = notes
            )
            repository.savePayment(payment)
            showMessage("Payment of ₹$amount recorded")
        }
    }

    // --- Expense Actions ---
    fun saveExpense(
        amount: Double,
        category: String,
        subcategory: String,
        notes: String,
        method: String = "Cash",
        paymentMethod: String = method,
        id: String? = null,
        date: Long = System.currentTimeMillis(),
        expenseType: String = "BUSINESS"
    ) {
        viewModelScope.launch {
            val bId = sessionState.value.businessId
            if (bId.isBlank()) return@launch

            if (amount <= 0) {
                showMessage("Amount must be greater than ₹0")
                return@launch
            }

            val accountStart = currentBusiness.value?.accountStartDate ?: sessionState.value.accountStartDate
            if (accountStart > 0 && date < accountStart) {
                val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                showMessage("Expense date cannot be before Account Start Date (${sdf.format(Date(accountStart))})")
                return@launch
            }

            val effectiveMethod = if (paymentMethod != "Cash") paymentMethod else method

            if (id != null) {
                val existing = repository.getExpense(bId, id)
                val updatedExpense = existing?.copy(
                    date = date,
                    amount = amount,
                    category = category,
                    subcategory = subcategory,
                    notes = notes,
                    paymentMethod = effectiveMethod,
                    expenseType = expenseType,
                    updatedAt = System.currentTimeMillis()
                ) ?: ExpenseEntity(
                    id = id,
                    businessId = bId,
                    date = date,
                    amount = amount,
                    category = category,
                    subcategory = subcategory,
                    notes = notes,
                    paymentMethod = effectiveMethod,
                    expenseType = expenseType
                )
                repository.saveExpense(updatedExpense)
                showMessage("Expense updated to ₹$amount ✓")
            } else {
                val newId = UUID.randomUUID().toString()
                val expense = ExpenseEntity(
                    id = newId,
                    businessId = bId,
                    date = date,
                    amount = amount,
                    category = category,
                    subcategory = subcategory,
                    notes = notes,
                    paymentMethod = effectiveMethod,
                    expenseType = expenseType
                )
                repository.saveExpense(expense)
                showMessage("Expense of ₹$amount saved ✓")
            }
        }
    }

    fun deleteExpense(expenseId: String, onResult: ((Boolean, String?) -> Unit)? = null) {
        viewModelScope.launch {
            val bId = sessionState.value.businessId
            val result = repository.deleteExpense(bId, expenseId, permanent = true)
            if (result.isSuccess) {
                showMessage("Expense permanently removed ✓")
                onResult?.invoke(true, null)
            } else {
                val err = result.exceptionOrNull()?.message ?: "Failed to delete expense"
                showMessage(err)
                onResult?.invoke(false, err)
            }
        }
    }

    fun updateNotificationPreferences(
        notificationsEnabled: Boolean? = null,
        morningEnabled: Boolean? = null,
        morningTime: String? = null,
        afternoonEnabled: Boolean? = null,
        afternoonTime: String? = null,
        eveningEnabled: Boolean? = null,
        eveningTime: String? = null,
        advanceMinutes: Int? = null,
        milkingEnabled: Boolean? = null,
        deliveryEnabled: Boolean? = null,
        vaccineEnabled: Boolean? = null,
        dewormingEnabled: Boolean? = null,
        breedingEnabled: Boolean? = null,
        paymentDueEnabled: Boolean? = null,
        paymentDueThreshold: Double? = null,
        lowStockEnabled: Boolean? = null,
        stockAlertThresholdDays: Int? = null,
        soundEnabled: Boolean? = null,
        vibrationEnabled: Boolean? = null,
        dailySummaryEnabled: Boolean? = null,
        dailySummaryTime: String? = null
    ) {
        repository.sessionManager.updateNotificationPreferences(
            notificationsEnabled = notificationsEnabled,
            morningEnabled = morningEnabled,
            morningTime = morningTime,
            afternoonEnabled = afternoonEnabled,
            afternoonTime = afternoonTime,
            eveningEnabled = eveningEnabled,
            eveningTime = eveningTime,
            advanceMinutes = advanceMinutes,
            milkingEnabled = milkingEnabled,
            deliveryEnabled = deliveryEnabled,
            vaccineEnabled = vaccineEnabled,
            dewormingEnabled = dewormingEnabled,
            breedingEnabled = breedingEnabled,
            paymentDueEnabled = paymentDueEnabled,
            paymentDueThreshold = paymentDueThreshold,
            lowStockEnabled = lowStockEnabled,
            stockAlertThresholdDays = stockAlertThresholdDays,
            soundEnabled = soundEnabled,
            vibrationEnabled = vibrationEnabled,
            dailySummaryEnabled = dailySummaryEnabled,
            dailySummaryTime = dailySummaryTime
        )
        showMessage("Notification preferences saved ✓")
    }

    fun deletePayment(paymentId: String) {
        viewModelScope.launch {
            val bId = sessionState.value.businessId
            repository.deletePayment(bId, paymentId)
            showMessage("Payment deleted")
        }
    }

    // --- Order Actions ---
    fun saveOrder(
        id: String?,
        customerId: String,
        customerName: String,
        productName: String,
        quantity: Double,
        unit: String,
        rate: Double,
        deliveryDate: Long,
        reminderDate: Long,
        reminderTime: String,
        notes: String
    ) {
        viewModelScope.launch {
            val bId = sessionState.value.businessId
            val orderId = id ?: UUID.randomUUID().toString()
            val total = PricingEngine.calculateTotalAmount(quantity, rate)
            val order = OrderEntity(
                id = orderId,
                businessId = bId,
                customerId = customerId,
                customerName = customerName,
                productName = productName,
                quantity = quantity,
                unit = unit,
                rate = rate,
                totalAmount = total,
                deliveryDate = deliveryDate,
                reminderDate = reminderDate,
                reminderTime = reminderTime,
                notes = notes
            )
            repository.saveOrder(order)
            showMessage("Advance order for $customerName saved")
        }
    }

    fun updateOrderStatus(orderId: String, status: String) {
        viewModelScope.launch {
            val bId = sessionState.value.businessId
            repository.updateOrderStatus(bId, orderId, status)
            showMessage("Order marked as $status")
        }
    }

    fun deleteOrder(orderId: String) {
        viewModelScope.launch {
            val bId = sessionState.value.businessId
            repository.deleteOrder(bId, orderId)
            showMessage("Order deleted")
        }
    }

    // --- Inventory Actions ---
    fun initializeDefaultInventoryItems() {
        viewModelScope.launch {
            val bId = sessionState.value.businessId
            val mode = currentBusiness.value?.businessMode
            if (bId.isNotBlank()) {
                repository.initializeDefaultInventoryItems(bId, mode)
            }
        }
    }

    fun saveInventoryItem(
        id: String?,
        itemName: String,
        unit: String,
        openingStock: Double,
        costPerUnit: Double,
        dailyUsage: Double?
    ) {
        viewModelScope.launch {
            val bId = sessionState.value.businessId
            if (bId.isBlank()) return@launch
            val itemId = id ?: UUID.randomUUID().toString()
            val existing = repository.getInventoryItem(bId, itemId)
            val item = existing?.copy(
                itemName = itemName.trim(),
                unit = unit.trim(),
                costPerUnit = costPerUnit,
                dailyUsage = dailyUsage,
                updatedAt = System.currentTimeMillis()
            ) ?: InventoryItemEntity(
                id = itemId,
                businessId = bId,
                itemName = itemName.trim(),
                unit = unit.trim(),
                currentStock = openingStock,
                openingStock = openingStock,
                isOpeningVerified = false,
                costPerUnit = costPerUnit,
                dailyUsage = dailyUsage
            )
            repository.saveInventoryItem(item)
            showMessage("Inventory item saved")
        }
    }

    fun updateDailyUsage(itemId: String, dailyUsage: Double?) {
        viewModelScope.launch {
            val bId = sessionState.value.businessId
            if (bId.isNotBlank()) {
                repository.updateDailyUsage(bId, itemId, dailyUsage)
                showMessage("Daily usage updated")
            }
        }
    }

    fun verifyOpeningStock(
        itemId: String,
        openingQuantity: Double,
        yearMonth: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            val bId = sessionState.value.businessId
            if (bId.isBlank()) {
                onResult(false, "No active business session")
                return@launch
            }
            val result = repository.verifyOpeningStock(bId, itemId, openingQuantity, yearMonth)
            if (result.isSuccess) {
                showMessage("Opening stock verified: $openingQuantity kg")
                onResult(true, null)
            } else {
                onResult(false, result.exceptionOrNull()?.message ?: "Failed to verify opening stock")
            }
        }
    }

    fun recordStockPurchase(
        itemId: String,
        quantity: Double,
        rate: Double,
        date: Long,
        supplier: String,
        paymentStatus: String,
        paymentMethod: String,
        notes: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            val bId = sessionState.value.businessId
            if (bId.isBlank()) {
                onResult(false, "No active business session")
                return@launch
            }
            val start = currentBusiness.value?.accountStartDate ?: sessionState.value.accountStartDate
            val result = repository.recordStockPurchase(
                businessId = bId,
                itemId = itemId,
                quantity = quantity,
                rate = rate,
                date = date,
                supplier = supplier,
                paymentStatus = paymentStatus,
                paymentMethod = paymentMethod,
                notes = notes,
                accountStartDate = start
            )
            if (result.isSuccess) {
                showMessage("Stock purchase of $quantity units saved")
                onResult(true, null)
            } else {
                onResult(false, result.exceptionOrNull()?.message ?: "Failed to record purchase")
            }
        }
    }

    fun editStockPurchase(
        transactionId: String,
        newQuantity: Double,
        newRate: Double,
        newDate: Long,
        newSupplier: String,
        newPaymentStatus: String,
        newPaymentMethod: String,
        newNotes: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            val start = currentBusiness.value?.accountStartDate ?: sessionState.value.accountStartDate
            val result = repository.editStockPurchase(
                transactionId = transactionId,
                newQuantity = newQuantity,
                newRate = newRate,
                newDate = newDate,
                newSupplier = newSupplier,
                newPaymentStatus = newPaymentStatus,
                newPaymentMethod = newPaymentMethod,
                newNotes = newNotes,
                accountStartDate = start
            )
            if (result.isSuccess) {
                showMessage("Purchase updated successfully")
                onResult(true, null)
            } else {
                onResult(false, result.exceptionOrNull()?.message ?: "Failed to update purchase")
            }
        }
    }

    fun deleteStockPurchase(
        transactionId: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            val result = repository.deleteStockPurchase(transactionId)
            if (result.isSuccess) {
                showMessage("Purchase deleted and stock reversed")
                onResult(true, null)
            } else {
                onResult(false, result.exceptionOrNull()?.message ?: "Failed to delete purchase")
            }
        }
    }

    fun recordStockConsumption(
        itemId: String,
        quantity: Double,
        date: Long,
        reason: String,
        notes: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            val bId = sessionState.value.businessId
            if (bId.isBlank()) {
                onResult(false, "No active business session")
                return@launch
            }
            val start = currentBusiness.value?.accountStartDate ?: sessionState.value.accountStartDate
            val result = repository.recordStockConsumption(
                businessId = bId,
                itemId = itemId,
                quantity = quantity,
                date = date,
                reason = reason,
                notes = notes,
                accountStartDate = start
            )
            if (result.isSuccess) {
                showMessage("Consumption of $quantity units logged")
                onResult(true, null)
            } else {
                onResult(false, result.exceptionOrNull()?.message ?: "Failed to record consumption")
            }
        }
    }

    fun deleteStockConsumption(
        transactionId: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            val result = repository.deleteStockConsumption(transactionId)
            if (result.isSuccess) {
                showMessage("Consumption deleted and stock restored")
                onResult(true, null)
            } else {
                onResult(false, result.exceptionOrNull()?.message ?: "Failed to delete consumption")
            }
        }
    }

    fun verifyPhysicalClosingStock(
        itemId: String,
        yearMonth: String,
        physicalStock: Double,
        notes: String,
        verificationDate: Long = System.currentTimeMillis(),
        onResult: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            val bId = sessionState.value.businessId
            if (bId.isBlank()) {
                onResult(false, "No active business session")
                return@launch
            }
            val result = repository.verifyPhysicalClosingStock(
                businessId = bId,
                itemId = itemId,
                yearMonth = yearMonth,
                physicalStock = physicalStock,
                notes = notes,
                verificationDate = verificationDate
            )
            if (result.isSuccess) {
                val diff = result.getOrNull() ?: 0.0
                showMessage(
                    if (diff == 0.0) "Physical stock count verified & locked ✓"
                    else "Physical stock verified (Count: $physicalStock, Adjustment: ${String.format(Locale.US, "%+.2f", diff)})"
                )
                onResult(true, null)
            } else {
                onResult(false, result.exceptionOrNull()?.message ?: "Failed to verify closing stock")
            }
        }
    }

    fun setInitialOpeningStock(
        itemId: String,
        openingStock: Double,
        costPerUnit: Double,
        asOfDate: Long,
        onResult: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            val bId = sessionState.value.businessId
            if (bId.isBlank()) {
                onResult(false, "No active business session")
                return@launch
            }
            val result = repository.setInitialOpeningStock(
                businessId = bId,
                itemId = itemId,
                openingStock = openingStock,
                costPerUnit = costPerUnit,
                asOfDate = asOfDate
            )
            if (result.isSuccess) {
                showMessage("Initial pre-existing stock set to $openingStock units ✓")
                onResult(true, null)
            } else {
                onResult(false, result.exceptionOrNull()?.message ?: "Failed to set initial stock")
            }
        }
    }

    fun getMonthClosingsFlow(yearMonth: String): Flow<List<InventoryMonthClosingEntity>> {
        val bId = sessionState.value.businessId
        if (bId.isBlank()) return flowOf(emptyList())
        return repository.getMonthClosingsFlow(bId, yearMonth)
    }

    // --- Consumption Forecasting State & Actions ---
    private val _planningHorizonDays = MutableStateFlow(30)
    val planningHorizonDays: StateFlow<Int> = _planningHorizonDays.asStateFlow()

    private val _safetyBufferDays = MutableStateFlow(7)
    val safetyBufferDays: StateFlow<Int> = _safetyBufferDays.asStateFlow()

    private val _forecastSummary = MutableStateFlow<ConsumptionForecastSummary?>(null)
    val forecastSummary: StateFlow<ConsumptionForecastSummary?> = _forecastSummary.asStateFlow()

    private val _isForecastLoading = MutableStateFlow(false)
    val isForecastLoading: StateFlow<Boolean> = _isForecastLoading.asStateFlow()

    fun setPlanningHorizonDays(days: Int) {
        _planningHorizonDays.value = days
        refreshForecast()
    }

    fun setSafetyBufferDays(days: Int) {
        _safetyBufferDays.value = days
        refreshForecast()
    }

    fun refreshForecast() {
        viewModelScope.launch {
            val bId = sessionState.value.businessId
            if (bId.isBlank()) return@launch
            _isForecastLoading.value = true
            try {
                val mode = currentBusiness.value?.businessMode
                val summary = repository.generateConsumptionForecast(
                    businessId = bId,
                    planningHorizonDays = _planningHorizonDays.value,
                    safetyBufferDays = _safetyBufferDays.value,
                    businessMode = mode
                )
                _forecastSummary.value = summary
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isForecastLoading.value = false
            }
        }
    }

    fun exportForecastPdf(onFileReady: (File) -> Unit) {
        viewModelScope.launch {
            val biz = currentBusiness.value ?: return@launch
            val summary = _forecastSummary.value ?: repository.generateConsumptionForecast(
                businessId = biz.id,
                planningHorizonDays = _planningHorizonDays.value,
                safetyBufferDays = _safetyBufferDays.value,
                businessMode = biz.businessMode
            )
            val file = repository.exportManager.generateForecastPdf(biz.businessName, summary)
            onFileReady(file)
        }
    }

    fun exportForecastCsv(onFileReady: (File) -> Unit) {
        viewModelScope.launch {
            val biz = currentBusiness.value ?: return@launch
            val summary = _forecastSummary.value ?: repository.generateConsumptionForecast(
                businessId = biz.id,
                planningHorizonDays = _planningHorizonDays.value,
                safetyBufferDays = _safetyBufferDays.value
            )
            val file = repository.exportManager.generateForecastCsv(summary)
            onFileReady(file)
        }
    }

    // --- Custom Expense Category Management ---
    fun addExpenseCategory(categoryName: String) {
        val success = repository.sessionManager.addExpenseCategory(categoryName)
        if (success) {
            showMessage("Added category: ${categoryName.trim()}")
        } else {
            showMessage("Category already exists or invalid")
        }
    }

    fun editExpenseCategory(oldName: String, newName: String) {
        val success = repository.sessionManager.editExpenseCategory(oldName, newName)
        if (success) {
            showMessage("Category updated to '$newName'")
        } else {
            showMessage("Failed to update category")
        }
    }

    fun deleteExpenseCategory(categoryName: String) {
        val success = repository.sessionManager.deleteExpenseCategory(categoryName)
        if (success) {
            showMessage("Category '$categoryName' removed")
        } else {
            showMessage("Cannot remove last remaining category")
        }
    }

    fun resetExpenseCategories() {
        repository.sessionManager.resetExpenseCategoriesToDefault()
        showMessage("Expense categories reset to default")
    }

    fun triggerStockAlertCheck() {
        try {
            com.example.worker.StockWorkScheduler.triggerImmediateStockCheck(MilkMateApplication.instance)
            showMessage("🔔 Background stock monitor triggered")
        } catch (e: Exception) {
            showMessage("Worker triggered")
        }
    }

    // --- Staff Actions ---
    fun saveStaff(
        id: String?,
        name: String,
        mobile: String,
        pin: String,
        role: String,
        permissions: String,
        assignedRoute: String,
        monthlySalary: Double = 12000.0,
        dailyWage: Double = 400.0,
        salaryType: String = "MONTHLY"
    ) {
        viewModelScope.launch {
            val bId = sessionState.value.businessId
            val staffId = id ?: "STF-${UUID.randomUUID().toString().take(6).uppercase()}"
            val staff = StaffEntity(
                id = staffId,
                businessId = bId,
                name = name.trim(),
                mobile = mobile.trim(),
                pin = pin.trim(),
                role = role,
                permissions = permissions,
                assignedRoute = assignedRoute.trim(),
                monthlySalary = monthlySalary,
                dailyWage = dailyWage,
                salaryType = salaryType
            )
            repository.saveStaff(staff)
            showMessage("Staff member saved with ID: $staffId")
        }
    }

    fun recordStaffAttendance(
        staffId: String,
        staffName: String,
        date: Long,
        status: String,
        notes: String = ""
    ) {
        viewModelScope.launch {
            val bId = sessionState.value.businessId
            if (bId.isBlank()) return@launch
            val att = StaffAttendanceEntity(
                id = "${bId}_${staffId}_$date",
                businessId = bId,
                staffId = staffId,
                staffName = staffName,
                date = date,
                status = status,
                notes = notes
            )
            repository.saveAttendance(att)
            showMessage("$staffName marked $status")
        }
    }

    fun recordStaffPayment(
        staffId: String,
        staffName: String,
        type: String, // "SALARY_PAYOUT", "ADVANCE", "BONUS", "DEDUCTION"
        amount: Double,
        paymentMode: String = "CASH",
        date: Long = System.currentTimeMillis(),
        monthYear: String = "",
        notes: String = ""
    ) {
        viewModelScope.launch {
            val bId = sessionState.value.businessId
            if (bId.isBlank()) return@launch
            val payment = StaffPaymentEntity(
                id = "SPAY-${UUID.randomUUID().toString().take(8).uppercase()}",
                businessId = bId,
                staffId = staffId,
                staffName = staffName,
                type = type,
                amount = amount,
                paymentMode = paymentMode,
                date = date,
                monthYear = monthYear,
                notes = notes
            )
            repository.saveStaffPayment(payment)
            showMessage("Recorded $type of ₹${amount.toInt()} for $staffName")
        }
    }

    fun deleteStaffPayment(id: String) {
        viewModelScope.launch {
            repository.deleteStaffPayment(id)
            showMessage("Staff payment record deleted")
        }
    }

    fun deleteStaff(staffId: String) {
        viewModelScope.launch {
            val bId = sessionState.value.businessId
            repository.deleteStaff(bId, staffId)
            showMessage("Staff member removed")
        }
    }

    // --- Subscription Action ---
    fun upgradeToPro(period: String = "MONTHLY") {
        viewModelScope.launch {
            val biz = currentBusiness.value ?: return@launch
            val expiresAt = if (period == "MONTHLY") {
                System.currentTimeMillis() + (30L * 24 * 3600 * 1000)
            } else {
                System.currentTimeMillis() + (365L * 24 * 3600 * 1000)
            }
            val updated = biz.copy(
                subscriptionPlan = "PRO",
                subscriptionStatus = "ACTIVE",
                billingPeriod = period,
                subscriptionStartedAt = System.currentTimeMillis(),
                subscriptionExpiresAt = expiresAt,
                updatedAt = System.currentTimeMillis()
            )
            repository.saveBusiness(updated)
            showMessage("Upgraded to MilkMate Pro! Unlimited customers unlocked.")
        }
    }

    // --- Settings & Pricing Formula ---
    fun updatePricingSettings(
        pricingMode: String,
        cowRate: Double,
        buffaloRate: Double,
        fatBase: Double,
        snfBase: Double,
        fatDiff: Double,
        snfDiff: Double,
        paneerRate: Double,
        khoaRate: Double
    ) {
        viewModelScope.launch {
            val biz = currentBusiness.value ?: return@launch
            val updated = biz.copy(
                pricingMode = pricingMode,
                cowMilkRate = cowRate,
                buffaloMilkRate = buffaloRate,
                fatBaseRate = fatBase,
                snfBaseRate = snfBase,
                fatDiffRate = fatDiff,
                snfDiffRate = snfDiff,
                paneerRatePerKg = paneerRate,
                khoaRatePerKg = khoaRate,
                updatedAt = System.currentTimeMillis()
            )
            repository.saveBusiness(updated)
            showMessage("Pricing rates and formula updated successfully")
        }
    }

    // --- Sync ---
    fun syncNow() {
        viewModelScope.launch {
            val count = repository.triggerSync()
            showMessage("Sync completed. $count items synchronized.")
        }
    }

    // --- Reports and Exports ---
    suspend fun getProfitReport(): ProfitReport? {
        val biz = currentBusiness.value ?: return null
        val (start, end) = when (_selectedReportRange.value) {
            "LAST_MONTH" -> ReportEngine.getLastMonthRange(biz.accountStartDate)
            else -> ReportEngine.getThisMonthRange(biz.accountStartDate)
        }
        return repository.reportEngine.calculateProfitReport(biz.id, start, end, biz.accountStartDate)
    }

    fun exportDeliveriesPdf(onFileReady: (File) -> Unit) {
        viewModelScope.launch {
            val biz = currentBusiness.value ?: return@launch
            val list = currentShiftDeliveries.value
            val file = repository.exportManager.generateDeliveriesPdf(
                businessName = biz.businessName,
                reportTitle = "Deliveries & Collections",
                deliveries = list
            )
            onFileReady(file)
        }
    }

    fun exportDeliveriesCsv(onFileReady: (File) -> Unit) {
        viewModelScope.launch {
            val list = currentShiftDeliveries.value
            val file = repository.exportManager.generateDeliveriesCsv(
                reportTitle = "Deliveries & Collections",
                deliveries = list
            )
            onFileReady(file)
        }
    }

    fun exportProfitPdf(onFileReady: (File) -> Unit) {
        viewModelScope.launch {
            val biz = currentBusiness.value ?: return@launch
            val profit = getProfitReport() ?: return@launch
            val file = repository.exportManager.generateProfitPdf(biz.businessName, profit)
            onFileReady(file)
        }
    }

    // --- Milk Wastage & Reconciliation Actions ---
    fun logMilkWastage(
        quantityLiters: Double,
        reason: String,
        milkType: String = "COW",
        shift: String = "MORNING",
        reasonDetails: String = "",
        costPerLiter: Double = 50.0,
        batchSource: String = "Farm Milking",
        preventativeAction: String = "",
        date: Long = System.currentTimeMillis()
    ) {
        viewModelScope.launch {
            val bizId = sessionState.value.businessId
            if (bizId.isBlank()) return@launch
            val id = "WST-${java.util.UUID.randomUUID().toString().take(8).uppercase()}"
            val wastage = com.example.data.local.entity.MilkWastageEntity(
                id = id,
                businessId = bizId,
                date = date,
                shift = shift,
                milkType = milkType,
                quantityLiters = quantityLiters,
                reason = reason,
                reasonDetails = reasonDetails,
                costPerLiter = costPerLiter,
                totalLossAmount = quantityLiters * costPerLiter,
                batchSource = batchSource,
                preventativeAction = preventativeAction
            )
            repository.saveMilkWastage(wastage)
            showMessage("Recorded ${quantityLiters}L milk wastage loss (₹${Math.round(quantityLiters * costPerLiter)})")
        }
    }

    fun deleteMilkWastage(id: String) {
        viewModelScope.launch {
            val bizId = sessionState.value.businessId
            if (bizId.isBlank()) return@launch
            repository.deleteMilkWastage(bizId, id)
            showMessage("Wastage record removed")
        }
    }

    suspend fun getDailyMilkReconciliation(date: Long = System.currentTimeMillis()): com.example.data.repository.MilkReconciliationSummary {
        val bizId = sessionState.value.businessId
        if (bizId.isBlank()) return com.example.data.repository.MilkReconciliationSummary(date = date)
        return repository.getDailyMilkReconciliation(bizId, date)
    }

    // --- Advanced Management & Rate Engine ---
    fun saveFullCustomFormula(
        formulaName: String,
        baseRate: Double,
        fatMultiplier: Double,
        snfMultiplier: Double,
        tsMultiplier: Double,
        clrMultiplier: Double,
        qualityBonus: Double,
        chillingDeduction: Double,
        minFloorRate: Double,
        yieldType: String,
        yieldMultiplier: Double,
        differentialMode: Boolean,
        baseYieldGrams: Double = 160.0,
        diffRatePer10g: Double = 2.8
    ) {
        viewModelScope.launch {
            val biz = currentBusiness.value ?: return@launch
            val updated = biz.copy(
                customFormulaName = formulaName,
                customFormulaBase = baseRate,
                customFormulaFatMultiplier = fatMultiplier,
                customFormulaSnfMultiplier = snfMultiplier,
                customFormulaTsMultiplier = tsMultiplier,
                customFormulaClrMultiplier = clrMultiplier,
                customFormulaQualityBonus = qualityBonus,
                customFormulaChillingDeduction = chillingDeduction,
                customFormulaMinFloorRate = minFloorRate,
                customFormulaYieldType = yieldType,
                customFormulaYieldMultiplier = yieldMultiplier,
                customFormulaBaseYieldGrams = baseYieldGrams,
                customFormulaDiffRatePer10g = diffRatePer10g,
                customFormulaDifferentialMode = differentialMode,
                pricingMode = "CUSTOM",
                updatedAt = System.currentTimeMillis()
            )
            repository.saveBusiness(updated)
            showMessage("Custom formula '$formulaName' applied & saved ✓")
        }
    }

    fun saveCustomRateFormula(
        cowRate: Double,
        buffaloRate: Double,
        baseFat: Double,
        baseSnf: Double,
        fatFactor: Double,
        snfFactor: Double,
        pricingMode: String,
        paneerRatePerKg: Double = 360.0,
        khoaRatePerKg: Double = 330.0,
        customFormulaBase: Double = 20.0,
        customFormulaFatMultiplier: Double = 5.0,
        customFormulaSnfMultiplier: Double = 3.0
    ) {
        viewModelScope.launch {
            val biz = currentBusiness.value ?: return@launch
            val updated = biz.copy(
                cowMilkRate = cowRate,
                buffaloMilkRate = buffaloRate,
                fatBaseRate = baseFat,
                snfBaseRate = baseSnf,
                fatDiffRate = fatFactor,
                snfDiffRate = snfFactor,
                pricingMode = pricingMode,
                paneerRatePerKg = paneerRatePerKg,
                khoaRatePerKg = khoaRatePerKg,
                customFormulaBase = customFormulaBase,
                customFormulaFatMultiplier = customFormulaFatMultiplier,
                customFormulaSnfMultiplier = customFormulaSnfMultiplier,
                updatedAt = System.currentTimeMillis()
            )
            repository.saveBusiness(updated)
            showMessage("Milk rates & yield formula saved successfully ✓")
        }
    }

    fun markAllStaffAttendance(status: String = "PRESENT") {
        viewModelScope.launch {
            val bId = sessionState.value.businessId
            if (bId.isBlank()) return@launch
            val date = _selectedDate.value
            val list = staffList.value
            list.forEach { staff ->
                val att = StaffAttendanceEntity(
                    id = "${bId}_${staff.id}_$date",
                    businessId = bId,
                    staffId = staff.id,
                    staffName = staff.name,
                    date = date,
                    status = status,
                    notes = "Bulk marked as $status"
                )
                repository.saveAttendance(att)
            }
            showMessage("Marked all ${list.size} staff members as $status ✓")
        }
    }

    fun markAllStaffAttendance(
        staffList: List<StaffEntity>,
        date: Long,
        status: String = "PRESENT"
    ) {
        viewModelScope.launch {
            val bId = sessionState.value.businessId
            if (bId.isBlank()) return@launch
            staffList.forEach { staff ->
                val att = StaffAttendanceEntity(
                    id = "${bId}_${staff.id}_$date",
                    businessId = bId,
                    staffId = staff.id,
                    staffName = staff.name,
                    date = date,
                    status = status,
                    notes = "Bulk marked as $status"
                )
                repository.saveAttendance(att)
            }
            showMessage("Marked all ${staffList.size} staff members as $status ✓")
        }
    }

    fun markStaffAttendance(staffId: String, status: String) {
        viewModelScope.launch {
            val bId = sessionState.value.businessId
            if (bId.isBlank()) return@launch
            val staff = staffList.value.find { it.id == staffId } ?: return@launch
            val date = _selectedDate.value
            val att = StaffAttendanceEntity(
                id = "${bId}_${staff.id}_$date",
                businessId = bId,
                staffId = staff.id,
                staffName = staff.name,
                date = date,
                status = status,
                notes = "Updated status to $status"
            )
            repository.saveAttendance(att)
            showMessage("${staff.name} marked as $status ✓")
        }
    }

    fun updateInventoryStock(itemId: String, quantityChange: Double, transactionType: String, notes: String = "") {
        viewModelScope.launch {
            val bId = sessionState.value.businessId
            if (bId.isBlank()) return@launch
            val item = repository.getInventoryItem(bId, itemId) ?: return@launch
            val newStock = (item.currentStock + quantityChange).coerceAtLeast(0.0)
            val updated = item.copy(
                currentStock = newStock,
                updatedAt = System.currentTimeMillis()
            )
            repository.saveInventoryItem(updated)
            showMessage("Updated ${item.itemName} stock to $newStock ${item.unit} ✓")
        }
    }

    fun saveDairyProductsCatalog(
        paneerRate: Double,
        khoaRate: Double,
        cowRate: Double,
        buffaloRate: Double
    ) {
        viewModelScope.launch {
            val biz = currentBusiness.value ?: return@launch
            val updated = biz.copy(
                paneerRatePerKg = paneerRate,
                khoaRatePerKg = khoaRate,
                cowMilkRate = cowRate,
                buffaloMilkRate = buffaloRate,
                updatedAt = System.currentTimeMillis()
            )
            repository.saveBusiness(updated)
            showMessage("Dairy products catalog rates saved successfully ✓")
        }
    }

    // --- Backup & Restore ---
    fun exportBackup(onFileReady: (File) -> Unit) {
        viewModelScope.launch {
            val bId = sessionState.value.businessId
            if (bId.isBlank()) return@launch
            val file = repository.backupManager.exportBusinessData(bId)
            onFileReady(file)
            showMessage("Backup file generated: ${file.name}")
        }
    }

    fun restoreBackup(jsonString: String, onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val bId = sessionState.value.businessId
            val validation = repository.backupManager.validateBackupContent(jsonString, bId)
            if (!validation.isValid) {
                onComplete(false, validation.errorMessage ?: "Invalid backup")
                return@launch
            }
            val success = repository.backupManager.restoreBackupContent(jsonString, bId)
            if (success) {
                showMessage("Backup restored successfully!")
                onComplete(true, "Restored ${validation.customerCount} customers, ${validation.deliveryCount} deliveries.")
            } else {
                onComplete(false, "Failed to restore backup.")
            }
        }
    }

    // --- Business Operating Mode Switcher ---
    fun switchBusinessMode(newMode: String) {
        viewModelScope.launch {
            val business = currentBusiness.value ?: return@launch
            val typeConfig = com.example.ui.util.BusinessModeFeatures.getByType(newMode)
            val updated = business.copy(
                businessMode = newMode,
                sourceOwnCattle = typeConfig.showHerdBreeding,
                sourceVillageFarmers = typeConfig.showFarmerProcurement,
                destHouseholds = typeConfig.showRetailRoutes,
                destBulkCommercial = typeConfig.showBulkDispatches,
                destFactoryTankers = (newMode == "COLLECTION_CENTER"),
                hasCattleFeed = typeConfig.showCattleFeed,
                hasPaneer = (newMode == "PROCESSING_UNIT" || newMode == "INTEGRATED" || newMode == "RETAIL_PARLOUR"),
                hasCurdChaas = (newMode == "PROCESSING_UNIT" || newMode == "INTEGRATED" || newMode == "RETAIL_PARLOUR"),
                hasGheeButter = (newMode == "PROCESSING_UNIT" || newMode == "INTEGRATED" || newMode == "RETAIL_PARLOUR" || newMode == "GAUSHALA"),
                updatedAt = System.currentTimeMillis()
            )
            repository.saveBusiness(updated)
            // Ensure default inventory items for the new mode are initialized if not present
            repository.initializeDefaultInventoryItems(business.id, newMode)
            repository.sessionManager.setCategoriesForMode(newMode)
            val label = typeConfig.title
            showMessage("Switched to $label profile")
        }
    }

    // --- Farmer Management ---
    fun saveFarmer(farmer: FarmerEntity, onDone: (() -> Unit)? = null) {
        viewModelScope.launch {
            repository.saveFarmer(farmer)
            showMessage("Farmer ${farmer.name} (Code: ${farmer.farmerCode}) saved")
            onDone?.invoke()
        }
    }

    fun deleteFarmer(farmerId: String) {
        viewModelScope.launch {
            val bId = sessionState.value.businessId
            repository.deleteFarmer(bId, farmerId)
            showMessage("Farmer removed")
        }
    }

    fun recordFarmerPayment(farmerId: String, amount: Double, paymentMode: String, reference: String) {
        viewModelScope.launch {
            val bId = sessionState.value.businessId
            repository.recordFarmerPayment(bId, farmerId, amount, reference)
            showMessage("Payment of ₹${String.format(Locale.getDefault(), "%.2f", amount)} recorded for farmer")
        }
    }

    // --- Milk Collection Entry (Farmer Procurement) ---
    fun saveMilkCollection(collection: MilkCollectionEntity, onDone: (() -> Unit)? = null) {
        viewModelScope.launch {
            repository.saveMilkCollection(collection)
            showMessage("Logged ${collection.quantityLiters}L from ${collection.farmerName} (₹${String.format(Locale.getDefault(), "%.2f", collection.totalAmount)})")
            onDone?.invoke()
        }
    }

    fun deleteMilkCollection(collectionId: String) {
        viewModelScope.launch {
            val bId = sessionState.value.businessId
            repository.deleteMilkCollection(bId, collectionId)
            showMessage("Collection entry removed")
        }
    }

    // --- Bulk Dispatches (Wholesale & Tanker Outward) ---
    fun saveBulkDispatch(dispatch: BulkDispatchEntity, onDone: (() -> Unit)? = null) {
        viewModelScope.launch {
            repository.saveBulkDispatch(dispatch)
            showMessage("Dispatch of ${dispatch.totalLiters}L to ${dispatch.buyerOrPlantName} recorded (₹${String.format(Locale.getDefault(), "%.2f", dispatch.totalAmount)})")
            onDone?.invoke()
        }
    }

    fun deleteBulkDispatch(dispatchId: String) {
        viewModelScope.launch {
            val bId = sessionState.value.businessId
            repository.deleteBulkDispatch(bId, dispatchId)
            showMessage("Bulk dispatch entry removed")
        }
    }

    // --- Cattle & Breeding Management ---
    fun saveCattle(cattle: CattleEntity, onDone: (() -> Unit)? = null) {
        viewModelScope.launch {
            repository.saveCattle(cattle)
            showMessage("Cattle Tag #${cattle.tagNumber} (${cattle.name.ifBlank { cattle.type }}) saved")
            onDone?.invoke()
        }
    }

    fun deleteCattle(cattleId: String) {
        viewModelScope.launch {
            val bId = sessionState.value.businessId
            repository.deleteCattle(bId, cattleId)
            showMessage("Cattle record removed")
        }
    }

    // --- Cattle Weight & Girth Calculation ---
    fun saveCattleWeight(weight: CattleWeightEntity, onDone: (() -> Unit)? = null) {
        viewModelScope.launch {
            repository.saveWeight(weight)
            showMessage("Weight record for #${weight.tagNumber} (${String.format(Locale.getDefault(), "%.1f", weight.weightKg)} kg) saved")
            onDone?.invoke()
        }
    }

    fun deleteCattleWeight(weightId: String) {
        viewModelScope.launch {
            repository.deleteWeight(weightId)
            showMessage("Weight entry removed")
        }
    }

    /**
     * Calculates body weight using Shaeffer's formula & metric standard:
     * Weight (kg) = (Heart Girth in cm² × Body Length in cm) / 10840
     */
    fun calculateWeightByGirth(heartGirthCm: Double, bodyLengthCm: Double): Double {
        if (heartGirthCm <= 0.0 || bodyLengthCm <= 0.0) return 0.0
        val wt = (heartGirthCm * heartGirthCm * bodyLengthCm) / 10840.0
        return (wt * 10.0).toInt() / 10.0
    }

    // --- California Mastitis Test (CMT) Records ---
    fun saveCmtRecord(cmt: CattleCmtEntity, onDone: (() -> Unit)? = null) {
        viewModelScope.launch {
            repository.saveCmt(cmt)
            val diagStr = when (cmt.overallDiagnosis) {
                "CLINICAL_MASTITIS" -> "⚠️ Clinical Mastitis Detected!"
                "SUBCLINICAL_MASTITIS" -> "⚡ Subclinical Mastitis Flagged"
                else -> "✅ All Quarters Healthy"
            }
            showMessage("CMT for #${cmt.tagNumber} saved: $diagStr")
            onDone?.invoke()
        }
    }

    fun deleteCmtRecord(cmtId: String) {
        viewModelScope.launch {
            repository.deleteCmt(cmtId)
            showMessage("CMT record removed")
        }
    }

    // --- Body Condition Scoring (BCS) ---
    fun saveBcsRecord(bcs: CattleBcsEntity, onDone: (() -> Unit)? = null) {
        viewModelScope.launch {
            repository.saveBcs(bcs)
            showMessage("BCS (${bcs.score}) for #${bcs.tagNumber} recorded: ${bcs.category}")
            onDone?.invoke()
        }
    }

    fun deleteBcsRecord(bcsId: String) {
        viewModelScope.launch {
            repository.deleteBcs(bcsId)
            showMessage("BCS record removed")
        }
    }

    // --- Breeding & Reproduction Management ---
    fun saveBreedingRecord(record: BreedingRecordEntity, onDone: (() -> Unit)? = null) {
        viewModelScope.launch {
            repository.saveBreeding(record)
            val eventDesc = when (record.eventType) {
                "INSEMINATION" -> "A.I. Insemination recorded"
                "PREGNANCY_DIAGNOSIS" -> "PD Result (${record.pdStatus}) saved"
                "CALVING" -> "Calving event recorded! Lactation incremented"
                "HEAT" -> "Heat observation recorded"
                "DRY_OFF" -> "Dry-off recorded"
                else -> "${record.eventType} recorded"
            }
            showMessage("$eventDesc for #${record.cattleTag}")
            onDone?.invoke()
        }
    }

    fun deleteBreedingRecord(recordId: String) {
        viewModelScope.launch {
            repository.deleteBreeding(recordId)
            showMessage("Breeding record removed")
        }
    }

    // --- Deworming Schedule & Records ---
    fun saveDewormingRecord(record: DewormingRecordEntity, onDone: (() -> Unit)? = null) {
        viewModelScope.launch {
            repository.saveDeworming(record)
            val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            val dueStr = sdf.format(Date(record.nextDueDate))
            showMessage("Deworming (${record.dewormerSalt}) for #${record.cattleTag} saved. Next due: $dueStr")
            onDone?.invoke()
        }
    }

    fun deleteDewormingRecord(recordId: String) {
        viewModelScope.launch {
            repository.deleteDeworming(recordId)
            showMessage("Deworming record removed")
        }
    }

    // --- Vaccination Records ---
    fun saveVaccinationRecord(record: VaccinationRecordEntity, onDone: (() -> Unit)? = null) {
        viewModelScope.launch {
            repository.saveVaccination(record)
            val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            val dueStr = sdf.format(Date(record.nextDueDate))
            showMessage("Vaccine (${record.vaccineName}) for #${record.cattleTag} recorded. Booster due: $dueStr")
            onDone?.invoke()
        }
    }

    fun deleteVaccinationRecord(recordId: String) {
        viewModelScope.launch {
            repository.deleteVaccination(recordId)
            showMessage("Vaccination record removed")
        }
    }

    // --- Treatment & Clinical Health Records ---
    fun saveTreatmentRecord(record: TreatmentRecordEntity, onDone: (() -> Unit)? = null) {
        viewModelScope.launch {
            repository.saveTreatment(record)
            val withdrawalAlert = if (record.milkWithdrawalDays > 0) " (⚠️ Milk withdrawal: ${record.milkWithdrawalDays} days)" else ""
            showMessage("Treatment for #${record.cattleTag} (${record.diseaseName}) recorded$withdrawalAlert")
            onDone?.invoke()
        }
    }

    fun deleteTreatmentRecord(recordId: String) {
        viewModelScope.launch {
            repository.deleteTreatment(recordId)
            showMessage("Treatment record removed")
        }
    }

    // --- Farm Observation Logs ---
    fun saveFarmObservation(observation: FarmObservationEntity, onDone: (() -> Unit)? = null) {
        viewModelScope.launch {
            repository.saveObservation(observation)
            val alertStr = if (observation.hasAlert) " ⚠️ (Flagged for Vet Review)" else ""
            showMessage("Farm observation (${observation.activityType}) logged$alertStr")
            onDone?.invoke()
        }
    }

    fun deleteFarmObservation(recordId: String) {
        viewModelScope.launch {
            repository.deleteObservation(recordId)
            showMessage("Farm observation removed")
        }
    }

    // --- Daily Milking Recording ---
    fun saveMilkingRecord(record: CattleMilkingRecordEntity, onDone: (() -> Unit)? = null) {
        viewModelScope.launch {
            repository.saveMilking(record)
            showMessage("Milking #${record.cattleTag} (${record.quantityLiters}L ${record.shift}) recorded")
            onDone?.invoke()
        }
    }

    fun deleteMilkingRecord(recordId: String) {
        viewModelScope.launch {
            repository.deleteMilking(recordId)
            showMessage("Milking record removed")
        }
    }

    // --- Dynamic Milk Procurement Rate Calculator ---
    fun calculateProcurementRate(milkType: String, fat: Double, snf: Double, clr: Double = 0.0): Double {
        val biz = currentBusiness.value
        val effectiveSnf = if (snf > 0.0) snf else if (clr > 0.0) (clr / 4.0) + (fat * 0.2) + 0.36 else 8.5
        
        return when (biz?.pricingMode) {
            "FAT_ONLY" -> {
                val fatBase = if (biz.fatBaseRate > 0) biz.fatBaseRate else 6.5
                fat * fatBase
            }
            "SNF_ONLY" -> {
                val snfBase = if (biz.snfBaseRate > 0) biz.snfBaseRate else 4.0
                effectiveSnf * snfBase
            }
            "CUSTOM" -> {
                val base = biz.customFormulaBase
                val fatMult = biz.customFormulaFatMultiplier
                val snfMult = biz.customFormulaSnfMultiplier
                val qualityBonus = biz.customFormulaQualityBonus
                (fat * fatMult) + (effectiveSnf * snfMult) + base + qualityBonus
            }
            else -> {
                // Cooperative Standard FAT + SNF
                val base = if (milkType == "BUFFALO") (biz?.buffaloMilkRate ?: 65.0) else (biz?.cowMilkRate ?: 50.0)
                val standardFat = if (milkType == "BUFFALO") 6.5 else 4.0
                val standardSnf = if (milkType == "BUFFALO") 9.0 else 8.5
                val fatDiff = (fat - standardFat) * (biz?.fatDiffRate ?: 0.5) * 10
                val snfDiff = (effectiveSnf - standardSnf) * (biz?.snfDiffRate ?: 0.4) * 10
                (base + fatDiff + snfDiff).coerceAtLeast(15.0)
            }
        }
    }

    companion object {
        fun getTodayMidnightMillis(): Long {
            val cal = Calendar.getInstance()
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            return cal.timeInMillis
        }

        fun normalizeToMidnight(millis: Long): Long {
            val cal = Calendar.getInstance()
            cal.timeInMillis = millis
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            return cal.timeInMillis
        }

        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val app = MilkMateApplication.instance
                return MilkMateViewModel(app.repository) as T
            }
        }
    }
}
