package com.example.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

object CategoryIconHelper {

    fun getIcon(iconName: String): ImageVector {
        return when (iconName) {
            "ShoppingCart" -> Icons.Default.ShoppingCart
            "Restaurant" -> Icons.Default.Restaurant
            "Home" -> Icons.Default.Home
            "Bolt" -> Icons.Default.Bolt
            "DirectionsCar" -> Icons.Default.DirectionsCar
            "Movie" -> Icons.Default.Movie
            "LocalHospital" -> Icons.Default.LocalHospital
            "School" -> Icons.Default.School
            "ShoppingBag" -> Icons.Default.ShoppingBag
            "Subscriptions" -> Icons.Default.Subscriptions
            "Flight" -> Icons.Default.Flight
            "Payments" -> Icons.Default.Payments
            "Work" -> Icons.Default.Work
            "TrendingUp" -> Icons.AutoMirrored.Filled.TrendingUp
            "AttachMoney" -> Icons.Default.AttachMoney
            else -> Icons.Default.Category
        }
    }

    fun parseColor(hex: String, defaultColor: Color = Color(0xFF10B981)): Color {
        return try {
            val cleanHex = if (hex.startsWith("#")) hex else "#$hex"
            Color(android.graphics.Color.parseColor(cleanHex))
        } catch (e: Exception) {
            defaultColor
        }
    }
}
