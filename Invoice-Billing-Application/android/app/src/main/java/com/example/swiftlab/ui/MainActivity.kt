package com.example.swiftlab.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.swiftlab.SwiftLabApp
import com.example.swiftlab.data.local.SessionManager
import com.example.swiftlab.data.remote.RetrofitClient
import com.example.swiftlab.data.repository.AppRepository
import com.example.swiftlab.sync.SyncManager
import com.example.swiftlab.ui.navigation.Screen
import com.example.swiftlab.ui.navigation.bottomNavItems
import com.example.swiftlab.ui.screens.*
import com.example.swiftlab.ui.theme.SwiftLabTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as SwiftLabApp
        val sessionManager = SessionManager(this)
        val retrofitClient = RetrofitClient(sessionManager)
        val repository = AppRepository(app.database, sessionManager, retrofitClient)
        val syncManager = SyncManager(this)

        if (sessionManager.isCloudSync) {
            syncManager.schedulePeriodicSync()
        }

        setContent {
            SwiftLabTheme {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                val showBottomBar = currentRoute != null && currentRoute != Screen.Login.route

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        if (showBottomBar) {
                            NavigationBar {
                                bottomNavItems.forEach { screen ->
                                    NavigationBarItem(
                                        icon = {
                                            screen.icon?.let {
                                                Icon(it, contentDescription = screen.title)
                                            }
                                        },
                                        label = { Text(screen.title) },
                                        selected = currentRoute == screen.route,
                                        onClick = {
                                            if (currentRoute != screen.route) {
                                                navController.navigate(screen.route) {
                                                    popUpTo(Screen.Dashboard.route) {
                                                        saveState = true
                                                    }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = if (sessionManager.isLoggedIn || sessionManager.isLocalOnly) Screen.Dashboard.route else Screen.Login.route,
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable(Screen.Login.route) {
                            LoginScreen(
                                sessionManager = sessionManager,
                                retrofitClient = retrofitClient,
                                onLoginSuccess = {
                                    navController.navigate(Screen.Dashboard.route) {
                                        popUpTo(Screen.Login.route) { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable(Screen.Dashboard.route) {
                            DashboardScreen(
                                repository = repository,
                                sessionManager = sessionManager,
                                onNavigate = { route -> navController.navigate(route) }
                            )
                        }

                        composable(Screen.Products.route) {
                            ProductsScreen(
                                repository = repository,
                                sessionManager = sessionManager
                            )
                        }

                        composable(Screen.Inventory.route) {
                            InventoryScreen(
                                repository = repository,
                                sessionManager = sessionManager
                            )
                        }

                        composable(Screen.Sales.route) {
                            SalesScreen(
                                repository = repository,
                                sessionManager = sessionManager
                            )
                        }

                        composable(Screen.Invoices.route) {
                            InvoicesScreen(
                                repository = repository,
                                sessionManager = sessionManager
                            )
                        }

                        composable(Screen.Customers.route) {
                            CustomersSuppliersScreen(
                                repository = repository,
                                sessionManager = sessionManager
                            )
                        }

                        composable(Screen.Payments.route) {
                            PaymentsScreen(
                                repository = repository,
                                sessionManager = sessionManager
                            )
                        }

                        composable(Screen.Settings.route) {
                            SettingsScreen(
                                repository = repository,
                                sessionManager = sessionManager,
                                syncManager = syncManager,
                                retrofitClient = retrofitClient,
                                onLogout = {
                                    navController.navigate(Screen.Login.route) {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
