package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.*
import com.example.data.local.entity.*

@Database(
    entities = [
        BusinessEntity::class,
        StaffEntity::class,
        StaffAttendanceEntity::class,
        StaffPaymentEntity::class,
        CustomerEntity::class,
        DeliveryEntity::class,
        PaymentEntity::class,
        ExpenseEntity::class,
        InventoryItemEntity::class,
        InventoryTransactionEntity::class,
        InventoryMonthClosingEntity::class,
        OrderEntity::class,
        SyncQueueEntity::class,
        FarmerEntity::class,
        MilkCollectionEntity::class,
        BulkDispatchEntity::class,
        CattleEntity::class,
        CattleWeightEntity::class,
        CattleCmtEntity::class,
        CattleBcsEntity::class,
        BreedingRecordEntity::class,
        DewormingRecordEntity::class,
        VaccinationRecordEntity::class,
        TreatmentRecordEntity::class,
        FarmObservationEntity::class,
        CattleMilkingRecordEntity::class,
        MilkWastageEntity::class
    ],
    version = 16,
    exportSchema = false
)
abstract class MilkMateDatabase : RoomDatabase() {
    abstract fun businessDao(): BusinessDao
    abstract fun staffDao(): StaffDao
    abstract fun customerDao(): CustomerDao
    abstract fun deliveryDao(): DeliveryDao
    abstract fun paymentDao(): PaymentDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun inventoryDao(): InventoryDao
    abstract fun orderDao(): OrderDao
    abstract fun syncQueueDao(): SyncQueueDao
    abstract fun farmerDao(): FarmerDao
    abstract fun milkCollectionDao(): MilkCollectionDao
    abstract fun bulkDispatchDao(): BulkDispatchDao
    abstract fun cattleDao(): CattleDao
    abstract fun milkWastageDao(): MilkWastageDao

    companion object {
        @Volatile
        private var INSTANCE: MilkMateDatabase? = null

        private val MIGRATION_14_15 = object : androidx.room.migration.Migration(14, 15) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE customers ADD COLUMN rateMethod TEXT NOT NULL DEFAULT 'FLAT'")
            }
        }

        fun getInstance(context: Context): MilkMateDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MilkMateDatabase::class.java,
                    "milkmate_production.db"
                )
                    .addMigrations(MIGRATION_14_15)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
