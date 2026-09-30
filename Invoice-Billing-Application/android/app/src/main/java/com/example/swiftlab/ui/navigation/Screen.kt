package com.example.swiftlab.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector? = null) {
    object Login : Screen("login", "Login")
    object Dashboard : Screen("dashboard", "Dashboard", Icons.Default.Dashboard)
    object Products : Screen("products", "Products", Icons.Default.Inventory2)
    object Inventory : Screen("inventory", "Inventory", Icons.Default.Warehouse)
    object Sales : Screen("sales", "Point of Sale", Icons.Default.PointOfSale)
    object Invoices : Screen("invoices", "Invoices", Icons.Default.Receipt)
    object Customers : Screen("customers", "Customers", Icons.Default.People)
    object Payments : Screen("payments", "Payments", Icons.Default.Payment)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
}

val bottomNavItems = listOf(
    Screen.Dashboard,
    Screen.Products,
    Screen.Sales,
    Screen.Invoices,
    Screen.Settings
)
