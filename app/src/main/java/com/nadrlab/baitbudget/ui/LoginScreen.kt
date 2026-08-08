package com.nadrlab.baitbudget.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nadrlab.baitbudget.viewmodel.BudgetViewModel

@Composable
fun LoginScreen(viewModel: BudgetViewModel) {
    var selectedMode by remember { mutableIntStateOf(0) } // 0=admin, 1=user
    var adminPassword by remember { mutableStateOf("") }
    var userName by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D0D))
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // ═══ الشعار ═══
        Text(
            "ميزانية البيت",
            fontSize = 32.sp,
            color = Color(0xFF4CAF50),
            fontWeight = FontWeight.Bold
        )
        Text(
            "نظام إدارة المشتريات والمدفوعات",
            color = Color.Gray,
            fontSize = 14.sp
        )

        Spacer(Modifier.height(40.dp))

        // ═══ اختيار نوع الدخول ═══
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { selectedMode = 0; error = "" },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = if (selectedMode == 0) Color(0xFFE8C547) else Color.Gray
                ),
                border = if (selectedMode == 0) ButtonDefaults.outlinedButtonBorder(enabled = true) else null,
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Lock, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("مشرف")
            }
            OutlinedButton(
                onClick = { selectedMode = 1; error = "" },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = if (selectedMode == 1) Color(0xFF4CAF50) else Color.Gray
                ),
                border = if (selectedMode == 1) ButtonDefaults.outlinedButtonBorder(enabled = true) else null,
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Person, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("مستخدم")
            }
        }

        Spacer(Modifier.height(24.dp))

        // ═══ نموذج الدخول ═══
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (selectedMode == 0) {
                    // ═══ دخول المشرف ═══
                    Text("دخول المشرف", color = Color(0xFFE8C547), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(16.dp))

                    OutlinedTextField(
                        value = adminPassword,
                        onValueChange = { adminPassword = it; error = "" },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("كلمة المرور") },
                        singleLine = true,
                        visualTransformation = if (showPassword) VisualTransformation.None
                        else PasswordVisualTransformation(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFFE8C547),
                            unfocusedBorderColor = Color.Gray,
                            cursorColor = Color(0xFFE8C547)
                        )
                    )

                    Spacer(Modifier.height(4.dp))
                    Text(
                        "كلمة المرور الافتراضية: 1234",
                        color = Color(0xFF666666),
                        fontSize = 11.sp,
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (adminPassword.isBlank()) {
                                error = "أدخل كلمة المرور"
                            } else if (!viewModel.adminLogin(adminPassword)) {
                                error = "كلمة المرور خاطئة"
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE8C547)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("دخول", color = Color.Black, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    // ═══ دخول المستخدم ═══
                    Text("دخول المستخدم", color = Color(0xFF4CAF50), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(16.dp))

                    OutlinedTextField(
                        value = userName,
                        onValueChange = { userName = it; error = "" },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("اسمك") },
                        placeholder = { Text("مثال: نادر", color = Color(0xFF444444)) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF4CAF50),
                            unfocusedBorderColor = Color.Gray,
                            cursorColor = Color(0xFF4CAF50)
                        )
                    )

                    Spacer(Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (userName.isBlank()) {
                                error = "أدخل اسمك"
                            } else {
                                viewModel.userLogin(userName.trim())
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("دخول", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // ═══ رسالة الخطأ ═══
                if (error.isNotBlank()) {
                    Spacer(Modifier.height(12.dp))
                    Text(error, color = Color(0xFFF44336), fontSize = 13.sp)
                }
            }
        }
    }
}
