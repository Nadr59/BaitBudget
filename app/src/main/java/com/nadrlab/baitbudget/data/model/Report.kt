package com.nadrlab.baitbudget.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reports")
data class Report(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userName: String,
    val reportText: String,
    val date: Long = System.currentTimeMillis(),
    val purchaseCount: Int = 0,
    val paymentCount: Int = 0,
    val totalPurchases: Double = 0.0,
    val totalPayments: Double = 0.0,
    val debt: Double = 0.0,
    val transactionCount: Int = 0,
    val isRead: Boolean = false
)
