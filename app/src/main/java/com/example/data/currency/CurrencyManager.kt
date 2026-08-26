package com.example.data.currency

import com.example.data.model.CurrencyInfo
import java.text.NumberFormat
import java.util.Locale

object CurrencyManager {
    val supportedCurrencies = listOf(
        CurrencyInfo(code = "USD", symbol = "$", name = "Dólar Estadounidense", flagEmoji = "🇺🇸", rateToUSD = 1.0),
        CurrencyInfo(code = "EUR", symbol = "€", name = "Euro", flagEmoji = "🇪🇺", rateToUSD = 0.92),
        CurrencyInfo(code = "MXN", symbol = "$", name = "Peso Mexicano", flagEmoji = "🇲🇽", rateToUSD = 18.45),
        CurrencyInfo(code = "GBP", symbol = "£", name = "Libra Esterlina", flagEmoji = "🇬🇧", rateToUSD = 0.79),
        CurrencyInfo(code = "JPY", symbol = "¥", name = "Yen Japonés", flagEmoji = "🇯🇵", rateToUSD = 154.80),
        CurrencyInfo(code = "CAD", symbol = "C$", name = "Dólar Canadiense", flagEmoji = "🇨🇦", rateToUSD = 1.37),
        CurrencyInfo(code = "COP", symbol = "$", name = "Peso Colombiano", flagEmoji = "🇨🇴", rateToUSD = 4030.0),
        CurrencyInfo(code = "ARS", symbol = "$", name = "Peso Argentino", flagEmoji = "🇦🇷", rateToUSD = 945.0),
        CurrencyInfo(code = "CLP", symbol = "$", name = "Peso Chileno", flagEmoji = "🇨🇱", rateToUSD = 920.0),
        CurrencyInfo(code = "BRL", symbol = "R$", name = "Real Brasileño", flagEmoji = "🇧🇷", rateToUSD = 5.45),
        CurrencyInfo(code = "CHF", symbol = "CHF", name = "Franco Suizo", flagEmoji = "🇨🇭", rateToUSD = 0.90),
        CurrencyInfo(code = "PEN", symbol = "S/", name = "Sol Peruano", flagEmoji = "🇵🇪", rateToUSD = 3.75)
    )

    fun getCurrency(code: String): CurrencyInfo {
        return supportedCurrencies.find { it.code.equals(code, ignoreCase = true) }
            ?: supportedCurrencies.first()
    }

    /**
     * Convert amount from fromCurrency to toCurrency
     */
    fun convert(amount: Double, fromCode: String, toCode: String): Double {
        if (fromCode == toCode) return amount
        val from = getCurrency(fromCode)
        val to = getCurrency(toCode)
        val amountInUSD = amount / from.rateToUSD
        return amountInUSD * to.rateToUSD
    }

    fun format(amount: Double, currencyCode: String): String {
        val currency = getCurrency(currencyCode)
        val format = NumberFormat.getNumberInstance(Locale.US).apply {
            minimumFractionDigits = if (currency.code == "JPY" || currency.code == "COP" || currency.code == "CLP") 0 else 2
            maximumFractionDigits = if (currency.code == "JPY" || currency.code == "COP" || currency.code == "CLP") 0 else 2
        }
        return "${currency.symbol}${format.format(amount)} ${currency.code}"
    }

    fun formatCompact(amount: Double, currencyCode: String): String {
        val currency = getCurrency(currencyCode)
        val format = NumberFormat.getNumberInstance(Locale.US).apply {
            minimumFractionDigits = if (currency.code == "JPY" || currency.code == "COP" || currency.code == "CLP") 0 else 2
            maximumFractionDigits = if (currency.code == "JPY" || currency.code == "COP" || currency.code == "CLP") 0 else 2
        }
        return "${currency.symbol}${format.format(amount)}"
    }
}
