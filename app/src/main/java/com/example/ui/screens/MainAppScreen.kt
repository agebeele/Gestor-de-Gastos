package com.example.ui.screens

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.ReceiptScanResult
import com.example.data.model.Transaction
import com.example.ui.components.BiometricLockOverlay
import com.example.ui.components.CurrencySelectorBottomSheet
import com.example.ui.components.ExportBottomSheet
import com.example.ui.components.FilterBottomSheet
import com.example.ui.components.ReceiptScannerBottomSheet
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.FinanceViewModel
import com.example.ui.viewmodel.ThemeMode
import kotlinx.coroutines.launch

enum class MainTab(val title: String) {
    HOME("Inicio"),
    TRANSACTIONS("Movimientos"),
    REPORTS("Reportes"),
    BUDGETS("Presupuestos"),
    SETTINGS("Ajustes")
}

@Composable
fun MainAppScreen(
    viewModel: FinanceViewModel = viewModel()
) {
    val themeMode by viewModel.themeMode.collectAsState()
    val isSystemDark = isSystemInDarkTheme()
    val isDarkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemDark
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }

    MyApplicationTheme(darkTheme = isDarkTheme) {
        val selectedMonthYear by viewModel.selectedMonthYear.collectAsState()
        val allCategories by viewModel.allCategories.collectAsState()
        val currentMonthTransactions by viewModel.currentMonthTransactions.collectAsState()
        val filteredTransactions by viewModel.filteredTransactions.collectAsState()
        val financialReport by viewModel.financialReport.collectAsState()
        val currentBudget by viewModel.currentMonthBudget.collectAsState()
        val securitySettings by viewModel.securitySettings.collectAsState()
        val syncStatus by viewModel.syncStatus.collectAsState()
        val filterCriteria by viewModel.filterCriteria.collectAsState()
        val isAppLocked by viewModel.isAppLocked.collectAsState()
        val isScanningReceipt by viewModel.isScanningReceipt.collectAsState()
        val scanResult by viewModel.scanResult.collectAsState()
        val userMessage by viewModel.userMessage.collectAsState()

        var currentTab by remember { mutableStateOf(MainTab.HOME) }
        var showAddEditScreen by remember { mutableStateOf(false) }
        var editingTransaction by remember { mutableStateOf<Transaction?>(null) }
        var activeScanResultForNewTx by remember { mutableStateOf<ReceiptScanResult?>(null) }

        var showCurrencySheet by remember { mutableStateOf(false) }
        var showScannerSheet by remember { mutableStateOf(false) }
        var showExportSheet by remember { mutableStateOf(false) }
        var showFilterSheet by remember { mutableStateOf(false) }

        val snackbarHostState = remember { SnackbarHostState() }
        val scope = rememberCoroutineScope()

        LaunchedEffect(userMessage) {
            userMessage?.let { msg ->
                scope.launch {
                    snackbarHostState.showSnackbar(msg)
                    viewModel.clearMessage()
                }
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            if (showAddEditScreen) {
                AddEditTransactionScreen(
                    initialTransaction = editingTransaction,
                    initialScanResult = activeScanResultForNewTx,
                    categories = allCategories,
                    activeCurrencyCode = securitySettings.activeCurrencyCode,
                    onSave = { tx ->
                        if (editingTransaction != null) {
                            viewModel.updateTransaction(tx)
                        } else {
                            viewModel.addTransaction(tx)
                        }
                        showAddEditScreen = false
                        editingTransaction = null
                        activeScanResultForNewTx = null
                    },
                    onDelete = { id ->
                        viewModel.deleteTransaction(id)
                        showAddEditScreen = false
                        editingTransaction = null
                    },
                    onCancel = {
                        showAddEditScreen = false
                        editingTransaction = null
                        activeScanResultForNewTx = null
                    }
                )
            } else {
                Scaffold(
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    bottomBar = {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ) {
                            NavigationBarItem(
                                selected = currentTab == MainTab.HOME,
                                onClick = { currentTab = MainTab.HOME },
                                icon = { Icon(Icons.Default.Home, contentDescription = "Inicio") },
                                label = { Text(MainTab.HOME.title, fontWeight = if (currentTab == MainTab.HOME) FontWeight.Bold else FontWeight.Normal) },
                                modifier = Modifier.testTag("tab_home")
                            )
                            NavigationBarItem(
                                selected = currentTab == MainTab.TRANSACTIONS,
                                onClick = { currentTab = MainTab.TRANSACTIONS },
                                icon = { Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = "Movimientos") },
                                label = { Text(MainTab.TRANSACTIONS.title, fontWeight = if (currentTab == MainTab.TRANSACTIONS) FontWeight.Bold else FontWeight.Normal) },
                                modifier = Modifier.testTag("tab_transactions")
                            )
                            NavigationBarItem(
                                selected = currentTab == MainTab.REPORTS,
                                onClick = { currentTab = MainTab.REPORTS },
                                icon = { Icon(Icons.Default.PieChart, contentDescription = "Reportes") },
                                label = { Text(MainTab.REPORTS.title, fontWeight = if (currentTab == MainTab.REPORTS) FontWeight.Bold else FontWeight.Normal) },
                                modifier = Modifier.testTag("tab_reports")
                            )
                            NavigationBarItem(
                                selected = currentTab == MainTab.BUDGETS,
                                onClick = { currentTab = MainTab.BUDGETS },
                                icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = "Presupuestos") },
                                label = { Text(MainTab.BUDGETS.title, fontWeight = if (currentTab == MainTab.BUDGETS) FontWeight.Bold else FontWeight.Normal) },
                                modifier = Modifier.testTag("tab_budgets")
                            )
                            NavigationBarItem(
                                selected = currentTab == MainTab.SETTINGS,
                                onClick = { currentTab = MainTab.SETTINGS },
                                icon = { Icon(Icons.Default.Settings, contentDescription = "Ajustes") },
                                label = { Text(MainTab.SETTINGS.title, fontWeight = if (currentTab == MainTab.SETTINGS) FontWeight.Bold else FontWeight.Normal) },
                                modifier = Modifier.testTag("tab_settings")
                            )
                        }
                    },
                    floatingActionButton = {
                        if (currentTab != MainTab.SETTINGS) {
                            FloatingActionButton(
                                onClick = {
                                    editingTransaction = null
                                    activeScanResultForNewTx = null
                                    showAddEditScreen = true
                                },
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.testTag("fab_add_transaction")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Nuevo Movimiento")
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        when (currentTab) {
                            MainTab.HOME -> {
                                HomeScreen(
                                    selectedMonthYear = selectedMonthYear,
                                    report = financialReport,
                                    budget = currentBudget,
                                    recentTransactions = currentMonthTransactions,
                                    categories = allCategories,
                                    activeCurrencyCode = securitySettings.activeCurrencyCode,
                                    syncStatus = syncStatus,
                                    onPrevMonth = { viewModel.changeMonth(-1) },
                                    onNextMonth = { viewModel.changeMonth(1) },
                                    onCurrencyClick = { showCurrencySheet = true },
                                    onScanReceiptClick = { showScannerSheet = true },
                                    onExportClick = { showExportSheet = true },
                                    onSearchClick = {
                                        currentTab = MainTab.TRANSACTIONS
                                        showFilterSheet = true
                                    },
                                    onAddTransactionClick = {
                                        editingTransaction = null
                                        activeScanResultForNewTx = null
                                        showAddEditScreen = true
                                    },
                                    onTransactionClick = { tx ->
                                        editingTransaction = tx
                                        showAddEditScreen = true
                                    },
                                    onViewAllTransactions = { currentTab = MainTab.TRANSACTIONS },
                                    onAdjustBudgetClick = { currentTab = MainTab.BUDGETS },
                                    onLockAppClick = { viewModel.lockApp() }
                                )
                            }
                            MainTab.TRANSACTIONS -> {
                                TransactionsScreen(
                                    transactions = filteredTransactions,
                                    categories = allCategories,
                                    activeCurrencyCode = securitySettings.activeCurrencyCode,
                                    filterCriteria = filterCriteria,
                                    onFilterChange = { viewModel.setFilterCriteria(it) },
                                    onOpenFilterSheet = { showFilterSheet = true },
                                    onTransactionClick = { tx ->
                                        editingTransaction = tx
                                        showAddEditScreen = true
                                    },
                                    onAddNewTransaction = {
                                        editingTransaction = null
                                        activeScanResultForNewTx = null
                                        showAddEditScreen = true
                                    }
                                )
                            }
                            MainTab.REPORTS -> {
                                ReportsScreen(
                                    report = financialReport,
                                    selectedMonthYear = selectedMonthYear,
                                    categories = allCategories,
                                    activeCurrencyCode = securitySettings.activeCurrencyCode,
                                    onPrevMonth = { viewModel.changeMonth(-1) },
                                    onNextMonth = { viewModel.changeMonth(1) },
                                    onExportClick = { showExportSheet = true },
                                    onTransactionClick = { tx ->
                                        editingTransaction = tx
                                        showAddEditScreen = true
                                    }
                                )
                            }
                            MainTab.BUDGETS -> {
                                BudgetsScreen(
                                    currentBudget = currentBudget,
                                    report = financialReport,
                                    selectedMonthYear = selectedMonthYear,
                                    categories = allCategories,
                                    activeCurrencyCode = securitySettings.activeCurrencyCode,
                                    onPrevMonth = { viewModel.changeMonth(-1) },
                                    onNextMonth = { viewModel.changeMonth(1) },
                                    onSaveBudget = { b -> viewModel.saveBudget(b) }
                                )
                            }
                            MainTab.SETTINGS -> {
                                SettingsScreen(
                                    securitySettings = securitySettings,
                                    syncStatus = syncStatus,
                                    themeMode = themeMode,
                                    activeCurrencyCode = securitySettings.activeCurrencyCode,
                                    onUpdateSecuritySettings = { viewModel.updateSecuritySettings(it) },
                                    onSetThemeMode = { viewModel.setThemeMode(it) },
                                    onCurrencyClick = { showCurrencySheet = true },
                                    onTriggerCloudSync = { viewModel.triggerManualCloudSync() }
                                )
                            }
                        }
                    }
                }
            }

            // --- BOTTOM SHEETS ---

            if (showCurrencySheet) {
                CurrencySelectorBottomSheet(
                    selectedCurrencyCode = securitySettings.activeCurrencyCode,
                    onCurrencySelected = { curr ->
                        viewModel.setActiveCurrency(curr.code)
                    },
                    onDismiss = { showCurrencySheet = false }
                )
            }

            if (showScannerSheet) {
                ReceiptScannerBottomSheet(
                    isScanning = isScanningReceipt,
                    scanResult = scanResult,
                    activeCurrencyCode = securitySettings.activeCurrencyCode,
                    onScanImage = { bitmap ->
                        viewModel.scanReceiptWithAi(bitmap)
                    },
                    onApplyResult = { result ->
                        activeScanResultForNewTx = result
                        editingTransaction = null
                        showAddEditScreen = true
                        viewModel.clearScanResult()
                    },
                    onDismiss = {
                        showScannerSheet = false
                        viewModel.clearScanResult()
                    }
                )
            }

            if (showExportSheet) {
                ExportBottomSheet(
                    transactions = currentMonthTransactions,
                    categories = allCategories,
                    monthYear = selectedMonthYear,
                    totalIncome = financialReport.totalIncome,
                    totalExpense = financialReport.totalExpense,
                    currencyCode = securitySettings.activeCurrencyCode,
                    onExportComplete = { msg ->
                        viewModel.showMessage(msg)
                    },
                    onDismiss = { showExportSheet = false }
                )
            }

            if (showFilterSheet) {
                FilterBottomSheet(
                    currentFilter = filterCriteria,
                    categories = allCategories,
                    onApplyFilter = { viewModel.setFilterCriteria(it) },
                    onResetFilter = { viewModel.resetFilters() },
                    onDismiss = { showFilterSheet = false }
                )
            }

            // --- BIOMETRIC SECURITY SHIELD ---
            BiometricLockOverlay(
                isLocked = isAppLocked,
                onUnlockSuccess = { viewModel.unlockApp() },
                pinCodeConfigured = securitySettings.pinCode
            )
        }
    }
}
