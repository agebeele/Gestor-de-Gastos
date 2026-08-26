package com.example.data.repository

import com.example.data.local.AppDao
import com.example.data.local.BudgetEntity
import com.example.data.local.CategoryEntity
import com.example.data.local.TransactionEntity
import com.example.data.model.Category
import com.example.data.model.MonthlyBudget
import com.example.data.model.SecuritySettings
import com.example.data.model.Transaction
import com.example.data.sync.CloudSyncManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

class FinanceRepository(private val dao: AppDao) {

    val allTransactions: Flow<List<Transaction>> = dao.getAllTransactionsFlow().map { list ->
        list.map { it.toDomain() }
    }

    val allCategories: Flow<List<Category>> = dao.getAllCategoriesFlow().map { list ->
        list.map { it.toDomain() }
    }

    val allBudgets: Flow<List<MonthlyBudget>> = dao.getAllBudgetsFlow().map { list ->
        list.map { it.toDomain() }
    }

    private val _syncStatus = MutableStateFlow(
        CloudSyncManager.SyncStatus(
            lastSyncTimestamp = System.currentTimeMillis() - 60000L * 15,
            backupFingerprint = "7F3B9A2C",
            isCloudConnected = true,
            syncedItemCount = 5,
            message = "Sincronización en la nube activa y protegida"
        )
    )
    val syncStatus = _syncStatus.asStateFlow()

    private val _securitySettings = MutableStateFlow(
        SecuritySettings(
            biometricEnabled = false,
            pinCode = "",
            endToEndEncryptionEnabled = true,
            lastCloudBackupTime = System.currentTimeMillis() - 60000L * 15,
            autoCloudSync = true,
            customMonthlyAlerts = true,
            activeCurrencyCode = "USD"
        )
    )
    val securitySettings = _securitySettings.asStateFlow()

    fun getTransactionsInRange(start: Long, end: Long): Flow<List<Transaction>> {
        return dao.getTransactionsInRangeFlow(start, end).map { list ->
            list.map { it.toDomain() }
        }
    }

    fun getBudgetForMonth(monthYear: String): Flow<MonthlyBudget?> {
        return dao.getBudgetForMonthFlow(monthYear).map { it?.toDomain() }
    }

    suspend fun insertTransaction(transaction: Transaction): Long {
        val id = dao.insertTransaction(TransactionEntity.fromDomain(transaction))
        if (_securitySettings.value.autoCloudSync) {
            triggerCloudSync()
        }
        return id
    }

    suspend fun updateTransaction(transaction: Transaction) {
        dao.updateTransaction(TransactionEntity.fromDomain(transaction))
        if (_securitySettings.value.autoCloudSync) {
            triggerCloudSync()
        }
    }

    suspend fun deleteTransaction(transactionId: Long) {
        dao.deleteTransactionById(transactionId)
        if (_securitySettings.value.autoCloudSync) {
            triggerCloudSync()
        }
    }

    suspend fun insertCategory(category: Category): Long {
        return dao.insertCategory(CategoryEntity.fromDomain(category))
    }

    suspend fun updateCategory(category: Category) {
        dao.updateCategory(CategoryEntity.fromDomain(category))
    }

    suspend fun deleteCategory(category: Category) {
        dao.deleteCategory(CategoryEntity.fromDomain(category))
    }

    suspend fun saveBudget(budget: MonthlyBudget): Long {
        return dao.insertOrUpdateBudget(BudgetEntity.fromDomain(budget))
    }

    suspend fun triggerCloudSync() {
        val status = CloudSyncManager.performCloudBackup(dao)
        _syncStatus.value = status
        _securitySettings.value = _securitySettings.value.copy(
            lastCloudBackupTime = status.lastSyncTimestamp
        )
    }

    fun updateSecuritySettings(settings: SecuritySettings) {
        _securitySettings.value = settings
    }

    fun setActiveCurrency(currencyCode: String) {
        _securitySettings.value = _securitySettings.value.copy(activeCurrencyCode = currencyCode)
    }
}
