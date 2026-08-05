package com.nadrlab.baitbudget.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nadrlab.baitbudget.data.BudgetRepository
import com.nadrlab.baitbudget.data.UserPrefs
import com.nadrlab.baitbudget.data.db.AppDatabase
import com.nadrlab.baitbudget.data.model.Store
import com.nadrlab.baitbudget.data.model.Transaction
import com.nadrlab.baitbudget.data.model.TransactionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

class BudgetViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = BudgetRepository(db.storeDao(), db.transactionDao())
    val userPrefs = UserPrefs(application)

    // ═══ حالة المصادقة ═══
    private val _isLoggedIn = MutableStateFlow(userPrefs.isLoggedIn)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn

    private val _isAdmin = MutableStateFlow(userPrefs.isAdmin)
    val isAdmin: StateFlow<Boolean> = _isAdmin

    private val _userName = MutableStateFlow(userPrefs.userName)
    val userName: StateFlow<String> = _userName

    // ═══ البيانات ═══
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
                    StoreWithDebt(store, purchases, payments, purchases - payments)
                }
            }) { it.toList() }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ═══ الرسائل ═══
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    fun onMessageShown() {
        _message.value = null
    }

    // ═══════════════════════════════
    // المصادقة
    // ═══════════════════════════════
    fun loginAsAdmin(password: String): Boolean {
        return if (userPrefs.checkAdminPassword(password)) {
            userPrefs.isAdmin = true
            userPrefs.isLoggedIn = true
            userPrefs.userName = "مشرف"
            _isAdmin.value = true
            _isLoggedIn.value = true
            _userName.value = "مشرف"
            true
        } else false
    }

    fun loginAsUser(name: String) {
        userPrefs.isAdmin = false
        userPrefs.isLoggedIn = true
        userPrefs.userName = name
        _isAdmin.value = false
        _isLoggedIn.value = true
        _userName.value = name
    }

    fun logout() {
        userPrefs.logout()
        _isLoggedIn.value = false
        _isAdmin.value = false
        _userName.value = ""
    }

    fun changeAdminPassword(oldPass: String, newPass: String): Boolean {
        return userPrefs.changeAdminPassword(oldPass, newPass)
    }

    // ═══════════════════════════════
    // المتاجر
    // ═══════════════════════════════
    fun addStore(name: String, phone: String, address: String) {
        viewModelScope.launch {
            repository.insertStore(Store(name = name, phone = phone, address = address))
            _message.value = "تم إضافة $name"
        }
    }

    fun deleteStore(store: Store) {
        viewModelScope.launch {
            repository.deleteStore(store)
            _message.value = "تم حذف ${store.name}"
        }
    }

    // ═══════════════════════════════
    // المعاملات
    // ═══════════════════════════════
    fun addPurchase(storeId: Long, amount: Double, description: String, note: String = "") {
        viewModelScope.launch {
            repository.insertTransaction(
                Transaction(storeId = storeId, amount = amount, description = description, type = TransactionType.PURCHASE, note = note)
            )
            _message.value = "تم تسجيل الشراء: ${formatAmount(amount)}"
        }
    }

    fun addPayment(storeId: Long, amount: Double, description: String, note: String = "") {
        viewModelScope.launch {
            repository.insertTransaction(
                Transaction(storeId = storeId, amount = amount, description = description, type = TransactionType.PAYMENT, note = note)
            )
            _message.value = "تم تسجيل الدفع: ${formatAmount(amount)}"
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
            _message.value = "تم حذف المعاملة"
        }
    }

    // ═══════════════════════════════
    // التصدير
    // ═══════════════════════════════
    suspend fun exportData(): String {
        val stores = allStores.first()
        val transactions = allTransactions.first()

        val json = JSONObject().apply {
            put("app", "BaitBudget")
            put("version", 1)
            put("exportDate", System.currentTimeMillis())
            put("userName", _userName.value)

            val storesArray = JSONArray()
            for (store in stores) {
                storesArray.put(JSONObject().apply {
                    put("name", store.name)
                    put("phone", store.phone)
                    put("address", store.address)
                })
            }
            put("stores", storesArray)

            val transArray = JSONArray()
            for (t in transactions) {
                val storeName = stores.find { it.id == t.storeId }?.name ?: ""
                transArray.put(JSONObject().apply {
                    put("storeName", storeName)
                    put("amount", t.amount)
                    put("description", t.description)
                    put("type", t.type.name)
                    put("date", t.date)
                    put("note", t.note)
                })
            }
            put("transactions", transArray)
        }

        return json.toString(2)
    }

    // ═══════════════════════════════
    // الاستيراد
    // ═══════════════════════════════
    suspend fun importData(jsonString: String): Result<Int> {
        return withContext(Dispatchers.IO) {
            try {
                val json = JSONObject(jsonString)
                val storesArray = json.getJSONArray("stores")
                val transArray = json.getJSONArray("transactions")

                val currentStores = allStores.first()
                val storeMap = mutableMapOf<String, Long>()

                for (i in 0 until storesArray.length()) {
                    val s = storesArray.getJSONObject(i)
                    val name = s.getString("name")
                    val phone = s.optString("phone", "")
                    val address = s.optString("address", "")

                    val existing = currentStores.find { it.name.equals(name, ignoreCase = true) }
                    val storeId = existing?.id ?: repository.insertStore(
                        Store(name = name, phone = phone, address = address)
                    )
                    storeMap[name] = storeId
                }

                var count = 0
                for (i in 0 until transArray.length()) {
                    val t = transArray.getJSONObject(i)
                    val storeName = t.getString("storeName")
                    val storeId = storeMap[storeName] ?: continue

                    repository.insertTransaction(Transaction(
                        storeId = storeId,
                        amount = t.getDouble("amount"),
                        description = t.optString("description", ""),
                        type = TransactionType.valueOf(t.getString("type")),
                        date = t.getLong("date"),
                        note = t.optString("note", "")
                    ))
                    count++
                }

                Result.success(count)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // ═══ تنسيق المبالغ ═══
    fun formatAmount(amount: Double): String {
        return if (amount == amount.toLong().toDouble()) "%.0f".format(amount) else "%.2f".format(amount)
    }

    data class StoreWithDebt(
        val store: Store,
        val totalPurchases: Double,
        val totalPayments: Double,
        val debt: Double
    )
}
