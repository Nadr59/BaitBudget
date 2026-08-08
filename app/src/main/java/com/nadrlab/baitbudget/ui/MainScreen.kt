package com.nadrlab.baitbudget.ui

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nadrlab.baitbudget.data.model.TransactionType
import com.nadrlab.baitbudget.viewmodel.BudgetViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: BudgetViewModel) {
    val stores by viewModel.allStores.collectAsState(initial = emptyList())
    val storesWithDebt by viewModel.storesWithDebt.collectAsState(initial = emptyList())
    val isAdmin by viewModel.isAdmin.collectAsState(initial = false)
    val userName by viewModel.userName.collectAsState(initial = "")

    var selectedTab by remember { mutableIntStateOf(0) }
    var showAddPurchase by remember { mutableStateOf(false) }
    var showAddPayment by remember { mutableStateOf(false) }
    var showQuickSummary by remember { mutableStateOf(false) }
    var showChangePassword by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var showReportViewer by remember { mutableStateOf(false) }
    val message by viewModel.message.collectAsState(initial = null)
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onMessageShown()
        }
    }

    val totalDebt = storesWithDebt.fold(0.0) { acc, item -> acc + item.debt }

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
                    viewModel = viewModel,
                    storesWithDebt = storesWithDebt,
                    totalDebt = totalDebt,
                    isAdmin = isAdmin,
                    userName = userName,
                    onAddPurchase = { showAddPurchase = true },
                    onAddPayment = { showAddPayment = true },
                    onShowSummary = { showQuickSummary = true },
                    onChangePassword = { showChangePassword = true },
                    onImport = { showImportDialog = true },
                    onReportViewer = { showReportViewer = true },
                    onLogout = { viewModel.logout() }
                )
                1 -> TransactionsTab(
                    viewModel = viewModel,
                    isAdmin = isAdmin,
                    onAddPurchase = { showAddPurchase = true },
                    onAddPayment = { showAddPayment = true }
                )
                2 -> StoresTab(viewModel = viewModel, isAdmin = isAdmin)
                3 -> if (isAdmin) ReportsTab(viewModel = viewModel)
            }
        }
    }

    if (showAddPurchase) {
        AddTransactionDialog(
            title = "تسجيل شراء",
            titleColor = Color(0xFFF44336),
            stores = stores,
            onDismiss = { showAddPurchase = false },
            onConfirm = { storeId, amount, desc, note ->
                viewModel.addPurchase(storeId, amount, desc, note)
                showAddPurchase = false
            }
        )
    }

    if (showAddPayment) {
        AddTransactionDialog(
            title = "تسجيل دفع",
            titleColor = Color(0xFF4CAF50),
            stores = stores,
            onDismiss = { showAddPayment = false },
            onConfirm = { storeId, amount, desc, note ->
                viewModel.addPayment(storeId, amount, desc, note)
                showAddPayment = false
            }
        )
    }

    if (showQuickSummary && isAdmin) {
        QuickSummaryDialog(
            viewModel = viewModel,
            storesWithDebt = storesWithDebt,
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

    if (showImportDialog && isAdmin) {
        ImportDialog(
            onDismiss = { showImportDialog = false },
            onImport = { text ->
                viewModel.importFromClipboard(text)
                showImportDialog = false
            }
        )
    }

    if (showReportViewer && isAdmin) {
        ReportViewerDialog(
            onDismiss = { showReportViewer = false }
        )
    }
}

// ═══════════════════════════════════════════
// تبويب الرئيسية
// ═══════════════════════════════════════════
@Composable
fun HomeTab(
    viewModel: BudgetViewModel,
    storesWithDebt: List<BudgetViewModel.StoreWithDebt>,
    totalDebt: Double,
    isAdmin: Boolean,
    userName: String,
    onAddPurchase: () -> Unit,
    onAddPayment: () -> Unit,
    onShowSummary: () -> Unit,
    onChangePassword: () -> Unit,
    onImport: () -> Unit,
    onReportViewer: () -> Unit,
    onLogout: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var isExporting by remember { mutableStateOf(false) }

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
                Text(
                    "ميزانية البيت",
                    fontSize = 26.sp,
                    color = Color(0xFF4CAF50),
                    fontWeight = FontWeight.Bold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (isAdmin) "مشرف" else userName,
                        color = if (isAdmin) Color(0xFFE8C547) else Color(0xFF4CAF50),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text("  •  ", color = Color.Gray, fontSize = 12.sp)
                    Text(
                        SimpleDateFormat("EEEE، d MMMM", Locale("ar")).format(Date()),
                        color = Color.Gray,
                        fontSize = 12.sp
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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
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
                    if (totalDebt > 0) "المديونية الكلية"
                    else if (totalDebt < 0) "رصيد لك"
                    else "كل الحسابات مسدّدة",
                    color = Color.White,
                    fontSize = 14.sp
                )
                Text(
                    viewModel.formatAmount(kotlin.math.abs(totalDebt)),
                    color = if (totalDebt > 0) Color(0xFFF44336)
                    else if (totalDebt < 0) Color(0xFF4CAF50)
                    else Color.Gray,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // ═══ أزرار شراء/دفع ═══
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onAddPurchase,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336)),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(vertical = 14.dp)
            ) {
                Icon(Icons.Default.AddShoppingCart, null, tint = Color.White)
                Spacer(Modifier.width(6.dp))
                Text("شراء", color = Color.White, fontSize = 15.sp)
            }
            Button(
                onClick = onAddPayment,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(vertical = 14.dp)
            ) {
                Icon(Icons.Default.Payment, null, tint = Color.White)
                Spacer(Modifier.width(6.dp))
                Text("دفع", color = Color.White, fontSize = 15.sp)
            }
        }

        Spacer(Modifier.height(12.dp))

        // ═══ زر التصدير (للمستخدم العادي) ═══
        if (!isAdmin) {
            Button(
                onClick = {
                    isExporting = true
                    scope.launch {
                        try {
                            val msg = viewModel.exportDataForSharing()
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, msg)
                            }
                            context.startActivity(Intent.createChooser(intent, "إرسال عبر"))
                        } catch (_: Exception) {}
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
                    CircularProgressIndicator(
                        Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
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

        // ═══ أزرار المشرف ═══
        if (isAdmin) {
            // ═══ زر الاستيراد ═══
            Button(
                onClick = onImport,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(vertical = 14.dp)
            ) {
                Icon(Icons.Default.FileDownload, null, tint = Color.White)
                Spacer(Modifier.width(8.dp))
                Text("📥 استيراد بيانات من مستخدم", color = Color.White, fontSize = 14.sp)
            }

            Spacer(Modifier.height(8.dp))

            Text(
                "الصق رسالة الواتساب المستلمة من المستخدم",
                color = Color(0xFF666666),
                fontSize = 11.sp
            )

            Spacer(Modifier.height(12.dp))

            // ═══ زر استلام التقرير ═══
            Button(
                onClick = onReportViewer,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9C27B0)),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(vertical = 14.dp)
            ) {
                Icon(Icons.Default.Description, null, tint = Color.White)
                Spacer(Modifier.width(8.dp))
                Text("📋 استلام تقرير من مستخدم", color = Color.White, fontSize = 14.sp)
            }

            Spacer(Modifier.height(8.dp))

            Text(
                "الصق تقرير المستخدم لعرضه بشكل منظم",
                color = Color(0xFF666666),
                fontSize = 11.sp
            )

            Spacer(Modifier.height(12.dp))
        }

        Spacer(Modifier.height(20.dp))

        // ═══ حسابات البقالات ═══
        if (storesWithDebt.any { it.debt != 0.0 }) {
            Text("حسابات البقالات", color = Color(0xFFE8C547), fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            for (item in storesWithDebt.filter { it.debt != 0.0 }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (item.debt > 0) Color(0xFF2A1A1A) else Color(0xFF1A2A1A)
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
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
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
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
// حوار الاستيراد
// ═══════════════════════════════════════════
@Composable
fun ImportDialog(
    onDismiss: () -> Unit,
    onImport: (String) -> Unit
) {
    var pastedText by remember { mutableStateOf("") }
    var hasData by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("استيراد بيانات", color = Color(0xFF2196F3), fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                Text("الصق رسالة الواتساب المستلمة:", color = Color.White, fontSize = 13.sp)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = pastedText,
                    onValueChange = { newText ->
                        pastedText = newText
                        hasData = newText.contains("BB2")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 100.dp),
                    placeholder = { Text("الصق الرسالة هنا...", color = Color(0xFF666666)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF2196F3),
                        unfocusedBorderColor = Color(0xFF444444),
                        cursorColor = Color(0xFF2196F3)
                    )
                )
                Spacer(Modifier.height(8.dp))
                when {
                    pastedText.isBlank() -> {
                        Text("الصق الرسالة من الواتساب هنا", color = Color(0xFF666666), fontSize = 11.sp)
                    }
                    hasData -> {
                        Text("تم العثور على بيانات - اضغط استيراد", color = Color(0xFF4CAF50), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    else -> {
                        Text("لم يتم التعرف على البيانات", color = Color(0xFFFF9800), fontSize = 11.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (pastedText.isNotBlank()) onImport(pastedText) },
                enabled = pastedText.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2196F3),
                    disabledContainerColor = Color(0xFF333333)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("استيراد", color = if (pastedText.isNotBlank()) Color.White else Color.Gray)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء", color = Color.Gray) }
        }
    )
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
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ملخص الحسابات", color = Color(0xFFE8C547), fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
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
                                color = if (item.debt > 0) Color(0xFFF44336)
                                else if (item.debt < 0) Color(0xFF4CAF50)
                                else Color.Gray,
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

// ═══════════════════════════════════════════
// حوار عرض تقرير المستخدم
// ═══════════════════════════════════════════
@Composable
fun ReportViewerDialog(
    onDismiss: () -> Unit
) {
    var pastedText by remember { mutableStateOf("") }
    var isParsed by remember { mutableStateOf(false) }
    var parsedReport by remember { mutableStateOf<ParsedReport?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Description, null, tint = Color(0xFF9C27B0))
                Spacer(Modifier.width(8.dp))
                Text("تقرير المستخدم", color = Color(0xFF9C27B0), fontWeight = FontWeight.Bold)
            }
        },
        text = {
            if (!isParsed) {
                Column {
                    Text("الصق تقرير المستخدم المستلم عبر الواتساب:", color = Color.White, fontSize = 13.sp)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = pastedText,
                        onValueChange = { pastedText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp),
                        placeholder = { Text("الصق التقرير هنا...", color = Color(0xFF666666)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF9C27B0),
                            unfocusedBorderColor = Color(0xFF444444),
                            cursorColor = Color(0xFF9C27B0)
                        )
                    )
                    Spacer(Modifier.height(8.dp))
                    if (pastedText.isNotBlank()) {
                        Text(
                            if (pastedText.contains("تقرير") || pastedText.contains("مشتريات"))
                                "تم التعرف على التقرير"
                            else "لم يتم التعرف على التقرير",
                            color = if (pastedText.contains("تقرير") || pastedText.contains("مشتريات"))
                                Color(0xFF4CAF50) else Color(0xFFFF9800),
                            fontSize = 11.sp
                        )
                    }
                }
            } else {
                val report = parsedReport
                if (report != null) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 450.dp)
                    ) {
                        // ═══ معلومات المستخدم ═══
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("معلومات المستخدم", color = Color(0xFF9C27B0), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.height(6.dp))
                                    ReportInfoRow("المستخدم:", report.userName)
                                    ReportInfoRow("التاريخ:", report.reportDate)
                                }
                            }
                        }

                        // ═══ الملخص ═══
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A2A1A)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("الملخص", color = Color(0xFF4CAF50), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.height(6.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                        ReportMiniStat("مشتريات", report.purchaseCount, Color(0xFFF44336))
                                        ReportMiniStat("مدفوعات", report.paymentCount, Color(0xFF4CAF50))
                                        ReportMiniStat("الكل", report.totalCount, Color(0xFF2196F3))
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    HorizontalDivider(color = Color(0xFF2A2A3E))
                                    Spacer(Modifier.height(8.dp))
                                    ReportInfoRow("اجمالي المشتريات:", report.totalPurchases)
                                    ReportInfoRow("اجمالي المدفوعات:", report.totalPayments)
                                    ReportInfoRow("المديونية:", report.debt, isHighlight = true)
                                }
                            }
                        }

                        // ═══ حسابات البقالات ═══
                        if (report.storeDebts.isNotEmpty()) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E)),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text("حسابات البقالات", color = Color(0xFFE8C547), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        Spacer(Modifier.height(6.dp))
                                        for (storeDebt in report.storeDebts) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(storeDebt.first, color = Color.White, fontSize = 12.sp)
                                                Text(storeDebt.second, color = Color(0xFFE8C547), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // ═══ جدول العمليات ═══
                        if (report.transactions.isNotEmpty()) {
                            item {
                                Text("جدول العمليات", color = Color(0xFFE8C547), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(6.dp))
                            }

                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFF2A2A3E), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("التاريخ", color = Color(0xFFE8C547), fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.5f))
                                    Text("النوع", color = Color(0xFFE8C547), fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.8f))
                                    Text("البقالة", color = Color(0xFFE8C547), fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                    Text("المبلغ", color = Color(0xFFE8C547), fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                                }
                            }

                            items(report.transactions) { tx ->
                                val idx = report.transactions.indexOf(tx)
                                val bgColor = if (idx % 2 == 0) Color(0xFF151520) else Color(0xFF1A1A2E)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(bgColor, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(tx.date, color = Color(0xFFAAAAAA), fontSize = 10.sp, modifier = Modifier.weight(1.5f))
                                    Text(
                                        tx.type,
                                        color = if (tx.type.contains("شراء")) Color(0xFFF44336) else Color(0xFF4CAF50),
                                        fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.8f)
                                    )
                                    Text(tx.store, color = Color.White, fontSize = 10.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(tx.amount, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                                }
                            }
                        }

                        // ═══ تنبيه التكرارات ═══
                        if (report.duplicates.isNotEmpty()) {
                            item {
                                Spacer(Modifier.height(8.dp))
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF3A2A1A)),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Warning, null, tint = Color(0xFFFF9800), modifier = Modifier.size(18.dp))
                                            Spacer(Modifier.width(6.dp))
                                            Text("تنبيه تكرار", color = Color(0xFFFF9800), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Spacer(Modifier.height(6.dp))
                                        for (dup in report.duplicates) {
                                            Text("- $dup", color = Color(0xFFFF9800), fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (!isParsed) {
                TextButton(
                    onClick = {
                        parsedReport = parseReport(pastedText)
                        isParsed = true
                    },
                    enabled = pastedText.isNotBlank()
                ) {
                    Text(
                        "عرض التقرير",
                        color = if (pastedText.isNotBlank()) Color(0xFF9C27B0) else Color.Gray,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                TextButton(onClick = { isParsed = false; pastedText = "" }) {
                    Text("تقرير آخر", color = Color.Gray)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إغلاق", color = Color.Gray)
            }
        }
    )
}

// ═══════════════════════════════════════════
// تحليل التقرير النصي
// ═══════════════════════════════════════════
data class ParsedReport(
    val userName: String,
    val reportDate: String,
    val purchaseCount: String,
    val paymentCount: String,
    val totalCount: String,
    val totalPurchases: String,
    val totalPayments: String,
    val debt: String,
    val storeDebts: List<Pair<String, String>>,
    val transactions: List<ParsedTransaction>,
    val duplicates: List<String>
)

data class ParsedTransaction(
    val date: String,
    val type: String,
    val store: String,
    val amount: String
)

fun parseReport(text: String): ParsedReport {
    val lines = text.lines().map { it.trim() }

    var userName = ""
    var reportDate = ""
    var purchaseCount = "0"
    var paymentCount = "0"
    var totalCount = "0"
    var totalPurchases = "0"
    var totalPayments = "0"
    var debt = "0"
    val storeDebts = mutableListOf<Pair<String, String>>()
    val transactions = mutableListOf<ParsedTransaction>()
    val duplicates = mutableListOf<String>()

    var section = ""

    for (line in lines) {
        when {
            line.startsWith("المستخدم:") -> {
                userName = line.substringAfter("المستخدم:").trim()
            }
            line.startsWith("التاريخ:") -> {
                reportDate = line.substringAfter("التاريخ:").trim()
            }
            line.startsWith("عدد المشتريات:") -> {
                purchaseCount = line.substringAfter(":").trim()
            }
            line.startsWith("عدد المدفوعات:") -> {
                paymentCount = line.substringAfter(":").trim()
            }
            line.startsWith("اجمالي العمليات:") || line.startsWith("إجمالي العمليات:") -> {
                totalCount = line.substringAfter(":").trim()
            }
            line.startsWith("اجمالي المشتريات:") || line.startsWith("إجمالي المشتريات:") -> {
                totalPurchases = line.substringAfter(":").trim()
            }
            line.startsWith("اجمالي المدفوعات:") || line.startsWith("إجمالي المدفوعات:") -> {
                totalPayments = line.substringAfter(":").trim()
            }
            line.startsWith("المديونية:") -> {
                debt = line.substringAfter(":").trim()
            }
            line.contains("حسابات البقالات") -> {
                section = "stores"
            }
            line.contains("جدول العمليات") -> {
                section = "transactions"
            }
            line.contains("تنبيه تكرار") -> {
                section = "duplicates"
            }
            section == "stores" && ":" in line && !line.startsWith("---") && !line.startsWith("===") && line.length < 60 -> {
                val parts = line.split(":", limit = 2)
                if (parts.size == 2 && parts[0].isNotBlank()) {
                    val name = parts[0].removePrefix("\uD83C\uDFEA").trim()
                    if (name.isNotBlank()) {
                        storeDebts.add(name to parts[1].trim())
                    }
                }
            }
            section == "transactions" && "|" in line && !line.startsWith("-") && !line.startsWith("التاريخ") && !line.startsWith("------") -> {
                val parts = line.split("|").map { it.trim() }
                if (parts.size >= 4 && parts[0].isNotBlank()) {
                    transactions.add(
                        ParsedTransaction(
                            date = parts[0],
                            type = parts[1],
                            store = parts[2],
                            amount = parts[3]
                        )
                    )
                }
            }
            section == "duplicates" && line.startsWith("-") && line.length > 3 -> {
                duplicates.add(line.removePrefix("-").trim())
            }
        }
    }

    return ParsedReport(
        userName = userName,
        reportDate = reportDate,
        purchaseCount = purchaseCount,
        paymentCount = paymentCount,
        totalCount = totalCount,
        totalPurchases = totalPurchases,
        totalPayments = totalPayments,
        debt = debt,
        storeDebts = storeDebts,
        transactions = transactions,
        duplicates = duplicates
    )
}

// ═══════════════════════════════════════════
// مكونات مساعدة للتقرير
// ═══════════════════════════════════════════
@Composable
fun ReportInfoRow(label: String, value: String, isHighlight: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color.Gray, fontSize = 12.sp)
        Text(
            value,
            color = if (isHighlight) Color(0xFFE8C547) else Color.White,
            fontSize = 12.sp,
            fontWeight = if (isHighlight) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
fun ReportMiniStat(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = color, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(label, color = Color.Gray, fontSize = 10.sp)
    }
}
