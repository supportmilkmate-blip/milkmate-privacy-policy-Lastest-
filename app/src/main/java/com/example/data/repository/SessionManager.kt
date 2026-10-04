package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

val DEFAULT_EXPENSE_CATEGORIES = listOf(
    "Cattle Feed",
    "Veterinary & Medicines",
    "Labor & Wages",
    "Electricity & Fuel",
    "Maintenance & Machinery",
    "Green Fodder / Silage",
    "Transportation",
    "Packaging & Milk Cans",
    "Personal Drawings",
    "Misc Farm Expense"
)

data class UserSession(
    val isLoggedIn: Boolean = false,
    val businessId: String = "",
    val userId: String = "",
    val role: String = "", // OWNER or STAFF
    val staffId: String = "",
    val staffName: String = "",
    val permissions: String = "",
    val accountStartDate: Long = 0L,
    val languageCode: String = "en",
    val upiId: String = "",
    val themeMode: String = "SYSTEM", // SYSTEM, LIGHT, DARK
    val accentTheme: String = "BLUE", // BLUE, GREEN, GOLD, PURPLE, TEAL, RED
    val confirmDelete: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val morningReminderEnabled: Boolean = true,
    val morningReminderTime: String = "06:00",
    val afternoonReminderEnabled: Boolean = true,
    val afternoonReminderTime: String = "13:00",
    val eveningReminderEnabled: Boolean = true,
    val eveningReminderTime: String = "17:30",
    val advanceAlertMinutes: Int = 0, // 0, 15, 30, 60, 120, 240, 720, 1440
    val milkingReminderEnabled: Boolean = true,
    val deliveryReminderEnabled: Boolean = true,
    val vaccineAlertEnabled: Boolean = true,
    val dewormingAlertEnabled: Boolean = true,
    val breedingCalvingAlertEnabled: Boolean = true,
    val paymentDueAlertsEnabled: Boolean = true,
    val paymentDueThreshold: Double = 500.0,
    val lowStockAlertsEnabled: Boolean = true,
    val stockAlertThresholdDays: Int = 15,
    val soundAlertsEnabled: Boolean = true,
    val vibrationAlertsEnabled: Boolean = true,
    val dailySummaryAlertsEnabled: Boolean = true,
    val dailySummaryTime: String = "21:00",
    val billNote: String = "Thank you for choosing pure fresh dairy milk!",
    val customExpenseCategories: List<String> = DEFAULT_EXPENSE_CATEGORIES,
    val appLockEnabled: Boolean = false,
    val securityPin: String = "",
    val biometricEnabled: Boolean = false,
    val securityPassword: String = "",
    val autoLockTimeoutMinutes: Int = 0,
    val isAppLocked: Boolean = false,
    val registeredPhone: String = ""
) {
    fun isOwner(): Boolean = role == "OWNER"
    fun isStaff(): Boolean = role == "STAFF"

    fun canAccess(module: String): Boolean {
        if (isOwner()) return true
        val perms = permissions.split(",").map { it.trim().uppercase() }
        return perms.contains(module.uppercase()) || perms.contains("ALL")
    }
}

class SessionManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("milkmate_session_prefs", Context.MODE_PRIVATE)

    private val _sessionState = MutableStateFlow(loadSession())
    val sessionState: StateFlow<UserSession> = _sessionState.asStateFlow()

    private fun loadSession(): UserSession {
        val isLoggedIn = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        val businessId = prefs.getString(KEY_BUSINESS_ID, "") ?: ""
        val userId = prefs.getString(KEY_USER_ID, "") ?: ""
        val role = prefs.getString(KEY_ROLE, "") ?: ""
        val staffId = prefs.getString(KEY_STAFF_ID, "") ?: ""
        val staffName = prefs.getString(KEY_STAFF_NAME, "") ?: ""
        val permissions = prefs.getString(KEY_PERMISSIONS, "") ?: ""
        val accountStartDate = prefs.getLong(KEY_ACCOUNT_START_DATE, 0L)
        val languageCode = prefs.getString(KEY_LANG, "en") ?: "en"
        val upiId = prefs.getString(KEY_UPI_ID, "") ?: ""
        val themeMode = prefs.getString(KEY_THEME_MODE, "SYSTEM") ?: "SYSTEM"
        val accentTheme = prefs.getString(KEY_ACCENT_THEME, "BLUE") ?: "BLUE"
        val confirmDelete = prefs.getBoolean(KEY_CONFIRM_DELETE, true)
        val notificationsEnabled = prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, true)
        val morningReminderEnabled = prefs.getBoolean(KEY_MORNING_REMINDER_ENABLED, true)
        val morningReminderTime = prefs.getString(KEY_MORNING_REMINDER_TIME, "06:00") ?: "06:00"
        val afternoonReminderEnabled = prefs.getBoolean(KEY_AFTERNOON_REMINDER_ENABLED, true)
        val afternoonReminderTime = prefs.getString(KEY_AFTERNOON_REMINDER_TIME, "13:00") ?: "13:00"
        val eveningReminderEnabled = prefs.getBoolean(KEY_EVENING_REMINDER_ENABLED, true)
        val eveningReminderTime = prefs.getString(KEY_EVENING_REMINDER_TIME, "17:30") ?: "17:30"
        val advanceAlertMinutes = prefs.getInt(KEY_ADVANCE_ALERT_MINUTES, 0)
        val milkingReminderEnabled = prefs.getBoolean(KEY_MILKING_REMINDER_ENABLED, true)
        val deliveryReminderEnabled = prefs.getBoolean(KEY_DELIVERY_REMINDER_ENABLED, true)
        val vaccineAlertEnabled = prefs.getBoolean(KEY_VACCINE_ALERT_ENABLED, true)
        val dewormingAlertEnabled = prefs.getBoolean(KEY_DEWORMING_ALERT_ENABLED, true)
        val breedingCalvingAlertEnabled = prefs.getBoolean(KEY_BREEDING_CALVING_ALERT_ENABLED, true)
        val paymentDueAlertsEnabled = prefs.getBoolean(KEY_PAYMENT_DUE_ALERTS_ENABLED, true)
        val paymentDueThreshold = prefs.getFloat(KEY_PAYMENT_DUE_THRESHOLD, 500f).toDouble()
        val lowStockAlertsEnabled = prefs.getBoolean(KEY_LOW_STOCK_ALERTS_ENABLED, true)
        val stockAlertThresholdDays = prefs.getInt(KEY_STOCK_ALERT_THRESHOLD_DAYS, 15)
        val soundAlertsEnabled = prefs.getBoolean(KEY_SOUND_ALERTS_ENABLED, true)
        val vibrationAlertsEnabled = prefs.getBoolean(KEY_VIBRATION_ALERTS_ENABLED, true)
        val dailySummaryAlertsEnabled = prefs.getBoolean(KEY_DAILY_SUMMARY_ALERTS_ENABLED, true)
        val dailySummaryTime = prefs.getString(KEY_DAILY_SUMMARY_TIME, "21:00") ?: "21:00"
        val billNote = prefs.getString(KEY_BILL_NOTE, "Thank you for choosing pure fresh dairy milk!") ?: "Thank you for choosing pure fresh dairy milk!"
        val appLockEnabled = prefs.getBoolean(KEY_APP_LOCK_ENABLED, false)
        val securityPin = prefs.getString(KEY_SECURITY_PIN, "") ?: ""
        val biometricEnabled = prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false)
        val securityPassword = prefs.getString(KEY_SECURITY_PASSWORD, "") ?: ""
        val autoLockTimeout = prefs.getInt(KEY_AUTOLOCK_TIMEOUT_MINUTES, 0)
        val registeredPhone = prefs.getString(KEY_REGISTERED_PHONE, "") ?: ""

        val categoriesRaw = prefs.getString(KEY_CUSTOM_EXPENSE_CATEGORIES, null)
        val categories = if (categoriesRaw.isNullOrBlank()) {
            DEFAULT_EXPENSE_CATEGORIES
        } else {
            categoriesRaw.split("|||").filter { it.isNotBlank() }.ifEmpty { DEFAULT_EXPENSE_CATEGORIES }
        }

        return UserSession(
            isLoggedIn = isLoggedIn,
            businessId = businessId,
            userId = userId,
            role = role,
            staffId = staffId,
            staffName = staffName,
            permissions = permissions,
            accountStartDate = accountStartDate,
            languageCode = languageCode,
            upiId = upiId,
            themeMode = themeMode,
            accentTheme = accentTheme,
            confirmDelete = confirmDelete,
            notificationsEnabled = notificationsEnabled,
            morningReminderEnabled = morningReminderEnabled,
            morningReminderTime = morningReminderTime,
            afternoonReminderEnabled = afternoonReminderEnabled,
            afternoonReminderTime = afternoonReminderTime,
            eveningReminderEnabled = eveningReminderEnabled,
            eveningReminderTime = eveningReminderTime,
            advanceAlertMinutes = advanceAlertMinutes,
            milkingReminderEnabled = milkingReminderEnabled,
            deliveryReminderEnabled = deliveryReminderEnabled,
            vaccineAlertEnabled = vaccineAlertEnabled,
            dewormingAlertEnabled = dewormingAlertEnabled,
            breedingCalvingAlertEnabled = breedingCalvingAlertEnabled,
            paymentDueAlertsEnabled = paymentDueAlertsEnabled,
            paymentDueThreshold = paymentDueThreshold,
            lowStockAlertsEnabled = lowStockAlertsEnabled,
            stockAlertThresholdDays = stockAlertThresholdDays,
            soundAlertsEnabled = soundAlertsEnabled,
            vibrationAlertsEnabled = vibrationAlertsEnabled,
            dailySummaryAlertsEnabled = dailySummaryAlertsEnabled,
            dailySummaryTime = dailySummaryTime,
            billNote = billNote,
            customExpenseCategories = categories,
            appLockEnabled = appLockEnabled,
            securityPin = securityPin,
            biometricEnabled = biometricEnabled,
            securityPassword = securityPassword,
            autoLockTimeoutMinutes = autoLockTimeout,
            isAppLocked = appLockEnabled && securityPin.isNotBlank(),
            registeredPhone = registeredPhone
        )
    }

    fun saveOwnerSession(businessId: String, ownerUid: String, accountStartDate: Long, phone: String = "") {
        val editor = prefs.edit()
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .putString(KEY_BUSINESS_ID, businessId)
            .putString(KEY_USER_ID, ownerUid)
            .putString(KEY_ROLE, "OWNER")
            .putString(KEY_STAFF_ID, "")
            .putString(KEY_STAFF_NAME, "Owner")
            .putString(KEY_PERMISSIONS, "ALL")
            .putLong(KEY_ACCOUNT_START_DATE, accountStartDate)

        if (phone.isNotBlank()) {
            editor.putString(KEY_REGISTERED_PHONE, phone.trim())
        }
        editor.apply()

        _sessionState.value = loadSession()
    }

    fun saveStaffSession(businessId: String, staffId: String, staffName: String, permissions: String, accountStartDate: Long) {
        prefs.edit()
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .putString(KEY_BUSINESS_ID, businessId)
            .putString(KEY_USER_ID, staffId)
            .putString(KEY_ROLE, "STAFF")
            .putString(KEY_STAFF_ID, staffId)
            .putString(KEY_STAFF_NAME, staffName)
            .putString(KEY_PERMISSIONS, permissions)
            .putLong(KEY_ACCOUNT_START_DATE, accountStartDate)
            .apply()

        _sessionState.value = loadSession()
    }

    fun setLanguage(langCode: String) {
        prefs.edit().putString(KEY_LANG, langCode).apply()
        _sessionState.value = loadSession()
    }

    fun setUpiId(upi: String) {
        prefs.edit().putString(KEY_UPI_ID, upi.trim()).apply()
        _sessionState.value = loadSession()
    }

    fun setThemeMode(mode: String) {
        prefs.edit().putString(KEY_THEME_MODE, mode).apply()
        _sessionState.value = loadSession()
    }

    fun setAccentTheme(accent: String) {
        prefs.edit().putString(KEY_ACCENT_THEME, accent).apply()
        _sessionState.value = loadSession()
    }

    fun setConfirmDelete(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_CONFIRM_DELETE, enabled).apply()
        _sessionState.value = loadSession()
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled).apply()
        _sessionState.value = loadSession()
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
        val editor = prefs.edit()
        notificationsEnabled?.let { editor.putBoolean(KEY_NOTIFICATIONS_ENABLED, it) }
        morningEnabled?.let { editor.putBoolean(KEY_MORNING_REMINDER_ENABLED, it) }
        morningTime?.let { editor.putString(KEY_MORNING_REMINDER_TIME, it) }
        afternoonEnabled?.let { editor.putBoolean(KEY_AFTERNOON_REMINDER_ENABLED, it) }
        afternoonTime?.let { editor.putString(KEY_AFTERNOON_REMINDER_TIME, it) }
        eveningEnabled?.let { editor.putBoolean(KEY_EVENING_REMINDER_ENABLED, it) }
        eveningTime?.let { editor.putString(KEY_EVENING_REMINDER_TIME, it) }
        advanceMinutes?.let { editor.putInt(KEY_ADVANCE_ALERT_MINUTES, it) }
        milkingEnabled?.let { editor.putBoolean(KEY_MILKING_REMINDER_ENABLED, it) }
        deliveryEnabled?.let { editor.putBoolean(KEY_DELIVERY_REMINDER_ENABLED, it) }
        vaccineEnabled?.let { editor.putBoolean(KEY_VACCINE_ALERT_ENABLED, it) }
        dewormingEnabled?.let { editor.putBoolean(KEY_DEWORMING_ALERT_ENABLED, it) }
        breedingEnabled?.let { editor.putBoolean(KEY_BREEDING_CALVING_ALERT_ENABLED, it) }
        paymentDueEnabled?.let { editor.putBoolean(KEY_PAYMENT_DUE_ALERTS_ENABLED, it) }
        paymentDueThreshold?.let { editor.putFloat(KEY_PAYMENT_DUE_THRESHOLD, it.toFloat()) }
        lowStockEnabled?.let { editor.putBoolean(KEY_LOW_STOCK_ALERTS_ENABLED, it) }
        stockAlertThresholdDays?.let { editor.putInt(KEY_STOCK_ALERT_THRESHOLD_DAYS, it) }
        soundEnabled?.let { editor.putBoolean(KEY_SOUND_ALERTS_ENABLED, it) }
        vibrationEnabled?.let { editor.putBoolean(KEY_VIBRATION_ALERTS_ENABLED, it) }
        dailySummaryEnabled?.let { editor.putBoolean(KEY_DAILY_SUMMARY_ALERTS_ENABLED, it) }
        dailySummaryTime?.let { editor.putString(KEY_DAILY_SUMMARY_TIME, it) }
        editor.apply()
        _sessionState.value = loadSession()
    }

    fun setBillNote(note: String) {
        prefs.edit().putString(KEY_BILL_NOTE, note.trim()).apply()
        _sessionState.value = loadSession()
    }

    fun updateAccountStartDate(newDate: Long) {
        prefs.edit().putLong(KEY_ACCOUNT_START_DATE, newDate).apply()
        _sessionState.value = loadSession()
    }

    // --- Custom Expense Category Management ---
    fun addExpenseCategory(categoryName: String): Boolean {
        val clean = categoryName.trim()
        if (clean.isBlank()) return false
        val current = _sessionState.value.customExpenseCategories.toMutableList()
        if (current.any { it.equals(clean, ignoreCase = true) }) return false
        current.add(clean)
        saveExpenseCategories(current)
        return true
    }

    fun editExpenseCategory(oldName: String, newName: String): Boolean {
        val clean = newName.trim()
        if (clean.isBlank()) return false
        val current = _sessionState.value.customExpenseCategories.toMutableList()
        val index = current.indexOfFirst { it.equals(oldName, ignoreCase = true) }
        if (index == -1) return false
        current[index] = clean
        saveExpenseCategories(current)
        return true
    }

    fun deleteExpenseCategory(categoryName: String): Boolean {
        val current = _sessionState.value.customExpenseCategories.toMutableList()
        if (current.size <= 1) return false // Prevent empty categories
        val removed = current.removeAll { it.equals(categoryName, ignoreCase = true) }
        if (removed) {
            saveExpenseCategories(current)
        }
        return removed
    }

    fun resetExpenseCategoriesToDefault() {
        saveExpenseCategories(DEFAULT_EXPENSE_CATEGORIES)
    }

    fun setCategoriesForMode(mode: String?) {
        val modeCategories = com.example.ui.util.BusinessModeFeatures.getExpenseCategoriesForMode(mode)
        saveExpenseCategories(modeCategories)
    }

    fun saveCustomExpenseCategories(categories: List<String>) {
        saveExpenseCategories(categories)
    }

    private fun saveExpenseCategories(categories: List<String>) {
        val serialized = categories.joinToString("|||")
        prefs.edit().putString(KEY_CUSTOM_EXPENSE_CATEGORIES, serialized).apply()
        _sessionState.value = loadSession()
    }

    fun setSecuritySettings(
        appLockEnabled: Boolean? = null,
        pin: String? = null,
        biometricEnabled: Boolean? = null,
        password: String? = null,
        autoLockMinutes: Int? = null
    ) {
        val editor = prefs.edit()
        appLockEnabled?.let { editor.putBoolean(KEY_APP_LOCK_ENABLED, it) }
        pin?.let { editor.putString(KEY_SECURITY_PIN, it.trim()) }
        biometricEnabled?.let { editor.putBoolean(KEY_BIOMETRIC_ENABLED, it) }
        password?.let { editor.putString(KEY_SECURITY_PASSWORD, it.trim()) }
        autoLockMinutes?.let { editor.putInt(KEY_AUTOLOCK_TIMEOUT_MINUTES, it) }
        editor.apply()
        _sessionState.value = loadSession().copy(isAppLocked = false)
    }

    fun setRegisteredPhone(phone: String) {
        prefs.edit().putString(KEY_REGISTERED_PHONE, phone.trim()).apply()
        _sessionState.value = loadSession()
    }

    fun lockApp() {
        val current = _sessionState.value
        if (current.appLockEnabled && (current.securityPin.isNotBlank() || current.securityPassword.isNotBlank() || current.biometricEnabled)) {
            _sessionState.value = current.copy(isAppLocked = true)
        }
    }

    fun unlockApp() {
        _sessionState.value = _sessionState.value.copy(isAppLocked = false)
    }

    fun verifyPin(enteredPin: String): Boolean {
        val currentPin = _sessionState.value.securityPin
        return currentPin.isNotBlank() && currentPin == enteredPin.trim()
    }

    fun verifyPassword(enteredPassword: String): Boolean {
        val currentPass = _sessionState.value.securityPassword
        return currentPass.isNotBlank() && currentPass == enteredPassword.trim()
    }

    fun logout() {
        // Clear active session but preserve language & appearance
        val currentLang = prefs.getString(KEY_LANG, "en") ?: "en"
        val themeMode = prefs.getString(KEY_THEME_MODE, "SYSTEM") ?: "SYSTEM"
        val accent = prefs.getString(KEY_ACCENT_THEME, "BLUE") ?: "BLUE"
        prefs.edit().clear()
            .putString(KEY_LANG, currentLang)
            .putString(KEY_THEME_MODE, themeMode)
            .putString(KEY_ACCENT_THEME, accent)
            .apply()
        _sessionState.value = UserSession(languageCode = currentLang, themeMode = themeMode, accentTheme = accent)
    }

    companion object {
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_BUSINESS_ID = "business_id"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_ROLE = "role"
        private const val KEY_STAFF_ID = "staff_id"
        private const val KEY_STAFF_NAME = "staff_name"
        private const val KEY_PERMISSIONS = "permissions"
        private const val KEY_ACCOUNT_START_DATE = "account_start_date"
        private const val KEY_LANG = "language_code"
        private const val KEY_UPI_ID = "upi_id"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_ACCENT_THEME = "accent_theme"
        private const val KEY_CONFIRM_DELETE = "confirm_delete"
        private const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
        private const val KEY_MORNING_REMINDER_ENABLED = "morning_reminder_enabled"
        private const val KEY_MORNING_REMINDER_TIME = "morning_reminder_time"
        private const val KEY_AFTERNOON_REMINDER_ENABLED = "afternoon_reminder_enabled"
        private const val KEY_AFTERNOON_REMINDER_TIME = "afternoon_reminder_time"
        private const val KEY_EVENING_REMINDER_ENABLED = "evening_reminder_enabled"
        private const val KEY_EVENING_REMINDER_TIME = "evening_reminder_time"
        private const val KEY_ADVANCE_ALERT_MINUTES = "advance_alert_minutes"
        private const val KEY_MILKING_REMINDER_ENABLED = "milking_reminder_enabled"
        private const val KEY_DELIVERY_REMINDER_ENABLED = "delivery_reminder_enabled"
        private const val KEY_VACCINE_ALERT_ENABLED = "vaccine_alert_enabled"
        private const val KEY_DEWORMING_ALERT_ENABLED = "deworming_alert_enabled"
        private const val KEY_BREEDING_CALVING_ALERT_ENABLED = "breeding_calving_alert_enabled"
        private const val KEY_PAYMENT_DUE_ALERTS_ENABLED = "payment_due_alerts_enabled"
        private const val KEY_PAYMENT_DUE_THRESHOLD = "payment_due_threshold"
        private const val KEY_LOW_STOCK_ALERTS_ENABLED = "low_stock_alerts_enabled"
        private const val KEY_STOCK_ALERT_THRESHOLD_DAYS = "stock_alert_threshold_days"
        private const val KEY_SOUND_ALERTS_ENABLED = "sound_alerts_enabled"
        private const val KEY_VIBRATION_ALERTS_ENABLED = "vibration_alerts_enabled"
        private const val KEY_DAILY_SUMMARY_ALERTS_ENABLED = "daily_summary_alerts_enabled"
        private const val KEY_DAILY_SUMMARY_TIME = "daily_summary_time"
        private const val KEY_BILL_NOTE = "bill_note"
        private const val KEY_CUSTOM_EXPENSE_CATEGORIES = "custom_expense_categories"
        private const val KEY_APP_LOCK_ENABLED = "app_lock_enabled"
        private const val KEY_SECURITY_PIN = "security_pin"
        private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
        private const val KEY_SECURITY_PASSWORD = "security_password"
        private const val KEY_AUTOLOCK_TIMEOUT_MINUTES = "autolock_timeout_minutes"
        private const val KEY_REGISTERED_PHONE = "registered_phone"
    }
}
