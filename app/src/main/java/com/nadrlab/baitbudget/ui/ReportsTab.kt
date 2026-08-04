package com.nadrlab.baitbudget.ui

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nadrlab.baitbudget.viewmodel.BudgetViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ReportsTab(viewModel: BudgetViewModel) {
    val allTimePurchases by viewModel.allTimePurchases.collectAsState()
    val allTimePayments by viewModel.allTimePayments.collectAsState()
    val monthPurchases by viewModel.monthPurchases.collectAsState()
    val monthPayments by viewModel.monthPayments.collectAsState()
    val storesWithDebt by viewModel.storesWithDebt.collectAsState()

    val totalDebt = storesWithDebt.sumOf { it.debt }.coerceAtLeast(0.0)
    val monthName = remember {
        SimpleDateFormat("MMMM yyyy", Locale("ar")).format(Date())
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D0D))
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            "التقارير",
            fontSize = 22.sp,
            color = Color(0xFF4CAF50),
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(16.dp))

        // ═══ ملخص الشهر ═══
        Text(
            "📊 $monthName",
            color = Color(0xFFE8C547),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ReportCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.ShoppingCart,
                title = "مشتريات الشهر",
                value = viewModel.formatAmount(monthPurchases),
                color = Color(0xFFF44336)
            )
            ReportCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Payment,
                title = "مدفوعات الشهر",
                value = viewModel.formatAmount(monthPayments),
                color = Color(0xFF4CAF50)
            )
        }

        Spacer(Modifier.height(8.dp))

        ReportCard(
            modifier = Modifier.fillMaxWidth(),
            icon = Icons.Default.TrendingDown,
            title = "صافي الشهر",
            value = viewModel.formatAmount(monthPurchases - monthPayments),
            color = if (monthPurchases - monthPayments > 0) Color(0xFFF44336) else Color(0xFF4CAF50)
        )

        Spacer(Modifier.height(24.dp))

        // ═══ ملخص شامل ═══
        Text(
            "📈 إجمالي",
            color = Color(0xFFE8C547),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ReportCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.ShoppingCart,
                title = "إجمالي المشتريات",
                value = viewModel.formatAmount(allTimePurchases),
                color = Color(0xFFF44336)
            )
            ReportCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Payment,
                title = "إجمالي المدفوعات",
                value = viewModel.formatAmount(allTimePayments),
                color = Color(0xFF4CAF50)
            )
        }

        Spacer(Modifier.height(8.dp))

        ReportCard(
            modifier = Modifier.fillMaxWidth(),
            icon = Icons.Default.Warning,
            title = "إجمالي المديونية الحالية",
            value = viewModel.formatAmount(totalDebt),
            color = if (totalDebt > 0) Color(0xFFF44336) else Color(0xFF4CAF50)
        )

        Spacer(Modifier.height(24.dp))

        // ═══ ملخص كل بقالة ═══
        Text(
            "🏪 ملخص البقالات",
            color = Color(0xFFE8C547),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(8.dp))

        if (storesWithDebt.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    "لا توجد بقالات مسجلة",
                    color = Color.Gray,
                    modifier = Modifier.padding(16.dp)
                )
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
                        Text(
                            item.store.name,
                            color = Color.White,
                            fontSize = 14.sp
                        )
                        Text(
                            if (item.debt > 0)
                                "عليك: ${viewModel.formatAmount(item.debt)}"
                            else if (item.debt < 0)
                                "لك: ${viewModel.formatAmount(kotlin.math.abs(item.debt))}"
                            else
                                "مسدد",
                            color = if (item.debt > 0) Color(0xFFF44336)
                            else if (item.debt < 0) Color(0xFF4CAF50)
                            else Color.Gray,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}

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
