package com.nadrlab.baitbudget

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nadrlab.baitbudget.ui.LoginScreen
import com.nadrlab.baitbudget.ui.MainScreen
import com.nadrlab.baitbudget.viewmodel.BudgetViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val viewModel: BudgetViewModel = viewModel()
            val isLoggedIn by viewModel.isAdmin.collectAsState(initial = viewModel.userPrefs.isAdmin)

            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color(0xFF0D0D0D)
            ) {
                if (isLoggedIn) {
                    MainScreen(viewModel = viewModel)
                } else {
                    LoginScreen(viewModel = viewModel)
                }
            }
        }
    }
}
