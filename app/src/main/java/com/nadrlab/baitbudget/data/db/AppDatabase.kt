package com.nadrlab.baitbudget.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.nadrlab.baitbudget.data.model.Report
import com.nadrlab.baitbudget.data.model.Store
import com.nadrlab.baitbudget.data.model.Transaction

@Database(
    entities = [Store::class, Transaction::class, Report::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun storeDao(): StoreDao
    abstract fun transactionDao(): TransactionDao
    abstract fun reportDao(): ReportDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS reports (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        userName TEXT NOT NULL,
                        reportText TEXT NOT NULL,
                        date INTEGER NOT NULL,
                        purchaseCount INTEGER NOT NULL DEFAULT 0,
                        paymentCount INTEGER NOT NULL DEFAULT 0,
                        totalPurchases REAL NOT NULL DEFAULT 0.0,
                        totalPayments REAL NOT NULL DEFAULT 0.0,
                        debt REAL NOT NULL DEFAULT 0.0,
                        transactionCount INTEGER NOT NULL DEFAULT 0,
                        isRead INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "baitbudget_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
