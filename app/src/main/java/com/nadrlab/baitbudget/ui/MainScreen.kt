package com.nadrlab.baitbudget.ui

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nadrlab.baitbudget.viewmodel.BudgetViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: BudgetViewModel) {
    val stores by viewModel.allStores.collectAsState()
    val storesWithDebt by viewModel.storesWithDebt.collectAsState()
    val allTimePurchases by viewModel.allTimePurchases.collectAsState()
    val allTimePayments by viewModel.allTimePayments.collectAsState()
    val monthPurchases by viewModel.monthPurchases.collectAsState()
    val monthPayments by viewModel.monthPayments.collectAsState()
    val weekPurchases by viewModel.weekPurchases.collectAsState()
    val weekPayments by viewModel.weekPayments.collectAsState()
    val isAdmin by viewModel.isAdmin.collectAsState()
    val userName by viewModel.userName.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    var showAddPurchase by remember { mutableStateOf(false) }
    var showAddPayment by remember { mutableStateOf(false) }
    var showQuickSummary by remember { mutableStateOf(false) }
    var showChangePassword by remember { mutableStateOf(false) }
    val message by viewModel.message.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onMessageShown()
        }
    }

    val totalDebt = storesWithDebt.sumOf { it.debt }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar(containerColor = Color(0xFF1A1A1A), contentColor = Color.White) {
                NavigationBarItem(
                    selected = selectedTab == 0, onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Home, null) },
                    label = { Text("الرئيسية", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF4CAF50), selectedTextColor = Color(0xFF4CAF50),
                        unselectedIconColor = Color.Gray, unselectedTextColor = Color.Gray,
                        indicatorColor = Color(0xFF1A3A1A)
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 1, onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Receipt, null) },
                    label = { Text("المعاملات", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF4CAF50), selectedTextColor = Color(0xFF4CAF50),
                        unselectedIconColor = Color.Gray, unselectedTextColor = Color.Gray,
                        indicatorColor = Color(0xFF1A3A1A)
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 2, onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Store, null) },
                    label = { Text("البقالات", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF4CAF50), selectedTextColor = Color(0xFF4CAF50),
                        unselectedIconColor = Color.Gray, unselectedTextColor = Color.Gray,
                        indicatorColor = Color(0xFF1A3A1A)
                    )
                )
                if (isAdmin) {
                    NavigationBarItem(
                        selected = selectedTab == 3, onClick = { selectedTab = 3 },
                        icon = { Icon(Icons.Default.Assessment, null) },
                        label = { Text("التقارير", fontSize = 10.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF4CAF50), selectedTextColor = Color(0xFF4CAF50),
                            unselectedIconColor = Color.Gray, unselectedTextColor = Color.Gray,
                            indicatorColor = Color(0xFF1A3A1A)
                        )
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (selectedTab) {
                0 -> HomeTab(
                    viewModel = viewModel, storesWithDebt = storesWithDebt, totalDebt = totalDebt,
                    weekPurchases = weekPurchases, weekPayments = weekPayments,
                    monthPurchases = monthPurchases, monthPayments = monthPayments,
                    isAdmin = isAdmin, userName = userName,
                    onAddPurchase = { showAddPurchase = true },
                    onAddPayment = { showAddPayment = true },
                    onShowSummary = { showQuickSummary = true },
                    onChangePassword = { showChangePassword = true },
                    onLogout = { viewModel.logout() }
                )
                1 -> TransactionsTab(viewModel = viewModel, isAdmin = isAdmin,
                    onAddPurchase = { showAddPurchase = true }, onAddPayment = { showAddPayment = true })
                2 -> StoresTab(viewModel = viewModel, isAdmin = isAdmin)
                3 -> if (isAdmin) ReportsTab(viewModel = viewModel)
            }
        }
    }

    if (showAddPurchase) {
        AddTransactionDialog(
            title = "تسجيل شراء", titleColor = Color(0xFFF44336), stores = stores,
            onDismiss = { showAddPurchase = false },
            onConfirm = { storeId, amount, desc, note ->
                viewModel.addPurchase(storeId, amount, desc, note)
                showAddPurchase = false
            }
        )
    }

    if (showAddPayment) {
        AddTransactionDialog(
            title = "تسجيل دفع", titleColor = Color(0xFF4CAF50), stores = stores,
            onDismiss = { showAddPayment = false },
            onConfirm = { storeId, amount, desc, note ->
                viewModel.addPayment(storeId, amount, desc, note)
                showAddPayment = false
            }
        )
    }

    if (showQuickSummary && isAdmin) {
        QuickSummaryDialog(
            viewModel = viewModel, storesWithDebt = storesWithDebt,
            weekPurchases = weekPurchases, weekPayments = weekPayments,
            monthPurchases = monthPurchases, monthPayments = monthPayments,
            onDismiss = { showQuickSummary = false }
        )
    }

    if (showChangePassword && isAdmin) {
        ChangePasswordDialog(
            onDismiss = { showChangePassword = false },
            onConfirm = { oldPass, newPass ->
                if (viewModel.changeAdminPassword(oldPass, newPass)) {
                    scope.launch { snackbarHostState.showSnackbar("تم تغيير كلمة المرور") }
                } else {
                    scope.launch { snackbarHostState.showSnackbar("كلمة المرور القديمة خاطئة") }
                }
                showChangePassword = false
            }
        )
    }
}

// ═══════════════════════════════════════════
// تبويب الرئيسية مع الواتساب
// ═══════════════════════════════════════════
@Composable
fun HomeTab(
    viewModel: BudgetViewModel,
    storesWithDebt: List<BudgetViewModel.StoreWithDebt>,
    totalDebt: Double,
    weekPurchases: Double, weekPayments: Double,
    monthPurchases: Double, monthPayments: Double,
    isAdmin: Boolean, userName: String,
    onAddPurchase: () -> Unit, onAddPayment: () -> Unit,
    onShowSummary: () -> Unit, onChangePassword: () -> Unit,
    onLogout: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var isExporting by remember { mutableStateOf(false) }
    var isImporting by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D0D))
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // ═══ العنوان ═══
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("ميزانية البيت", fontSize = 26.sp, color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (isAdmin) "مشرف" else userName,
                        color = if (isAdmin) Color(0xFFE8C547) else Color(0xFF4CAF50),
                        fontSize = 12.sp, fontWeight = FontWeight.Bold
                    )
                    Text("  •  ", color = Color.Gray, fontSize = 12.sp)
                    Text(
                        SimpleDateFormat("EEEE، d MMMM", Locale("ar")).format(Date()),
                        color = Color.Gray, fontSize = 12.sp
                    )
                }
            }
            Row {
                if (isAdmin) {
                    IconButton(onClick = onShowSummary) {
                        Icon(Icons.Default.Summarize, "ملخص", tint = Color(0xFFE8C547))
                    }
                    IconButton(onClick = onChangePassword) {
                        Icon(Icons.Default.Lock, "كلمة المرور", tint = Color.Gray)
                    }
                }
                IconButton(onClick = onLogout) {
                    Icon(Icons.Default.ExitToApp, "خروج", tint = Color.Gray)
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // ═══ بطاقة المديونية ═══
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (totalDebt > 0) Color(0xFF3A1A1A) else Color(0xFF1A3A1A)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    if (totalDebt > 0) Icons.Default.Warning else Icons.Default.CheckCircle,
                    null,
                    tint = if (totalDebt > 0) Color(0xFFF44336) else Color(0xFF4CAF50),
                    modifier = Modifier.size(36.dp)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    if (totalDebt > 0) "المديونية الكلية" else if (totalDebt < 0) "رصيد لك" else "كل الحسابات مسدّدة",
                    color = Color.White, fontSize = 14.sp
                )
                Text(
                    viewModel.formatAmount(kotlin.math.abs(totalDebt)),
                    color = if (totalDebt > 0) Color(0xFFF44336) else if (totalDebt < 0) Color(0xFF4CAF50) else Color.Gray,
                    fontSize = 36.sp, fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // ═══ أزرار شراء/دفع ═══
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onAddPurchase, modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336)),
                shape = RoundedCornerShape(12.dp), contentPadding = PaddingValues(vertical = 14.dp)
            ) {
                Icon(Icons.Default.AddShoppingCart, null, tint = Color.White)
                Spacer(Modifier.width(6.dp))
                Text("شراء", color = Color.White, fontSize = 15.sp)
            }
            Button(
                onClick = onAddPayment, modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                shape = RoundedCornerShape(12.dp), contentPadding = PaddingValues(vertical = 14.dp)
            ) {
                Icon(Icons.Default.Payment, null, tint = Color.White)
                Spacer(Modifier.width(6.dp))
                Text("دفع", color = Color.White, fontSize = 15.sp)
            }
        }

        Spacer(Modifier.height(12.dp))

        // ═══ زر التصدير وإرسال عبر الواتساب (للمستخدم العادي) ═══
        if (!isAdmin) {
            Button(
                onClick = {
                    isExporting = true
                    scope.launch {
                        try {
                            val message = viewModel.exportDataForSharing()
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, message)
                                putExtra(Intent.EXTRA_SUBJECT, "بيانات ميزانية البيت")
                            }
                            context.startActivity(Intent.createChooser(intent, "إرسال البيانات عبر"))
                        } catch (e: Exception) {
                            snackbarHostState.showSnackbar("خطأ: ${e.message}")
                        }
                        isExporting = false
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isExporting,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(vertical = 14.dp)
            ) {
                if (isExporting) {
                    CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Share, null, tint = Color.White)
                }
                Spacer(Modifier.width(8.dp))
                Text("📤 تصدير وإرسال عبر الواتساب", color = Color.White, fontSize = 14.sp)
            }

            Spacer(Modifier.height(8.dp))

            Text(
                "سيتم إرسال رسالة تحتوي على جميع بياناتك إلى المشرف",
                color = Color(0xFF666666),
                fontSize = 11.sp
            )

            Spacer(Modifier.height(12.dp))
        }

                        // ═══ زر الاستيراد (للمشرف) ═══
        if (isAdmin) {
            Button(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clipText = clipboard.primaryClip?.getItemAt(0)?.text?.toString() ?: ""

                    if (clipText.isBlank()) {
                        scope.launch { snackbarHostState.showSnackbar("الحافظة فاضية! انسخ رسالة الواتساب أولاً") }
                        return@Button
                    }

                    if (!clipText.contains("BB2::")) {
                        scope.launch { snackbarHostState.showSnackbar("لا توجد بيانات صالحة. انسخ الرسالة كاملة") }
                        return@Button
                    }

                    viewModel.importFromClipboard(clipText)
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(vertical = 14.dp)
            ) {
                Icon(Icons.Default.FileDownload, null, tint = Color.White)
                Spacer(Modifier.width(8.dp))
                Text("📥 استيراد من حافظة الواتساب", color = Color.White, fontSize = 14.sp)
            }

            Spacer(Modifier.height(8.dp))

            Text(
                "انسخ رسالة المستخدم من الواتساب ثم اضغط هنا",
                color = Color(0xFF666666),
                fontSize = 11.sp
            )

            Spacer(Modifier.height(12.dp))
        }
          

        // ═══ ملخص الأسبوع ═══
        Text("هذا الأسبوع", color = Color(0xFFE8C547), fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SummaryCard(Modifier.weight(1f), Icons.Default.ShoppingCart, "مشتريات", viewModel.formatAmount(weekPurchases), Color(0xFFF44336))
            SummaryCard(Modifier.weight(1f), Icons.Default.Payment, "مدفوعات", viewModel.formatAmount(weekPayments), Color(0xFF4CAF50))
            SummaryCard(Modifier.weight(1f), Icons.Default.TrendingDown, "الصافي", viewModel.formatAmount(weekPurchases - weekPayments), if (weekPurchases - weekPayments > 0) Color(0xFFFF9800) else Color(0xFF4CAF50))
        }

        Spacer(Modifier.height(16.dp))

        // ═══ ملخص الشهر ═══
        Text("هذا الشهر", color = Color(0xFFE8C547), fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SummaryCard(Modifier.weight(1f), Icons.Default.ShoppingCart, "مشتريات", viewModel.formatAmount(monthPurchases), Color(0xFFF44336))
            SummaryCard(Modifier.weight(1f), Icons.Default.Payment, "مدفوعات", viewModel.formatAmount(monthPayments), Color(0xFF4CAF50))
            SummaryCard(Modifier.weight(1f), Icons.Default.TrendingDown, "الصافي", viewModel.formatAmount(monthPurchases - monthPayments), if (monthPurchases - monthPayments > 0) Color(0xFFFF9800) else Color(0xFF4CAF50))
        }

        Spacer(Modifier.height(20.dp))

        // ═══ حسابات البقالات (للمشرف فقط) ═══
        if (isAdmin && storesWithDebt.any { it.debt != 0.0 }) {
            Text("حسابات البقالات", color = Color(0xFFE8C547), fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            for (item in storesWithDebt.filter { it.debt != 0.0 }) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (item.debt > 0) Color(0xFF2A1A1A) else Color(0xFF1A2A1A)
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Store, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(22.dp))
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(item.store.name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text(if (item.debt > 0) "لك ذمة" else "لك رصيد", color = Color.Gray, fontSize = 11.sp)
                            }
                        }
                        Text(
                            viewModel.formatAmount(kotlin.math.abs(item.debt)),
                            color = if (item.debt > 0) Color(0xFFF44336) else Color(0xFF4CAF50),
                            fontSize = 18.sp, fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        if (storesWithDebt.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.Store, null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("ابدأ بإضافة بقالة", color = Color.Gray, fontSize = 15.sp)
                }
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}

// ═══════════════════════════════════════════
// حوار تغيير كلمة المرور
// ═══════════════════════════════════════════
@Composable
fun ChangePasswordDialog(onDismiss: () -> Unit, onConfirm: (String, String) -> Unit) {
    var oldPass by remember { mutableStateOf("") }
    var newPass by remember { mutableStateOf("") }
    var confirmPass by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تغيير كلمة المرور", color = Color(0xFFE8C547)) },
        text = {
            Column {
                OutlinedTextField(
                    value = oldPass, onValueChange = { oldPass = it; error = "" },
                    label = { Text("كلمة المرور الحالية") }, modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFE8C547), unfocusedBorderColor = Color.Gray
                    )
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = newPass, onValueChange = { newPass = it; error = "" },
                    label = { Text("كلمة المرور الجديدة") }, modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFE8C547), unfocusedBorderColor = Color.Gray
                    )
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = confirmPass, onValueChange = { confirmPass = it; error = "" },
                    label = { Text("تأكيد كلمة المرور") }, modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFE8C547), unfocusedBorderColor = Color.Gray
                    )
                )
                if (error.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(error, color = Color.Red, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (newPass != confirmPass) error = "كلمتا المرور غير متطابقتين"
                else if (newPass.length < 4) error = "كلمة المرور قصيرة جداً"
                else onConfirm(oldPass, newPass)
            }) { Text("تغيير", color = Color(0xFFE8C547)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء", color = Color.Gray) }
        }
    )
}

// ═══════════════════════════════════════════
// ملخص سريع
// ═══════════════════════════════════════════
@Composable
fun QuickSummaryDialog(
    viewModel: BudgetViewModel,
    storesWithDebt: List<BudgetViewModel.StoreWithDebt>,
    weekPurchases: Double, weekPayments: Double,
    monthPurchases: Double, monthPayments: Double,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ملخص الحسابات", color = Color(0xFFE8C547), fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text("هذا الأسبوع:", color = Color(0xFFE8C547), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                SummaryRow("مشتريات", viewModel.formatAmount(weekPurchases), Color(0xFFF44336))
                SummaryRow("مدفوعات", viewModel.formatAmount(weekPayments), Color(0xFF4CAF50))
                SummaryRow("الصافي", viewModel.formatAmount(weekPurchases - weekPayments), Color(0xFFFF9800))
                Spacer(Modifier.height(12.dp))
                Divider(color = Color(0xFF333333))
                Spacer(Modifier.height(12.dp))
                Text("هذا الشهر:", color = Color(0xFFE8C547), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                SummaryRow("مشتريات", viewModel.formatAmount(monthPurchases), Color(0xFFF44336))
                SummaryRow("مدفوعات", viewModel.formatAmount(monthPayments), Color(0xFF4CAF50))
                SummaryRow("الصافي", viewModel.formatAmount(monthPurchases - monthPayments), Color(0xFFFF9800))
                Spacer(Modifier.height(12.dp))
                Divider(color = Color(0xFF333333))
                Spacer(Modifier.height(12.dp))
                Text("حسابات البقالات:", color = Color(0xFFE8C547), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                if (storesWithDebt.isEmpty()) {
                    Text("لا توجد بقالات", color = Color.Gray, fontSize = 12.sp)
                } else {
                    for (item in storesWithDebt) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(item.store.name, color = Color.White, fontSize = 13.sp)
                            Text(
                                if (item.debt > 0) "عليك ${viewModel.formatAmount(item.debt)}"
                                else if (item.debt < 0) "لك ${viewModel.formatAmount(kotlin.math.abs(item.debt))}"
                                else "مسدد",
                                color = if (item.debt > 0) Color(0xFFF44336) else if (item.debt < 0) Color(0xFF4CAF50) else Color.Gray,
                                fontSize = 13.sp, fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("إغلاق", color = Color(0xFFE8C547)) }
        }
    )
}

@Composable
fun SummaryRow(label: String, value: String, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color.Gray, fontSize = 13.sp)
        Text(value, color = color, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SummaryCard(modifier: Modifier = Modifier, icon: ImageVector, title: String, value: String, color: Color) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(4.dp))
            Text(title, color = Color.Gray, fontSize = 10.sp)
            Text(value, color = color, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
    }
}
