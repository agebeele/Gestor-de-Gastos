package com.example.data.model

enum class TransactionType {
    EXPENSE,
    INCOME
}

enum class PaymentMethod(val displayName: String) {
    CARD("Tarjeta Débito/Crédito"),
    CASH("Efectivo"),
    TRANSFER("Transferencia"),
    DIGITAL_WALLET("Billetera Digital"),
    OTHER("Otro")
}

data class Transaction(
    val id: Long = 0,
    val title: String,
    val amount: Double,
    val type: TransactionType,
    val categoryId: Long,
    val date: Long = System.currentTimeMillis(),
    val currencyCode: String = "USD",
    val originalAmount: Double = amount,
    val exchangeRateToMain: Double = 1.0,
    val notes: String = "",
    val receiptImagePath: String? = null,
    val paymentMethod: PaymentMethod = PaymentMethod.CARD,
    val tags: List<String> = emptyList(),
    val isRecurring: Boolean = false
)

data class Category(
    val id: Long = 0,
    val name: String,
    val iconName: String,
    val colorHex: String,
    val isIncome: Boolean = false,
    val defaultBudgetLimit: Double = 0.0
)

data class MonthlyBudget(
    val id: Long = 0,
    val monthYear: String, // Format: "YYYY-MM" e.g., "2026-08"
    val globalBudgetAmount: Double,
    val alertThresholdPercent: Int = 80, // e.g., 80%
    val isAlertEnabled: Boolean = true,
    val categoryLimits: Map<Long, Double> = emptyMap()
)

data class CurrencyInfo(
    val code: String,
    val symbol: String,
    val name: String,
    val flagEmoji: String,
    val rateToUSD: Double // e.g., USD=1.0, EUR=0.92, MXN=18.5, GBP=0.79, JPY=155.0, CAD=1.38, COP=4050.0, ARS=940.0
)

data class CategorySpending(
    val category: Category,
    val totalAmount: Double,
    val transactionCount: Int,
    val percentageOfTotal: Double
)

data class FinancialReport(
    val monthYear: String,
    val totalIncome: Double,
    val totalExpense: Double,
    val netBalance: Double,
    val savingsRate: Double, // (Income - Expense) / Income * 100
    val budgetLimit: Double,
    val budgetSpentPercentage: Double,
    val categoryBreakdown: List<CategorySpending>,
    val dailyTrend: List<Pair<Int, Double>>, // Day of month -> Expense
    val topExpenses: List<Transaction>
)

data class SecuritySettings(
    val biometricEnabled: Boolean = false,
    val pinCode: String = "",
    val endToEndEncryptionEnabled: Boolean = true,
    val lastCloudBackupTime: Long = 0L,
    val autoCloudSync: Boolean = true,
    val customMonthlyAlerts: Boolean = true,
    val activeCurrencyCode: String = "USD"
)

data class ReceiptScanResult(
    val merchant: String = "",
    val totalAmount: Double = 0.0,
    val dateString: String = "",
    val suggestedCategory: String = "",
    val notes: String = "",
    val items: List<String> = emptyList(),
    val confidence: String = "Alta"
)
