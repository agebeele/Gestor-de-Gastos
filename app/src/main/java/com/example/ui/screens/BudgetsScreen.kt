package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.currency.CurrencyManager
import com.example.data.model.Category
import com.example.data.model.FinancialReport
import com.example.data.model.MonthlyBudget
import com.example.ui.components.CategoryIconHelper
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.WarningAmber
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun BudgetsScreen(
    currentBudget: MonthlyBudget?,
    report: FinancialReport,
    selectedMonthYear: String,
    categories: List<Category>,
    activeCurrencyCode: String,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSaveBudget: (MonthlyBudget) -> Unit
) {
    val expenseCategories = categories.filter { !it.isIncome }
    val categorySpendMap = report.categoryBreakdown.associate { it.category.id to it.totalAmount }

    var budgetLimitText by remember(currentBudget) {
        mutableStateOf(currentBudget?.globalBudgetAmount?.toString() ?: "2500.0")
    }

    var thresholdPercent by remember(currentBudget) {
        mutableIntStateOf(currentBudget?.alertThresholdPercent ?: 80)
    }

    var isAlertEnabled by remember(currentBudget) {
        mutableStateOf(currentBudget?.isAlertEnabled ?: true)
    }

    val categoryLimits = remember(currentBudget) {
        mutableStateMapOf<Long, String>().apply {
            expenseCategories.forEach { cat ->
                val limit = currentBudget?.categoryLimits?.get(cat.id) ?: cat.defaultBudgetLimit
                put(cat.id, limit.toString())
            }
        }
    }

    val monthTitle = try {
        val parts = selectedMonthYear.split("-")
        val cal = Calendar.getInstance()
        cal.set(Calendar.YEAR, parts[0].toInt())
        cal.set(Calendar.MONTH, parts[1].toInt() - 1)
        SimpleDateFormat("MMMM yyyy", Locale.forLanguageTag("es")).format(cal.time).replaceFirstChar { it.uppercase() }
    } catch (e: Exception) {
        selectedMonthYear
    }

    val globalLimit = budgetLimitText.toDoubleOrNull() ?: 2500.0
    val totalSpent = report.totalExpense
    val spentPercent = if (globalLimit > 0) (totalSpent / globalLimit) * 100.0 else 0.0

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("budgets_screen_content")
            .padding(top = 8.dp),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- HEADER & MONTH CONTROLLER ---
        item {
            Column {
                Text(
                    text = "Control de Presupuestos",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Límites mensuales y notificaciones preventivas de gastos",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onPrevMonth) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Anterior")
                        }
                        Text(
                            text = monthTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = onNextMonth) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Siguiente")
                        }
                    }
                }
            }
        }

        // --- GLOBAL MONTHLY BUDGET CARD ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalanceWallet,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Presupuesto Global Mensual",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Límite total para todos tus gastos del mes",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    OutlinedTextField(
                        value = budgetLimitText,
                        onValueChange = { budgetLimitText = it },
                        label = { Text("Límite Mensual ($activeCurrencyCode)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Progress Overview
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Gastado: ${CurrencyManager.format(totalSpent, activeCurrencyCode)}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (spentPercent > 100) ExpenseRed else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${String.format(Locale.US, "%.0f%%", spentPercent)} del límite",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (spentPercent >= thresholdPercent) WarningAmber else IncomeGreen
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LinearProgressIndicator(
                        progress = { (spentPercent / 100.0).toFloat().coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp)),
                        color = if (spentPercent >= 100) ExpenseRed else if (spentPercent >= thresholdPercent) WarningAmber else IncomeGreen,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        }

        // --- NOTIFICATION & ALERT SETTINGS ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(MaterialTheme.colorScheme.tertiaryContainer, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Alertas de Presupuesto",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Avisar cuando esté por alcanzar el límite",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Switch(
                            checked = isAlertEnabled,
                            onCheckedChange = { isAlertEnabled = it }
                        )
                    }

                    if (isAlertEnabled) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Umbral de Alerta: $thresholdPercent%",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Slider(
                            value = thresholdPercent.toFloat(),
                            onValueChange = { thresholdPercent = it.toInt() },
                            valueRange = 50f..100f,
                            steps = 9
                        )
                        Text(
                            text = "Recibirás una advertencia al gastar ${CurrencyManager.format(globalLimit * (thresholdPercent / 100.0), activeCurrencyCode)} ($thresholdPercent%).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // --- CATEGORY LIMITS SECTION ---
        item {
            Text(
                text = "Límites por Categoría de Gasto",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        items(expenseCategories) { cat ->
            val spentInCat = categorySpendMap[cat.id] ?: 0.0
            val catLimitStr = categoryLimits[cat.id] ?: "100.0"
            val catLimit = catLimitStr.toDoubleOrNull() ?: 100.0
            val catPercent = if (catLimit > 0) (spentInCat / catLimit) * 100.0 else 0.0
            val catColor = CategoryIconHelper.parseColor(cat.colorHex)

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(catColor.copy(alpha = 0.2f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = CategoryIconHelper.getIcon(cat.iconName),
                                    contentDescription = null,
                                    tint = catColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = cat.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        OutlinedTextField(
                            value = catLimitStr,
                            onValueChange = { categoryLimits[cat.id] = it },
                            label = { Text("Límite") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.width(120.dp),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Gastado: ${CurrencyManager.formatCompact(spentInCat, activeCurrencyCode)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${String.format(Locale.US, "%.0f%%", catPercent)}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (catPercent >= 100) ExpenseRed else catColor
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    LinearProgressIndicator(
                        progress = { (catPercent / 100.0).toFloat().coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = if (catPercent >= 100) ExpenseRed else catColor,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        }

        // --- SAVE BUTTON ---
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    val map = mutableMapOf<Long, Double>()
                    categoryLimits.forEach { (catId, str) ->
                        str.toDoubleOrNull()?.let { map[catId] = it }
                    }
                    val newBudget = MonthlyBudget(
                        id = currentBudget?.id ?: 0,
                        monthYear = selectedMonthYear,
                        globalBudgetAmount = budgetLimitText.toDoubleOrNull() ?: 2500.0,
                        alertThresholdPercent = thresholdPercent,
                        isAlertEnabled = isAlertEnabled,
                        categoryLimits = map
                    )
                    onSaveBudget(newBudget)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(imageVector = Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Guardar Configuración de Presupuesto",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}
