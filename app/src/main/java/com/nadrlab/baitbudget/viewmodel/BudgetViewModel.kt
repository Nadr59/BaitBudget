package com.nadrlab.baitbudget.viewmodel

import android.app.Application
import android.util.Base64
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nadrlab.baitbudget.data.BudgetRepository
import com.nadrlab.baitbudget.data.UserPrefs
import com.nadrlab.baitbudget.data.db.AppDatabase
import com.nadrlab.baitbudget.data.model.Report
import com.nadrlab.baitbudget.data.model.Store
import com.nadrlab.baitbudget.data.model.Transaction
import com.nadrlab.baitbudget.data.model.TransactionType
import com.nadrlab.baitbudget.ui.ParsedReport
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class BudgetViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = BudgetRepository(db.storeDao(), db.transactionDao())
    private val reportDao = db.reportDao()
    val userPrefs = UserPrefs(application)

    // ═══ المستخدم والمشرف ═══
    private val _isAdmin = MutableStateFlow(userPrefs.isAdmin)
    val isAdmin: StateFlow<Boolean> = _isAdmin

    private val _userName = MutableStateFlow(userPrefs.userName)
    val userName: StateFlow<String> = _userName

    // ═══ البيانات الأساسية ═══
    val allStores = repository.getAllStores()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTransactions = repository.getAllTransactions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTimePurchases = repository.getAllTimePurchases()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val allTimePayments = repository.getAllTimePayments()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val storesWithDebt: StateFlow<List<StoreWithDebt>> = allStores.flatMapLatest { stores ->
        if (stores.isEmpty()) flowOf(emptyList())
        else combine(stores.map { store ->
            combine(
                repository.getTotalPurchases(store.id),
                repository.getTotalPayments(store.id)
            ) { purchases, payments ->
                StoreWithDebt(store, purchases, payments, purchases - payments)
            }
        }) { it.toList() }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userSummaries: StateFlow<List<UserSummary>> =
        allTransactions.combine(allStores) { transactions, stores ->
            transactions
                .groupBy { it.senderTag }
                .filter { it.key.isNotBlank() }
                .map { (senderTag, txs) ->
                    UserSummary(
                        senderTag = senderTag,
                        totalPurchases = txs.filter { it.type == TransactionType.PURCHASE }.sumOf { it.amount },
                        totalPayments = txs.filter { it.type == TransactionType.PAYMENT }.sumOf { it.amount },
                        transactionCount = txs.size
                    )
                }
                .sortedByDescending { it.totalPurchases - it.totalPayments }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ═══ التقارير المحفوظة ═══
    val savedReports = reportDao.getAllReports()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unreadReportCount = reportDao.getUnreadCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val reportUserNames = reportDao.getAllUserNames()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ═══ الرسائل ═══
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    fun onMessageShown() { _message.value = null }

    // ═══ تسجيل الدخول ═══
    fun adminLogin(password: String): Boolean {
        if (password == userPrefs.adminPassword) {
            userPrefs.isAdmin = true
            _isAdmin.value = true
            _userName.value = "مشرف"
            return true
        }
        return false
    }

    fun userLogin(name: String) {
        userPrefs.isAdmin = false
        userPrefs.userName = name
        _isAdmin.value = false
        _userName.value = name
    }

    fun changeAdminPassword(oldPass: String, newPass: String): Boolean {
        return if (oldPass == userPrefs.adminPassword) {
            userPrefs.adminPassword = newPass
            true
        } else false
    }

    fun logout() {
        userPrefs.clear()
        _isAdmin.value = false
        _userName.value = ""
    }

    // ═══ البقالات ═══
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

    // ═══ المعاملات ═══
    fun addPurchase(storeId: Long, amount: Double, description: String, note: String = "") {
        viewModelScope.launch {
            repository.insertTransaction(
                Transaction(
                    storeId = storeId,
                    amount = amount,
                    description = description,
                    type = TransactionType.PURCHASE,
                    note = note,
                    senderTag = _userName.value
                )
            )
            _message.value = "تم تسجيل الشراء: ${formatAmount(amount)}"
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
                    note = note,
                    senderTag = _userName.value
                )
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

    // ═══ التقارير ═══
    fun saveReport(parsedReport: ParsedReport, rawText: String) {
        viewModelScope.launch {
            reportDao.insertReport(
                Report(
                    userName = parsedReport.userName.ifBlank { "غير معروف" },
                    reportText = rawText,
                    date = System.currentTimeMillis(),
                    purchaseCount = parsedReport.purchaseCount.toIntOrNull() ?: 0,
                    paymentCount = parsedReport.paymentCount.toIntOrNull() ?: 0,
                    totalPurchases = parsedReport.totalPurchases.replace(Regex("[^0-9.]"), "").toDoubleOrNull() ?: 0.0,
                    totalPayments = parsedReport.totalPayments.replace(Regex("[^0-9.]"), "").toDoubleOrNull() ?: 0.0,
                    debt = parsedReport.debt.replace(Regex("[^0-9.\\-]"), "").toDoubleOrNull() ?: 0.0,
                    transactionCount = parsedReport.totalCount.toIntOrNull() ?: 0,
                    isRead = false
                )
            )
            _message.value = "تم حفظ تقرير ${parsedReport.userName.ifBlank { "مستخدم" }}"
        }
    }

    fun markReportRead(reportId: Long) {
        viewModelScope.launch {
            reportDao.markAsRead(reportId)
        }
    }

    fun deleteReport(report: Report) {
        viewModelScope.launch {
            reportDao.deleteReport(report)
            _message.value = "تم حذف التقرير"
        }
    }

    fun getReportsForUser(userName: String): Flow<List<Report>> {
        return reportDao.getReportsByUser(userName)
    }

    // ═══ الاستيراد ═══
    fun importFromClipboard(text: String) {
        viewModelScope.launch {
            try {
                val base64 = text.lines()
                    .find { it.trim().startsWith("BB2::") }
                    ?.trim()
                    ?.removePrefix("BB2::")

                if (base64.isNullOrBlank()) {
                    _message.value = "لم يتم العثور على بيانات صالحة"
                    return@launch
                }

                val jsonStr = String(
                    Base64.decode(base64, Base64.NO_WRAP or Base64.URL_SAFE),
                    Charsets.UTF_8
                )
                val json = JSONObject(jsonStr)

                val storesArray = json.getJSONArray("s")
                val storeNameToId = mutableMapOf<String, Long>()

                for (i in 0 until storesArray.length()) {
                    val s = storesArray.getJSONObject(i)
                    val name = s.getString("n")
                    val phone = s.optString("p", "")
                    val address = s.optString("a", "")

                    val existingStore = repository.getStoreByName(name)
                    val storeId = if (existingStore != null) {
                        existingStore.id
                    } else {
                        repository.insertStore(Store(name = name, phone = phone, address = address))
                    }
                    storeNameToId[name] = storeId
                }

                val txArray = json.getJSONArray("t")
                var importedCount = 0

                for (i in 0 until txArray.length()) {
                    val t = txArray.getJSONObject(i)
                    val storeName = t.getString("n")
                    val storeId = storeNameToId[storeName] ?: continue
                    val amount = t.getDouble("a")
                    val desc = t.optString("d", "")
                    val type = if (t.getString("t") == "P") TransactionType.PURCHASE else TransactionType.PAYMENT
                    val date = t.getLong("dt")
                    val note = t.optString("nt", "")

                    repository.insertTransaction(
                        Transaction(
                            storeId = storeId,
                            amount = amount,
                            description = desc,
                            type = type,
                            date = date,
                            note = note,
                            senderTag = json.optString("u", "مستخدم"),
                            exported = true
                        )
                    )
                    importedCount++
                }

                _message.value = "تم استيراد $importedCount معاملة بنجاح"

            } catch (e: Exception) {
                _message.value = "خطأ في الاستيراد: ${e.message}"
            }
        }
    }

    // ═══ التصدير ═══
    suspend fun exportDataForSharing(): String {
        val stores = db.storeDao().getAllStoresOnce()
        val transactions = db.transactionDao().getUnexportedTransactions()

        if (transactions.isEmpty()) {
            return "لا توجد معاملات جديدة للتصدير"
        }

        val json = JSONObject().apply {
            put("app", "BaitBudget")
            put("v", 1)
            put("d", System.currentTimeMillis())
            put("u", _userName.value)

            val sa = JSONArray()
            for (store in stores) {
                sa.put(JSONObject().apply {
                    put("n", store.name)
                    put("p", store.phone)
                    put("a", store.address)
                })
            }
            put("s", sa)

            val ta = JSONArray()
            for (t in transactions) {
                val storeName = stores.find { it.id == t.storeId }?.name ?: ""
                ta.put(JSONObject().apply {
                    put("n", storeName)
                    put("a", t.amount)
                    put("d", t.description)
                    put("t", if (t.type == TransactionType.PURCHASE) "P" else "Y")
                    put("dt", t.date)
                    put("nt", t.note)
                })
            }
            put("t", ta)
        }

        val base64 = Base64.encodeToString(
            json.toString().toByteArray(Charsets.UTF_8),
            Base64.NO_WRAP or Base64.URL_SAFE
        )

        db.transactionDao().markAllAsExported()

        val dateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale("ar"))
        val pCount = transactions.count { it.type == TransactionType.PURCHASE }
        val yCount = transactions.count { it.type == TransactionType.PAYMENT }

        return buildString {
            appendLine("📊 بيانات ميزانية البيت")
            appendLine("👤 المرسل: ${_userName.value}")
            appendLine("📅 ${dateFormat.format(Date())}")
            appendLine("🛒 مشتريات: $pCount | 💰 مدفوعات: $yCount")
            appendLine("────────────────")
            appendLine("BB2::$base64")
            appendLine("────────────────")
            appendLine("📥 انسخ هذه الرسالة واضغط استيراد")
        }
    }

    // ═══ أدوات ═══
    fun formatAmount(amount: Double): String {
        return if (amount == amount.toLong().toDouble()) "%.0f".format(amount) else "%.2f".format(amount)
    }

    fun normalizeNumbers(text: String): String {
        return text
            .replace('٠', '0').replace('١', '1').replace('٢', '2')
            .replace('٣', '3').replace('٤', '4').replace('٥', '5')
            .replace('٦', '6').replace('٧', '7').replace('٨', '8')
            .replace('٩', '9').replace('٫', '.')
    }

    // ═══ بيانات ═══
    data class StoreWithDebt(
        val store: Store,
        val totalPurchases: Double,
        val totalPayments: Double,
        val debt: Double
    )

    data class UserSummary(
        val senderTag: String,
        val totalPurchases: Double,
        val totalPayments: Double,
        val transactionCount: Int
    )
}
