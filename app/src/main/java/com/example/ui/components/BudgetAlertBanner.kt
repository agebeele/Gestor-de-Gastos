package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.currency.CurrencyManager
import java.util.Locale

@Composable
fun BudgetAlertBanner(
    totalSpent: Double,
    budgetLimit: Double,
    alertThresholdPercent: Int,
    currencyCode: String,
    onAdjustBudgetClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (budgetLimit <= 0.0) return

    val percentage = (totalSpent / budgetLimit) * 100.0
    val isExceeded = percentage >= 100.0
    val isThresholdReached = percentage >= alertThresholdPercent

    AnimatedVisibility(
        visible = isThresholdReached,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        val bannerBg = if (isExceeded) {
            Color(0xFFFEF2F2)
        } else {
            Color(0xFFFFFBEB)
        }

        val primaryColor = if (isExceeded) {
            Color(0xFFDC2626)
        } else {
            Color(0xFFD97706)
        }

        val title = if (isExceeded) {
            "¡Límite de Presupuesto Excedido!"
        } else {
            "¡Alerta de Gasto Mensual ($alertThresholdPercent%)!"
        }

        val description = if (isExceeded) {
            val excess = totalSpent - budgetLimit
            "Has superado tu presupuesto en ${CurrencyManager.format(excess, currencyCode)}."
        } else {
            val remaining = budgetLimit - totalSpent
            "Has consumido el ${String.format(Locale.US, "%.0f%%", percentage)} del presupuesto mensual. Te quedan ${CurrencyManager.format(remaining, currencyCode)}."
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = bannerBg),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(primaryColor.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isExceeded) Icons.Default.Warning else Icons.Default.NotificationsActive,
                            contentDescription = "Alerta",
                            tint = primaryColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = primaryColor
                        )
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = Color(0xFF374151)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Progress Bar
                LinearProgressIndicator(
                    progress = { (percentage / 100.0).toFloat().coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = primaryColor,
                    trackColor = primaryColor.copy(alpha = 0.2f),
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${String.format(Locale.US, "%.0f%%", percentage)} gastado",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = primaryColor
                    )
                    TextButton(
                        onClick = onAdjustBudgetClick,
                        colors = ButtonDefaults.textButtonColors(contentColor = primaryColor)
                    ) {
                        Text(
                            text = "Ajustar Presupuesto",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
