package com.example.data.currency

import com.example.data.model.CurrencyInfo
import java.text.NumberFormat
import java.util.Locale

object CurrencyManager {
    const val DEFAULT_CURRENCY = "MXN"

    val supportedCurrencies = listOf(
        CurrencyInfo(code = "MXN", symbol = "$", name = "Peso Mexicano", flagEmoji = "🇲🇽", rateToUSD = 1.0)
    )

    fun getCurrency(code: String = DEFAULT_CURRENCY): CurrencyInfo {
        return supportedCurrencies.find { it.code.equals(code, ignoreCase = true) }
            ?: supportedCurrencies.first()
    }

    /**
     * Convert amount - exclusively in Mexican Pesos (MXN)
     */
    fun convert(amount: Double, fromCode: String = DEFAULT_CURRENCY, toCode: String = DEFAULT_CURRENCY): Double {
        return amount
    }

    fun format(amount: Double, currencyCode: String = DEFAULT_CURRENCY): String {
        val format = NumberFormat.getNumberInstance(Locale.US).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
        return "$${format.format(amount)} MXN"
    }

    fun formatCompact(amount: Double, currencyCode: String = DEFAULT_CURRENCY): String {
        val format = NumberFormat.getNumberInstance(Locale.US).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
        return "$${format.format(amount)}"
    }
}
