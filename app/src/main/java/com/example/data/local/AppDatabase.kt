package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        TransactionEntity::class,
        CategoryEntity::class,
        BudgetEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "gestor_gastos_database"
                )
                    .addCallback(AppDatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class AppDatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialData(database.appDao())
                    }
                }
            }
        }

        suspend fun populateInitialData(dao: AppDao) {
            val initialCategories = listOf(
                CategoryEntity(name = "Supermercado", iconName = "ShoppingCart", colorHex = "#10B981", isIncome = false, defaultBudgetLimit = 400.0),
                CategoryEntity(name = "Restaurantes y Cafés", iconName = "Restaurant", colorHex = "#F59E0B", isIncome = false, defaultBudgetLimit = 200.0),
                CategoryEntity(name = "Vivienda y Alquiler", iconName = "Home", colorHex = "#3B82F6", isIncome = false, defaultBudgetLimit = 800.0),
                CategoryEntity(name = "Servicios Básicos", iconName = "Bolt", colorHex = "#8B5CF6", isIncome = false, defaultBudgetLimit = 150.0),
                CategoryEntity(name = "Transporte y Gasolina", iconName = "DirectionsCar", colorHex = "#06B6D4", isIncome = false, defaultBudgetLimit = 180.0),
                CategoryEntity(name = "Entretenimiento", iconName = "Movie", colorHex = "#EC4899", isIncome = false, defaultBudgetLimit = 120.0),
                CategoryEntity(name = "Salud y Farmacia", iconName = "LocalHospital", colorHex = "#EF4444", isIncome = false, defaultBudgetLimit = 100.0),
                CategoryEntity(name = "Educación", iconName = "School", colorHex = "#6366F1", isIncome = false, defaultBudgetLimit = 100.0),
                CategoryEntity(name = "Compras y Ropa", iconName = "ShoppingBag", colorHex = "#F97316", isIncome = false, defaultBudgetLimit = 150.0),
                CategoryEntity(name = "Suscripciones Digitales", iconName = "Subscriptions", colorHex = "#14B8A6", isIncome = false, defaultBudgetLimit = 50.0),
                CategoryEntity(name = "Viajes", iconName = "Flight", colorHex = "#0EA5E9", isIncome = false, defaultBudgetLimit = 300.0),
                CategoryEntity(name = "Otros Gastos", iconName = "Category", colorHex = "#64748B", isIncome = false, defaultBudgetLimit = 100.0),
                // Incomes
                CategoryEntity(name = "Salario Mensual", iconName = "Payments", colorHex = "#10B981", isIncome = true, defaultBudgetLimit = 0.0),
                CategoryEntity(name = "Freelance / Honorarios", iconName = "Work", colorHex = "#3B82F6", isIncome = true, defaultBudgetLimit = 0.0),
                CategoryEntity(name = "Inversiones", iconName = "TrendingUp", colorHex = "#059669", isIncome = true, defaultBudgetLimit = 0.0),
                CategoryEntity(name = "Otros Ingresos", iconName = "AttachMoney", colorHex = "#14B8A6", isIncome = true, defaultBudgetLimit = 0.0)
            )
            dao.insertCategories(initialCategories)

            // Current Month Initial Budget
            val now = java.util.Calendar.getInstance()
            val currentMonthYear = String.format(java.util.Locale.US, "%04d-%02d", now.get(java.util.Calendar.YEAR), now.get(java.util.Calendar.MONTH) + 1)
            dao.insertOrUpdateBudget(
                BudgetEntity(
                    monthYear = currentMonthYear,
                    globalBudgetAmount = 2500.0,
                    alertThresholdPercent = 80,
                    isAlertEnabled = true,
                    categoryLimitsJson = "1:400.0;2:200.0;3:800.0;4:150.0;5:180.0;6:120.0;7:100.0"
                )
            )

            // Sample initial helpful transactions for immediate visualization
            val sampleTransactions = listOf(
                TransactionEntity(
                    title = "Nómina Quincenal",
                    amount = 1850.0,
                    type = "INCOME",
                    categoryId = 13,
                    date = System.currentTimeMillis() - 86400000L * 4,
                    currencyCode = "USD",
                    originalAmount = 1850.0,
                    exchangeRateToMain = 1.0,
                    notes = "Pago de sueldo quincenal",
                    receiptImagePath = null,
                    paymentMethod = "TRANSFER",
                    tags = "sueldo,fijo",
                    isRecurring = true
                ),
                TransactionEntity(
                    title = "Compra Supermercado Orgánico",
                    amount = 142.50,
                    type = "EXPENSE",
                    categoryId = 1,
                    date = System.currentTimeMillis() - 86400000L * 2,
                    currencyCode = "USD",
                    originalAmount = 142.50,
                    exchangeRateToMain = 1.0,
                    notes = "Frutas, verduras y despensa semanal",
                    receiptImagePath = null,
                    paymentMethod = "CARD",
                    tags = "despensa,semanal",
                    isRecurring = false
                ),
                TransactionEntity(
                    title = "Cena Restaurante La Terraza",
                    amount = 58.00,
                    type = "EXPENSE",
                    categoryId = 2,
                    date = System.currentTimeMillis() - 86400000L * 1,
                    currencyCode = "USD",
                    originalAmount = 58.00,
                    exchangeRateToMain = 1.0,
                    notes = "Cena de fin de semana con amigos",
                    receiptImagePath = null,
                    paymentMethod = "CARD",
                    tags = "ocio,amigos",
                    isRecurring = false
                ),
                TransactionEntity(
                    title = "Recarga Gasolina Premium",
                    amount = 45.00,
                    type = "EXPENSE",
                    categoryId = 5,
                    date = System.currentTimeMillis() - 3600000L * 6,
                    currencyCode = "USD",
                    originalAmount = 45.00,
                    exchangeRateToMain = 1.0,
                    notes = "Tanque lleno estación central",
                    receiptImagePath = null,
                    paymentMethod = "CARD",
                    tags = "auto,gasolina",
                    isRecurring = false
                ),
                TransactionEntity(
                    title = "Suscripción Streaming 4K",
                    amount = 15.99,
                    type = "EXPENSE",
                    categoryId = 10,
                    date = System.currentTimeMillis() - 3600000L * 2,
                    currencyCode = "USD",
                    originalAmount = 15.99,
                    exchangeRateToMain = 1.0,
                    notes = "Membresía mensual",
                    receiptImagePath = null,
                    paymentMethod = "DIGITAL_WALLET",
                    tags = "suscripcion,series",
                    isRecurring = true
                )
            )
            dao.insertTransactions(sampleTransactions)
        }
    }
}
