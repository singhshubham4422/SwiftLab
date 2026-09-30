package com.example.swiftlab.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.swiftlab.data.local.SessionManager
import com.example.swiftlab.data.local.entity.SaleEntity
import com.example.swiftlab.data.repository.AppRepository
import com.example.swiftlab.ui.navigation.Screen
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    repository: AppRepository,
    sessionManager: SessionManager,
    onNavigate: (String) -> Unit
) {
    val sales by repository.sales.collectAsState(initial = emptyList())
    val products by repository.products.collectAsState(initial = emptyList())
    val invoices by repository.invoices.collectAsState(initial = emptyList())
    val pendingSync by repository.pendingSyncCount.collectAsState(initial = 0)

    val totalRevenue = sales.sumOf { it.totalAmount }
    val sym = sessionManager.currencySymbol

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = sessionManager.orgName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "Logged in as: ${sessionManager.userName}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    // Data Mode Badge
                    Surface(
                        color = if (sessionManager.isLocalOnly) Color(0xFF10B981) else Color(0xFF2563EB),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Text(
                            text = if (sessionManager.isLocalOnly) "LOCAL_ONLY" else "CLOUD_SYNC",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                // Quick Summary Cards in a 2x2 Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricCard(
                        title = "Total Sales",
                        value = "$sym${String.format(Locale.US, "%.2f", totalRevenue)}",
                        icon = Icons.Default.TrendingUp,
                        color = Color(0xFF2563EB),
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Invoices",
                        value = "${invoices.size}",
                        icon = Icons.Default.Receipt,
                        color = Color(0xFF0D9488),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricCard(
                        title = "Products",
                        value = "${products.size}",
                        icon = Icons.Default.Inventory2,
                        color = Color(0xFF8B5CF6),
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Pending Sync",
                        value = if (sessionManager.isLocalOnly) "Off" else "$pendingSync",
                        icon = Icons.Default.Sync,
                        color = if (pendingSync > 0) Color(0xFFF59E0B) else Color(0xFF6B7280),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Quick Actions
            item {
                Text(
                    text = "Quick Actions",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onNavigate(Screen.Sales.route) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.AddShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("New Sale", fontSize = 13.sp)
                    }

                    OutlinedButton(
                        onClick = { onNavigate(Screen.Products.route) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Item", fontSize = 13.sp)
                    }
                }
            }

            // Recent Sales section
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Sales",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    TextButton(onClick = { onNavigate(Screen.Invoices.route) }) {
                        Text("View Invoices")
                    }
                }
            }

            if (sales.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Text(
                            text = "No sales recorded yet. Tap 'New Sale' to start POS checkout.",
                            modifier = Modifier.padding(16.dp),
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(sales.take(5)) { sale ->
                    SaleItemRow(sale, sym)
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp)) // padding for bottom bar
            }
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun SaleItemRow(sale: SaleEntity, sym: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = sale.saleNumber, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(
                    text = "${sale.customerName} • ${sale.createdAt}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$sym${String.format(Locale.US, "%.2f", sale.totalAmount)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = sale.status,
                    fontSize = 11.sp,
                    color = if (sale.paidAmount >= sale.totalAmount) Color(0xFF10B981) else Color(0xFFF59E0B)
                )
            }
        }
    }
}
