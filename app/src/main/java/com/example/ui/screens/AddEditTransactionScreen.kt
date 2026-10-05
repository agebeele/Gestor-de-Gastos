package com.example.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.currency.CurrencyManager
import com.example.data.model.Category
import com.example.data.model.PaymentMethod
import com.example.data.model.ReceiptScanResult
import com.example.data.model.Transaction
import com.example.data.model.TransactionType
import com.example.ui.components.CategoryIconHelper
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditTransactionScreen(
    initialTransaction: Transaction?,
    initialScanResult: ReceiptScanResult?,
    categories: List<Category>,
    activeCurrencyCode: String,
    onSave: (Transaction) -> Unit,
    onDelete: (Long) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val isEdit = initialTransaction != null

    // Determine initial values
    var type by remember {
        mutableStateOf(
            initialTransaction?.type ?: TransactionType.EXPENSE
        )
    }

    var amountText by remember {
        mutableStateOf(
            if (initialTransaction != null) initialTransaction.amount.toString()
            else if (initialScanResult != null && initialScanResult.totalAmount > 0.0) initialScanResult.totalAmount.toString()
            else ""
        )
    }

    var title by remember {
        mutableStateOf(
            initialTransaction?.title ?: initialScanResult?.merchant ?: ""
        )
    }

    var selectedCurrency by remember {
        mutableStateOf(initialTransaction?.currencyCode ?: activeCurrencyCode)
    }

    var selectedCategoryId by remember {
        val matchingCategory = if (initialScanResult != null) {
            categories.find { it.name.contains(initialScanResult.suggestedCategory, ignoreCase = true) }
        } else null
        mutableStateOf(initialTransaction?.categoryId ?: matchingCategory?.id ?: categories.firstOrNull { it.isIncome == (type == TransactionType.INCOME) }?.id ?: 1L)
    }

    var dateMillis by remember {
        mutableLongStateOf(initialTransaction?.date ?: System.currentTimeMillis())
    }

    var paymentMethod by remember {
        mutableStateOf(initialTransaction?.paymentMethod ?: PaymentMethod.CARD)
    }

    var notes by remember {
        mutableStateOf(initialTransaction?.notes ?: initialScanResult?.notes ?: "")
    }

    var tagsText by remember {
        mutableStateOf(initialTransaction?.tags?.joinToString(", ") ?: "")
    }

    var isRecurring by remember {
        mutableStateOf(initialTransaction?.isRecurring ?: false)
    }

    val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.US)
    val availableCategories = categories.filter { it.isIncome == (type == TransactionType.INCOME) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("add_edit_transaction_screen")
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // --- HEADER ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onCancel) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Atrás",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = if (isEdit) "Editar Movimiento" else "Nuevo Movimiento",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (isEdit) {
                IconButton(onClick = { initialTransaction?.let { onDelete(it.id) } }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Eliminar",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            } else {
                Spacer(modifier = Modifier.size(48.dp))
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // --- TYPE SEGMENTED BUTTON (GASTO / INGRESO) ---
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier.fillMaxWidth()
        ) {
            SegmentedButton(
                selected = type == TransactionType.EXPENSE,
                onClick = {
                    type = TransactionType.EXPENSE
                    val firstExpenseCat = categories.firstOrNull { !it.isIncome }
                    if (firstExpenseCat != null) selectedCategoryId = firstExpenseCat.id
                },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
            ) {
                Text(
                    text = "Gasto",
                    fontWeight = FontWeight.Bold,
                    color = if (type == TransactionType.EXPENSE) ExpenseRed else MaterialTheme.colorScheme.onSurface
                )
            }
            SegmentedButton(
                selected = type == TransactionType.INCOME,
                onClick = {
                    type = TransactionType.INCOME
                    val firstIncomeCat = categories.firstOrNull { it.isIncome }
                    if (firstIncomeCat != null) selectedCategoryId = firstIncomeCat.id
                },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
            ) {
                Text(
                    text = "Ingreso",
                    fontWeight = FontWeight.Bold,
                    color = if (type == TransactionType.INCOME) IncomeGreen else MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // --- AMOUNT INPUT CARD ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (type == TransactionType.EXPENSE) Color(0xFFFEF2F2) else Color(0xFFECFDF5)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Monto de la Transacción",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF4B5563)
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val curr = CurrencyManager.getCurrency(selectedCurrency)
                    Text(
                        text = curr.symbol,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (type == TransactionType.EXPENSE) ExpenseRed else IncomeGreen
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        textStyle = TextStyle(
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = if (type == TransactionType.EXPENSE) ExpenseRed else IncomeGreen
                        ),
                        placeholder = { Text("0.00", fontSize = 32.sp, textAlign = TextAlign.Center) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.width(180.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent
                        )
                    )
                }

                // Moneda Fija: Pesos Mexicanos
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🇲🇽 Moneda: Pesos Mexicanos ($ MXN)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // --- CONCEPT / TITLE ---
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Concepto o Establecimiento *") },
            placeholder = { Text("Ej. Supermercado, Alquiler, Sueldo...") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        // --- CATEGORY SELECTOR ---
        Text(
            text = "Categoría *",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            availableCategories.forEach { cat ->
                val isSelected = selectedCategoryId == cat.id
                val catColor = CategoryIconHelper.parseColor(cat.colorHex)

                FilterChip(
                    selected = isSelected,
                    onClick = { selectedCategoryId = cat.id },
                    label = { Text(cat.name) },
                    leadingIcon = {
                        Icon(
                            imageVector = CategoryIconHelper.getIcon(cat.iconName),
                            contentDescription = null,
                            tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else catColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- DATE PICKER & PAYMENT METHOD ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Date Picker Card
            OutlinedTextField(
                value = dateFormatter.format(Date(dateMillis)),
                onValueChange = {},
                readOnly = true,
                label = { Text("Fecha") },
                trailingIcon = {
                    IconButton(onClick = {
                        val cal = Calendar.getInstance().apply { timeInMillis = dateMillis }
                        DatePickerDialog(
                            context,
                            { _, year, month, dayOfMonth ->
                                cal.set(year, month, dayOfMonth)
                                dateMillis = cal.timeInMillis
                            },
                            cal.get(Calendar.YEAR),
                            cal.get(Calendar.MONTH),
                            cal.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    }) {
                        Icon(Icons.Default.CalendarToday, contentDescription = "Seleccionar fecha")
                    }
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- PAYMENT METHOD ---
        Text(
            text = "Método de Pago",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            PaymentMethod.values().forEach { method ->
                FilterChip(
                    selected = paymentMethod == method,
                    onClick = { paymentMethod = method },
                    label = { Text(method.displayName) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- NOTES & TAGS ---
        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("Notas adicionales") },
            placeholder = { Text("Detalles sobre la compra...") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            minLines = 2
        )

        Spacer(modifier = Modifier.height(14.dp))

        OutlinedTextField(
            value = tagsText,
            onValueChange = { tagsText = it },
            label = { Text("Etiquetas (separadas por coma)") },
            placeholder = { Text("despensa, fin de semana, fijo") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(14.dp))

        // --- RECURRING TOGGLE ---
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Transacción Recurrente",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Se repite automáticamente cada mes (ej. sueldo, alquiler)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = isRecurring,
                    onCheckedChange = { isRecurring = it }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- SAVE BUTTON ---
        Button(
            onClick = {
                val parsedAmount = amountText.toDoubleOrNull() ?: 0.0
                if (title.isNotBlank() && parsedAmount > 0.0) {
                    val convertedAmount = CurrencyManager.convert(
                        amount = parsedAmount,
                        fromCode = selectedCurrency,
                        toCode = activeCurrencyCode
                    )

                    val newTx = Transaction(
                        id = initialTransaction?.id ?: 0,
                        title = title.trim(),
                        amount = convertedAmount,
                        type = type,
                        categoryId = selectedCategoryId,
                        date = dateMillis,
                        currencyCode = selectedCurrency,
                        originalAmount = parsedAmount,
                        exchangeRateToMain = convertedAmount / parsedAmount,
                        notes = notes.trim(),
                        receiptImagePath = initialTransaction?.receiptImagePath,
                        paymentMethod = paymentMethod,
                        tags = if (tagsText.isBlank()) emptyList() else tagsText.split(",").map { it.trim() },
                        isRecurring = isRecurring
                    )
                    onSave(newTx)
                }
            },
            enabled = title.isNotBlank() && (amountText.toDoubleOrNull() ?: 0.0) > 0.0,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("save_transaction_button"),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(imageVector = Icons.Default.Check, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isEdit) "Guardar Cambios" else "Registrar Movimiento",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(30.dp))
    }
}
