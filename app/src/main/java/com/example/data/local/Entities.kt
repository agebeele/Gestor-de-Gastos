package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.example.data.model.PaymentMethod
import com.example.data.model.Transaction
import com.example.data.model.TransactionType

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val amount: Double,
    val type: String, // "EXPENSE" or "INCOME"
    val categoryId: Long,
    val date: Long,
    val currencyCode: String,
    val originalAmount: Double,
    val exchangeRateToMain: Double,
    val notes: String,
    val receiptImagePath: String?,
    val paymentMethod: String,
    val tags: String, // Comma separated tags
    val isRecurring: Boolean
) {
    fun toDomain(): Transaction = Transaction(
        id = id,
        title = title,
        amount = amount,
        type = if (type == "INCOME") TransactionType.INCOME else TransactionType.EXPENSE,
        categoryId = categoryId,
        date = date,
        currencyCode = currencyCode,
        originalAmount = originalAmount,
        exchangeRateToMain = exchangeRateToMain,
        notes = notes,
        receiptImagePath = receiptImagePath,
        paymentMethod = try { PaymentMethod.valueOf(paymentMethod) } catch (e: Exception) { PaymentMethod.CARD },
        tags = if (tags.isBlank()) emptyList() else tags.split(",").map { it.trim() },
        isRecurring = isRecurring
    )

    companion object {
        fun fromDomain(transaction: Transaction): TransactionEntity = TransactionEntity(
            id = transaction.id,
            title = transaction.title,
            amount = transaction.amount,
            type = transaction.type.name,
            categoryId = transaction.categoryId,
            date = transaction.date,
            currencyCode = transaction.currencyCode,
            originalAmount = transaction.originalAmount,
            exchangeRateToMain = transaction.exchangeRateToMain,
            notes = transaction.notes,
            receiptImagePath = transaction.receiptImagePath,
            paymentMethod = transaction.paymentMethod.name,
            tags = transaction.tags.joinToString(","),
            isRecurring = transaction.isRecurring
        )
    }
}

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val iconName: String,
    val colorHex: String,
    val isIncome: Boolean,
    val defaultBudgetLimit: Double
) {
    fun toDomain() = com.example.data.model.Category(
        id = id,
        name = name,
        iconName = iconName,
        colorHex = colorHex,
        isIncome = isIncome,
        defaultBudgetLimit = defaultBudgetLimit
    )

    companion object {
        fun fromDomain(cat: com.example.data.model.Category) = CategoryEntity(
            id = cat.id,
            name = cat.name,
            iconName = cat.iconName,
            colorHex = cat.colorHex,
            isIncome = cat.isIncome,
            defaultBudgetLimit = cat.defaultBudgetLimit
        )
    }
}

@Entity(tableName = "monthly_budgets")
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val monthYear: String, // "YYYY-MM"
    val globalBudgetAmount: Double,
    val alertThresholdPercent: Int,
    val isAlertEnabled: Boolean,
    val categoryLimitsJson: String // Serialized format: "catId:amount,catId:amount"
) {
    fun toDomain(): com.example.data.model.MonthlyBudget {
        val limits = mutableMapOf<Long, Double>()
        if (categoryLimitsJson.isNotBlank()) {
            categoryLimitsJson.split(";").forEach { pair ->
                val parts = pair.split(":")
                if (parts.size == 2) {
                    parts[0].toLongOrNull()?.let { catId ->
                        parts[1].toDoubleOrNull()?.let { limit ->
                            limits[catId] = limit
                        }
                    }
                }
            }
        }
        return com.example.data.model.MonthlyBudget(
            id = id,
            monthYear = monthYear,
            globalBudgetAmount = globalBudgetAmount,
            alertThresholdPercent = alertThresholdPercent,
            isAlertEnabled = isAlertEnabled,
            categoryLimits = limits
        )
    }

    companion object {
        fun fromDomain(budget: com.example.data.model.MonthlyBudget): BudgetEntity {
            val json = budget.categoryLimits.entries.joinToString(";") { "${it.key}:${it.value}" }
            return BudgetEntity(
                id = budget.id,
                monthYear = budget.monthYear,
                globalBudgetAmount = budget.globalBudgetAmount,
                alertThresholdPercent = budget.alertThresholdPercent,
                isAlertEnabled = budget.isAlertEnabled,
                categoryLimitsJson = json
            )
        }
    }
}
