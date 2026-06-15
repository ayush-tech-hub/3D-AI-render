package com.qrbarcode.scanner.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.qrbarcode.scanner.ui.history.HistoryScreen
import com.qrbarcode.scanner.ui.history.HistoryViewModel
import com.qrbarcode.scanner.ui.scanner.ScannerScreen
import com.qrbarcode.scanner.ui.scanner.ScannerViewModel

sealed class Screen(val route: String) {
    object Scanner : Screen("scanner")
    object History : Screen("history")
}

@Composable
fun AppNavGraph(
    navController: NavHostController,
    scannerViewModel: ScannerViewModel,
    historyViewModel: HistoryViewModel
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Scanner.route
    ) {
        composable(Screen.Scanner.route) {
            ScannerScreen(
                viewModel = scannerViewModel,
                onNavigateToHistory = { navController.navigate(Screen.History.route) }
            )
        }
        composable(Screen.History.route) {
            HistoryScreen(
                viewModel = historyViewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
