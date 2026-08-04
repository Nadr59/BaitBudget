package com.nadrlab.baitbudget.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nadrlab.baitbudget.data.BudgetRepository
import com.nadrlab.baitbudget.data.db.AppDatabase
import com.nadrlab.baitbudget.data.model.Store
import com.nadrlab.baitbudget.data.model.Transaction
import com.nadrlab.baitbudget.data.model.TransactionType
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

class BudgetViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = BudgetRepository(db.storeDao(), db.transactionDao())

    // ═══ الحالات ═══
    val allStores = repository.getAllStores()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTransactions = repository.getAllTransactions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTimePurchases = repository.getAllTimePurchases()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val allTimePayments = repository.getAllTimePayments()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // ═══ الشهر الحالي ═══
    private val monthStart: Long
        get() {
            val cal = Calendar.getInstance()
            cal.set(Calendar.DAY_OF_MONTH, 1)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            return cal.timeInMillis
        }

    private val monthEnd: Long
        get() {
            val cal = Calendar.getInstance()
            cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
            cal.set(Calendar.HOUR_OF_DAY, 23)
            cal.set(Calendar.MINUTE, 59)
            cal.set(Calendar.SECOND, 59)
            cal.set(Calendar.MILLISECOND, 999)
            return cal.timeInMillis
        }

    val monthPurchases = repository.getTotalPurchasesByDateRange(monthStart, monthEnd)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val monthPayments = repository.getTotalPaymentsByDateRange(monthStart, monthEnd)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // ═══ الأسبوع الحالي ═══
    private val weekStart: Long
        get() {
            val cal = Calendar.getInstance()
            cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            return cal.timeInMillis
        }

    val weekPurchases = repository.getTotalPurchasesByDateRange(weekStart, System.currentTimeMillis())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val weekPayments = repository.getTotalPaymentsByDateRange(weekStart, System.currentTimeMillis())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // ═══ المتاجر مع الأرصدة ═══
    val storesWithDebt: StateFlow<List<StoreWithDebt>> = allStores.flatMapLatest { stores ->
        if (stores.isEmpty()) {
            flowOf(emptyList())
        } else {
            combine(stores.map { store ->
                combine(
                    repository.getTotalPurchases(store.id),
                    repository.getTotalPayments(store.id)
                ) { purchases, payments ->
                    StoreWithDebt(
                        store = store,
                        totalPurchases = purchases,
                        totalPayments = payments,
                        debt = purchases - payments
                    )
                }
            }) { it.toList() }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ═══ التنقل ═══
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    fun onMessageShown() {
        _message.value = null
    }

    // ═══ إضافة متجر ═══
    fun addStore(name: String, phone: String, address: String) {
        viewModelScope.launch {
            repository.insertStore(
                Store(name = name, phone = phone, address = address)
            )
            _message.value = "تم إضافة $name"
        }
    }

    fun deleteStore(store: Store) {
        viewModelScope.launch {
            repository.deleteStore(store)
            _message.value = "تم حذف ${store.name}"
        }
    }

    // ═══ إضافة معاملة ═══
    fun addPurchase(storeId: Long, amount: Double, description: String, note: String = "") {
        viewModelScope.launch {
            repository.insertTransaction(
                Transaction(
                    storeId = storeId,
                    amount = amount,
                    description = description,
                    type = TransactionType.PURCHASE,
                    note = note
                )
            )
            _message.value = "تم تسجيل الشراء: %.2f".format(amount)
        }
    }

    fun addPayment(storeId: Long, amount: Double, description: String, note: String = "") {
        viewModelScope.launch {
            repository.insertTransaction(
                Transaction(
                    storeId = storeId,
                    amount = amount,
                    description = description,
                    type = TransactionType.PAYMENT,
                    note = note
                )
            )
            _message.value = "تم تسجيل الدفع: %.2f".format(amount)
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
            _message.value = "تم حذف المعاملة"
        }
    }

    // ═══ تنسيق المبالغ ═══
    fun formatAmount(amount: Double): String {
        return if (amount == amount.toLong().toDouble()) {
            "%.0f".format(amount)
        } else {
            "%.2f".format(amount)
        }
    }

    data class StoreWithDebt(
        val store: Store,
        val totalPurchases: Double,
        val totalPayments: Double,
        val debt: Double
    )
}
