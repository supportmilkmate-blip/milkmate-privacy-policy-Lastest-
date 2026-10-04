# MilkMate — Production Android Dairy Management System

MilkMate is a production-grade, offline-first Android application designed for commercial dairy farms, milk delivery businesses, and milk collection centers.

## Architecture Overview

```
Android UI (Jetpack Compose M3)
      ↓
  ViewModel
      ↓
Repository Layer (Centralized Pricing & Business Logic)
      ↓
Room Local Database (SQLite offline operational layer)
      ↓
Sync Queue (Deterministic state machine: PENDING, SYNCING, SYNCED, FAILED)
      ↓
WorkManager / Background Worker
      ↓
Firebase (Cloud Firestore, Phone Auth, Security Rules)
```

## Key Modules & Features

### 1. Offline-First Synchronization & Data Safety
- Every write operation first persists locally in Room Database with an immediate reactive UI update via Kotlin `Flow`.
- Stable UUIDs for all documents (`id`, `businessId`, `createdAt`, `updatedAt`, `createdBy`, `updatedBy`, `version`, `deletedAt`, `syncStatus`).
- Soft-delete pattern (`deletedAt`) to prevent orphan states and conflict collisions across devices.
- Sync queue retries automatically when internet connectivity is re-established.

### 2. Multi-Business Data Isolation
- Enforced at the SQLite level (`WHERE businessId = :businessId AND deletedAt IS NULL`) and in `firestore.rules`.
- Businesses cannot view or mutate documents belonging to another business ID.

### 3. Centralized Rate Engine (`PricingEngine.kt`)
- Supports 7 distinct pricing calculation modes:
  1. **Direct Rate**: Customer-specific override or fixed dairy base rate.
  2. **FAT + SNF**: Two-axis differential pricing with configurable base FAT/SNF targets and fat/snf steps.
  3. **FAT Only**: Proportional pricing based on fat kg content.
  4. **SNF Only**: Proportional pricing based on solids-not-fat content.
  5. **Paneer Yield Formula**: Expected paneer yield per 100L `(0.9 * Fat + 0.35 * SNF)` factored by wholesale paneer market price minus processing allowances.
  6. **Khoa Yield Formula**: Total solids yield `(Fat + SNF) * 0.95` factored by wholesale khoa market price.
  7. **Custom Formulas**: Linear coefficient matrix.
- Centralized: used identically across Deliveries, Billing, Reports, Ledger statements, and Profit calculations.
- Rate history preservation: editing a new rate never mutates historical delivery records.

### 4. Subscription & Entitlement Architecture (`BillingManager.kt`)
- **Free Plan**:
  - Full access to ALL features (Deliveries, Orders, Payments, Expenses, Stock, Reports, FAT/SNF, Paneer/Khoa, Custom Formulas, Backup, Staff, Settings, Language localization).
  - Free limits strictly bound to creation quantities:
    - Individual customers: Max 25
    - Bulk buyers: Max 5
    - Suppliers: Max 5
- **Pro Plan (₹49/month or ₹499/year)**:
  - Unlimited Individual customers, Bulk buyers, and Suppliers.
- **Entitlement Rule**: If Pro expires, existing records remain 100% accessible. Creating additional records beyond the Free limit is blocked until Pro is restored.

### 5. Staff Accounts & Granular Permissions
- Owner creates staff members with Staff ID, Name, Mobile, Security PIN, Role, and assigned routes.
- Staff credentials persist permanently across app restarts, force stops, and device reboots.
- Staff login evaluates against local Room credentials and restricts access to permitted modules.

### 6. Real PDF & Excel / CSV Exports (`ExportManager.kt`)
- Generates native Android `PdfDocument` files with formatted tables, headers, and grand totals.
- Generates standard `.csv` spreadsheet files with comma separation and headers recognized by Microsoft Excel and Google Sheets.
- Shared directly via Android `FileProvider` (`content://` URIs).

### 7. Full Backup & Restore (`BackupManager.kt`)
- Exports complete encrypted JSON snapshot of business data.
- Validates backup integrity and business ID matching prior to restoration to prevent cross-business corruption.

## Firebase Setup Instructions

1. Register your package name `com.aistudio.milkmate.qxkpvr` in the [Firebase Console](https://console.firebase.google.com).
2. Download `google-services.json` and place it in the `app/` folder.
3. Deploy `firestore.rules` using Firebase CLI:
   ```bash
   firebase deploy --only firestore:rules
   ```
4. Enable Phone Authentication and Cloud Firestore in native mode.
