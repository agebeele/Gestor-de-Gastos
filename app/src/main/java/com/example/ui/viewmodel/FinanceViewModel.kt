package com.example.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.GeminiReceiptScanner
import com.example.data.currency.CurrencyManager
import com.example.data.local.AppDatabase
import com.example.data.model.Category
import com.example.data.model.CategorySpending
import com.example.data.model.FinancialReport
import com.example.data.model.MonthlyBudget
import com.example.data.model.PaymentMethod
import com.example.data.model.ReceiptScanResult
import com.example.data.model.SecuritySettings
import com.example.data.model.Transaction
import com.example.data.model.TransactionType
import com.example.data.repository.FinanceRepository
import com.example.data.sync.CloudSyncManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class ThemeMode {
    SYSTEM,
    DARK,
    LIGHT
}

data class FilterCriteria(
    val query: String = "",
    val type: TransactionType? = null,
    val categoryId: Long? = null,
    val paymentMethod: PaymentMethod? = null,
    val minAmount: Double? = null,
    val maxAmount: Double? = null,
    val startDate: Long? = null,
    val endDate: Long? = null
)

class FinanceViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: FinanceRepository
    private val calendar = Calendar.getInstance()

    // Current selected Month Year (e.g., "2026-08")
    private val _selectedMonthYear = MutableStateFlow(
        String.format(Locale.US, "%04d-%02d", calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1)
    )
    val selectedMonthYear: StateFlow<String> = _selectedMonthYear.asStateFlow()

    // Theme Mode
    private val _themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    // Search and Filters
    private val _filterCriteria = MutableStateFlow(FilterCriteria())
    val filterCriteria: StateFlow<FilterCriteria> = _filterCriteria.asStateFlow()

    // Security Lock state
    private val _isAppLocked = MutableStateFlow(false)
    val isAppLocked: StateFlow<Boolean> = _isAppLocked.asStateFlow()

    // Receipt Scanner State
    private val _isScanningReceipt = MutableStateFlow(false)
    val isScanningReceipt: StateFlow<Boolean> = _isScanningReceipt.asStateFlow()

    private val _scanResult = MutableStateFlow<ReceiptScanResult?>(null)
    val scanResult: StateFlow<ReceiptScanResult?> = _scanResult.asStateFlow()

    // In-app Snackbar / Toast message
    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    val allTransactions: StateFlow<List<Transaction>>
    val allCategories: StateFlow<List<Category>>
    val allBudgets: StateFlow<List<MonthlyBudget>>
    val securitySettings: StateFlow<SecuritySettings>
    val syncStatus: StateFlow<CloudSyncManager.SyncStatus>

    init {
        val database = AppDatabase.getDatabase(application, viewModelScope)
        repository = FinanceRepository(database.appDao())

        allTransactions = repository.allTransactions.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

        allCategories = repository.allCategories.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

        allBudgets = repository.allBudgets.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

        securitySettings = repository.securitySettings.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            SecuritySettings()
        )

        syncStatus = repository.syncStatus.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            CloudSyncManager.SyncStatus()
        )
    }

    // Transactions filtered for current month
    val currentMonthTransactions: StateFlow<List<Transaction>> = combine(
        allTransactions,
        _selectedMonthYear
    ) { transactions, monthYear ->
        val parts = monthYear.split("-")
        if (parts.size == 2) {
            val year = parts[0].toIntOrNull() ?: 2026
            val month = (parts[1].toIntOrNull() ?: 8) - 1
            val cal = Calendar.getInstance()
            cal.set(year, month, 1, 0, 0, 0)
            val startMillis = cal.timeInMillis
            cal.set(year, month, cal.getActualMaximum(Calendar.DAY_OF_MONTH), 23, 59, 59)
            val endMillis = cal.timeInMillis
            transactions.filter { it.date in startMillis..endMillis }
        } else {
            transactions
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Transactions with advanced search & filter applied
    val filteredTransactions: StateFlow<List<Transaction>> = combine(
        allTransactions,
        _filterCriteria
    ) { transactions, filter ->
        transactions.filter { tx ->
            val matchesQuery = filter.query.isBlank() ||
                tx.title.contains(filter.query, ignoreCase = true) ||
                tx.notes.contains(filter.query, ignoreCase = true) ||
                tx.tags.any { it.contains(filter.query, ignoreCase = true) }

            val matchesType = filter.type == null || tx.type == filter.type
            val matchesCategory = filter.categoryId == null || tx.categoryId == filter.categoryId
            val matchesPayment = filter.paymentMethod == null || tx.paymentMethod == filter.paymentMethod
            val matchesMin = filter.minAmount == null || tx.amount >= filter.minAmount
            val matchesMax = filter.maxAmount == null || tx.amount <= filter.maxAmount
            val matchesStartDate = filter.startDate == null || tx.date >= filter.startDate
            val matchesEndDate = filter.endDate == null || tx.date <= filter.endDate

            matchesQuery && matchesType && matchesCategory && matchesPayment && matchesMin && matchesMax && matchesStartDate && matchesEndDate
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Monthly Report Data
    val financialReport: StateFlow<FinancialReport> = combine(
        currentMonthTransactions,
        allCategories,
        allBudgets,
        _selectedMonthYear
    ) { transactions, categories, budgets, monthYear ->
        val catMap = categories.associateBy { it.id }
        var totalIncome = 0.0
        var totalExpense = 0.0
        val categoryExpenses = mutableMapOf<Long, Double>()
        val categoryCounts = mutableMapOf<Long, Int>()
        val dailyMap = mutableMapOf<Int, Double>()

        val cal = Calendar.getInstance()
        for (tx in transactions) {
            cal.timeInMillis = tx.date
            val day = cal.get(Calendar.DAY_OF_MONTH)
            if (tx.type == TransactionType.INCOME) {
                totalIncome += tx.amount
            } else {
                totalExpense += tx.amount
                categoryExpenses[tx.categoryId] = (categoryExpenses[tx.categoryId] ?: 0.0) + tx.amount
                categoryCounts[tx.categoryId] = (categoryCounts[tx.categoryId] ?: 0) + 1
                dailyMap[day] = (dailyMap[day] ?: 0.0) + tx.amount
            }
        }

        val netBalance = totalIncome - totalExpense
        val savingsRate = if (totalIncome > 0) ((totalIncome - totalExpense) / totalIncome) * 100.0 else 0.0

        val currentBudget = budgets.find { it.monthYear == monthYear }
        val budgetLimit = currentBudget?.globalBudgetAmount ?: 2500.0
        val spentPercent = if (budgetLimit > 0) (totalExpense / budgetLimit) * 100.0 else 0.0

        val categoryBreakdown = categoryExpenses.mapNotNull { (catId, amount) ->
            catMap[catId]?.let { category ->
                CategorySpending(
                    category = category,
                    totalAmount = amount,
                    transactionCount = categoryCounts[catId] ?: 0,
                    percentageOfTotal = if (totalExpense > 0) (amount / totalExpense) * 100.0 else 0.0
                )
            }
        }.sortedByDescending { it.totalAmount }

        val dailyTrend = dailyMap.entries.map { it.key to it.value }.sortedBy { it.first }
        val topExpenses = transactions.filter { it.type == TransactionType.EXPENSE }
            .sortedByDescending { it.amount }
            .take(5)

        FinancialReport(
            monthYear = monthYear,
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            netBalance = netBalance,
            savingsRate = savingsRate,
            budgetLimit = budgetLimit,
            budgetSpentPercentage = spentPercent,
            categoryBreakdown = categoryBreakdown,
            dailyTrend = dailyTrend,
            topExpenses = topExpenses
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        FinancialReport(
            monthYear = "2026-08",
            totalIncome = 0.0,
            totalExpense = 0.0,
            netBalance = 0.0,
            savingsRate = 0.0,
            budgetLimit = 2500.0,
            budgetSpentPercentage = 0.0,
            categoryBreakdown = emptyList(),
            dailyTrend = emptyList(),
            topExpenses = emptyList()
        )
    )

    // Current Month Budget
    val currentMonthBudget: StateFlow<MonthlyBudget?> = combine(
        allBudgets,
        _selectedMonthYear
    ) { budgets, monthYear ->
        budgets.find { it.monthYear == monthYear } ?: MonthlyBudget(
            monthYear = monthYear,
            globalBudgetAmount = 2500.0,
            alertThresholdPercent = 80,
            isAlertEnabled = true
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // --- ACTIONS ---

    fun changeMonth(offsetMonths: Int) {
        val parts = _selectedMonthYear.value.split("-")
        if (parts.size == 2) {
            val year = parts[0].toIntOrNull() ?: 2026
            val month = (parts[1].toIntOrNull() ?: 8) - 1
            val cal = Calendar.getInstance()
            cal.set(year, month, 1)
            cal.add(Calendar.MONTH, offsetMonths)
            _selectedMonthYear.value = String.format(Locale.US, "%04d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
        }
    }

    fun setSelectedMonthYear(monthYear: String) {
        _selectedMonthYear.value = monthYear
    }

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
    }

    fun setFilterCriteria(criteria: FilterCriteria) {
        _filterCriteria.value = criteria
    }

    fun resetFilters() {
        _filterCriteria.value = FilterCriteria()
    }

    fun addTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.insertTransaction(transaction)
            showMessage("Transacción guardada exitosamente")
        }
    }

    fun updateTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.updateTransaction(transaction)
            showMessage("Transacción actualizada")
        }
    }

    fun deleteTransaction(id: Long) {
        viewModelScope.launch {
            repository.deleteTransaction(id)
            showMessage("Transacción eliminada")
        }
    }

    fun saveBudget(budget: MonthlyBudget) {
        viewModelScope.launch {
            repository.saveBudget(budget)
            showMessage("Presupuesto mensual guardado")
        }
    }

    fun addCategory(category: Category) {
        viewModelScope.launch {
            repository.insertCategory(category)
            showMessage("Categoría creada")
        }
    }

    fun updateSecuritySettings(settings: SecuritySettings) {
        repository.updateSecuritySettings(settings)
        showMessage("Configuración de seguridad guardada")
    }

    fun setActiveCurrency(currencyCode: String) {
        repository.setActiveCurrency(currencyCode)
        showMessage("Moneda principal cambiada a $currencyCode")
    }

    fun triggerManualCloudSync() {
        viewModelScope.launch {
            repository.triggerCloudSync()
            showMessage("Sincronización en la nube completada")
        }
    }

    fun scanReceiptWithAi(bitmap: Bitmap, imagePath: String? = null) {
        viewModelScope.launch {
            _isScanningReceipt.value = true
            try {
                val result = GeminiReceiptScanner.parseReceiptImage(bitmap).copy(imagePath = imagePath)
                _scanResult.value = result
                showMessage("Recibo procesado: ${result.merchant} ($${result.totalAmount})")
            } catch (e: Exception) {
                showMessage("Error al escanear recibo: ${e.localizedMessage}")
            } finally {
                _isScanningReceipt.value = false
            }
        }
    }

    fun clearScanResult() {
        _scanResult.value = null
    }

    fun unlockApp() {
        _isAppLocked.value = false
    }

    fun lockApp() {
        // Biometric / security lock disabled per user request
        _isAppLocked.value = false
    }

    fun showMessage(message: String) {
        _userMessage.value = message
    }

    fun clearMessage() {
        _userMessage.value = null
    }
}
