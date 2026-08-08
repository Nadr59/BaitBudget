package com.nadrlab.baitbudget.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nadrlab.baitbudget.data.model.Report
import com.nadrlab.baitbudget.viewmodel.BudgetViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ReportsTab(viewModel: BudgetViewModel) {
    val allTimePurchases by viewModel.allTimePurchases.collectAsState(initial = 0.0)
    val allTimePayments by viewModel.allTimePayments.collectAsState(initial = 0.0)
    val storesWithDebt by viewModel.storesWithDebt.collectAsState(initial = emptyList())
    val userSummaries by viewModel.userSummaries.collectAsState(initial = emptyList())
    val savedReports by viewModel.savedReports.collectAsState(initial = emptyList())
    val unreadCount by viewModel.unreadReportCount.collectAsState(initial = 0)
    val reportUserNames by viewModel.reportUserNames.collectAsState(initial = emptyList())

    var showReportsList by remember { mutableStateOf(false) }
    var selectedUserForReports by remember { mutableStateOf<String?>(null) }
    var viewReport by remember { mutableStateOf<Report?>(null) }

    val totalDebt = allTimePurchases - allTimePayments

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D0D))
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("التقارير", fontSize = 22.sp, color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))

        // ═══ ملخص شامل ═══
        Text("📈 إجمالي", color = Color(0xFFE8C547), fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ReportCard(Modifier.weight(1f), Icons.Default.ShoppingCart, "إجمالي المشتريات", viewModel.formatAmount(allTimePurchases), Color(0xFFF44336))
            ReportCard(Modifier.weight(1f), Icons.Default.Payment, "إجمالي المدفوعات", viewModel.formatAmount(allTimePayments), Color(0xFF4CAF50))
        }
        Spacer(Modifier.height(8.dp))
        ReportCard(
            Modifier.fillMaxWidth(),
            Icons.Default.Warning,
            "إجمالي المديونية",
            viewModel.formatAmount(kotlin.math.abs(totalDebt)),
            if (totalDebt > 0) Color(0xFFF44336) else Color(0xFF4CAF50)
        )

        Spacer(Modifier.height(24.dp))

        // ═══ التقارير المحفوظة ═══
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("📋 التقارير المستلمة", color = Color(0xFF9C27B0), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                if (unreadCount > 0) {
                    Spacer(Modifier.width(8.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF44336)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            "$unreadCount جديد",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        if (savedReports.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.Description, null, tint = Color.Gray, modifier = Modifier.size(40.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("لا توجد تقارير محفوظة", color = Color.Gray, fontSize = 14.sp)
                    Text("استخدم زر استلام التقرير في الرئيسية", color = Color(0xFF555555), fontSize = 11.sp)
                }
            }
        } else {
            // ═══ أزرار المستخدمين ═══
            if (reportUserNames.size > 1) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    UserFilterChip("الكل", selectedUserForReports == null) {
                        selectedUserForReports = null
                    }
                    for (name in reportUserNames) {
                        UserFilterChip(name, selectedUserForReports == name) {
                            selectedUserForReports = if (selectedUserForReports == name) null else name
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
            }

            // ═══ قائمة التقارير ═══
            val filteredReports = if (selectedUserForReports != null) {
                savedReports.filter { it.userName == selectedUserForReports }
            } else {
                savedReports
            }

            for (report in filteredReports) {
                val dateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale("ar"))
                val isRead = report.isRead

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable {
                            viewModel.markReportRead(report.id)
                            viewReport = report
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = if (!isRead) Color(0xFF2A1A2E) else Color(0xFF1A1A1A)
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (!isRead) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF9C27B0)),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        "  جديد  ",
                                        color = Color.White,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(vertical = 1.dp)
                                    )
                                }
                                Spacer(Modifier.width(8.dp))
                            }
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Person, null, tint = Color(0xFF9C27B0), modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        report.userName,
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    dateFormat.format(Date(report.date)),
                                    color = Color(0xFF666666),
                                    fontSize = 11.sp
                                )
                                Row {
                                    Text(
                                        "مشتريات: ${report.purchaseCount}",
                                        color = Color(0xFFF44336),
                                        fontSize = 10.sp
                                    )
                                    Text(" | ", color = Color(0xFF333333), fontSize = 10.sp)
                                    Text(
                                        "مدفوعات: ${report.paymentCount}",
                                        color = Color(0xFF4CAF50),
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                "مديونية",
                                color = Color(0xFF888888),
                                fontSize = 10.sp
                            )
                            Text(
                                viewModel.formatAmount(kotlin.math.abs(report.debt)),
                                color = if (report.debt > 0) Color(0xFFF44336) else Color(0xFF4CAF50),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(
                                onClick = { viewModel.deleteReport(report) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Delete, "حذف", tint = Color.Gray, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // ═══ ملخص المستخدمين ═══
        if (userSummaries.isNotEmpty()) {
            Text("👥 ملخص المستخدمين", color = Color(0xFFE8C547), fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))

            for (user in userSummaries) {
                val userDebt = user.totalPurchases - user.totalPayments
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (userDebt > 0) Color(0xFF2A1A1A) else if (userDebt < 0) Color(0xFF1A2A1A) else Color(0xFF1A1A1A)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Person, null, tint = Color(0xFF2196F3), modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(user.senderTag, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                            Text(
                                if (userDebt > 0) "عليه ${viewModel.formatAmount(userDebt)}"
                                else if (userDebt < 0) "له ${viewModel.formatAmount(kotlin.math.abs(userDebt))}"
                                else "مسدد",
                                color = if (userDebt > 0) Color(0xFFF44336) else if (userDebt < 0) Color(0xFF4CAF50) else Color.Gray,
                                fontSize = 14.sp, fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        HorizontalDivider(color = Color(0xFF333333))
                        Spacer(Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("مشتريات", color = Color.Gray, fontSize = 11.sp)
                                Text(viewModel.formatAmount(user.totalPurchases), color = Color(0xFFF44336), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("مدفوعات", color = Color.Gray, fontSize = 11.sp)
                                Text(viewModel.formatAmount(user.totalPayments), color = Color(0xFF4CAF50), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                            // زر عرض تقارير المستخدم
                            val userReportCount = savedReports.count { it.userName == user.senderTag }
                            if (userReportCount > 0) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("تقارير", color = Color.Gray, fontSize = 11.sp)
                                    Text("$userReportCount", color = Color(0xFF9C27B0), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("إجمالي المستخدمين", color = Color.Gray, fontSize = 12.sp)
                    Text("${userSummaries.size}", color = Color(0xFF2196F3), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // ═══ ملخص البقالات ═══
        Text("🏪 ملخص البقالات", color = Color(0xFFE8C547), fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))

        if (storesWithDebt.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("لا توجد بقالات مسجلة", color = Color.Gray, modifier = Modifier.padding(16.dp))
            }
        } else {
            for (item in storesWithDebt) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (item.debt > 0) Color(0xFF2A1A1A) else Color(0xFF1A2A1A)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(item.store.name, color = Color.White, fontSize = 14.sp)
                        Text(
                            if (item.debt > 0) "عليك: ${viewModel.formatAmount(item.debt)}"
                            else if (item.debt < 0) "لك: ${viewModel.formatAmount(kotlin.math.abs(item.debt))}"
                            else "مسدد",
                            color = if (item.debt > 0) Color(0xFFF44336) else if (item.debt < 0) Color(0xFF4CAF50) else Color.Gray,
                            fontSize = 13.sp, fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(32.dp))
    }

    // ═══ حوار عرض تقرير محفوظ ═══
    viewReport?.let { report ->
        SavedReportViewerDialog(
            report = report,
            viewModel = viewModel,
            onDismiss = { viewReport = null }
        )
    }
}

// ═══════════════════════════════════════════
// حوار عرض تقرير محفوظ
// ═══════════════════════════════════════════
@Composable
fun SavedReportViewerDialog(
    report: Report,
    viewModel: BudgetViewModel,
    onDismiss: () -> Unit
) {
    val dateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale("ar"))

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Description, null, tint = Color(0xFF9C27B0))
                Spacer(Modifier.width(8.dp))
                Column {
                    Text("تقرير ${report.userName}", color = Color(0xFF9C27B0), fontWeight = FontWeight.Bold)
                    Text(dateFormat.format(Date(report.date)), color = Color(0xFF666666), fontSize = 11.sp)
                }
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
            ) {
                // ═══ ملخص ═══
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
                                ReportMiniStat("مشتريات", "${report.purchaseCount}", Color(0xFFF44336))
                                ReportMiniStat("مدفوعات", "${report.paymentCount}", Color(0xFF4CAF50))
                                ReportMiniStat("الكل", "${report.transactionCount}", Color(0xFF2196F3))
                            }
                            Spacer(Modifier.height(8.dp))
                            HorizontalDivider(color = Color(0xFF2A2A3E))
                            Spacer(Modifier.height(8.dp))
                            ReportInfoRow("اجمالي المشتريات:", viewModel.formatAmount(report.totalPurchases))
                            ReportInfoRow("اجمالي المدفوعات:", viewModel.formatAmount(report.totalPayments))
                            ReportInfoRow("المديونية:", viewModel.formatAmount(kotlin.math.abs(report.debt)), isHighlight = true)
                        }
                    }
                }

                // ═══ النص الكامل ═══
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("النص الكامل", color = Color(0xFFE8C547), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                report.reportText,
                                color = Color(0xFFCCCCCC),
                                fontSize = 11.sp,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("إغلاق", color = Color(0xFF9C27B0))
            }
        }
    )
}

// ═══════════════════════════════════════════
// مكون فلتر المستخدمين
// ═══════════════════════════════════════════
@Composable
fun UserFilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier.clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (selected) Color(0xFF9C27B0).copy(alpha = 0.3f) else Color(0xFF1A1A1A)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            label,
            color = if (selected) Color(0xFFCE93D8) else Color.Gray,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

// ═══════════════════════════════════════════
// مكونات مشتركة
// ═══════════════════════════════════════════
@Composable
fun ReportCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    value: String,
    color: Color
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(28.dp))
            Spacer(Modifier.height(6.dp))
            Text(title, color = Color.Gray, fontSize = 11.sp)
            Spacer(Modifier.height(4.dp))
            Text(value, color = color, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}
