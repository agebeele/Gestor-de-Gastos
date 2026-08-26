package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.currency.CurrencyManager
import com.example.data.model.Category
import com.example.data.model.Transaction
import com.example.data.model.TransactionType
import com.example.ui.components.TransactionItemCard
import com.example.ui.viewmodel.FilterCriteria

@Composable
fun TransactionsScreen(
    transactions: List<Transaction>,
    categories: List<Category>,
    activeCurrencyCode: String,
    filterCriteria: FilterCriteria,
    onFilterChange: (FilterCriteria) -> Unit,
    onOpenFilterSheet: () -> Unit,
    onTransactionClick: (Transaction) -> Unit,
    onAddNewTransaction: () -> Unit
) {
    val categoryMap = categories.associateBy { it.id }
    val isFiltered = filterCriteria.query.isNotBlank() ||
        filterCriteria.type != null ||
        filterCriteria.categoryId != null ||
        filterCriteria.paymentMethod != null ||
        filterCriteria.minAmount != null ||
        filterCriteria.maxAmount != null

    val totalSpent = transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
    val totalIncome = transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("transactions_screen_content")
            .padding(top = 8.dp)
    ) {
        // --- TOP BAR WITH SEARCH & FILTER BUTTON ---
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Text(
                text = "Historial de Transacciones",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = filterCriteria.query,
                    onValueChange = { onFilterChange(filterCriteria.copy(query = it)) },
                    placeholder = { Text("Buscar concepto, nota, #tag...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Buscar",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingIcon = {
                        if (filterCriteria.query.isNotBlank()) {
                            IconButton(onClick = { onFilterChange(filterCriteria.copy(query = "")) }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Limpiar",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    )
                )

                // Advanced Filter Button
                IconButton(
                    onClick = onOpenFilterSheet,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (isFiltered) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        )
                ) {
                    if (isFiltered) {
                        BadgedBox(badge = { Badge { Text("!") } }) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "Filtros activos",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Filtros",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Quick Type Filter Chips
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = filterCriteria.type == null,
                    onClick = { onFilterChange(filterCriteria.copy(type = null)) },
                    label = { Text("Todos (${transactions.size})") }
                )
                FilterChip(
                    selected = filterCriteria.type == TransactionType.EXPENSE,
                    onClick = { onFilterChange(filterCriteria.copy(type = TransactionType.EXPENSE)) },
                    label = { Text("Gastos") }
                )
                FilterChip(
                    selected = filterCriteria.type == TransactionType.INCOME,
                    onClick = { onFilterChange(filterCriteria.copy(type = TransactionType.INCOME)) },
                    label = { Text("Ingresos") }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Summary Ticker
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Gastos: -${CurrencyManager.formatCompact(totalSpent, activeCurrencyCode)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Ingresos: +${CurrencyManager.formatCompact(totalIncome, activeCurrencyCode)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // --- TRANSACTIONS LIST ---
        if (transactions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No se encontraron movimientos",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Prueba modificando tus términos de búsqueda o filtros.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (isFiltered) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { onFilterChange(FilterCriteria()) },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Restablecer Filtros")
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(transactions, key = { it.id }) { tx ->
                    TransactionItemCard(
                        transaction = tx,
                        category = categoryMap[tx.categoryId],
                        activeCurrencyCode = activeCurrencyCode,
                        onClick = { onTransactionClick(tx) }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(70.dp))
                }
            }
        }
    }
}
