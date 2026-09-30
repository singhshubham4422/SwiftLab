package com.example.swiftlab.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.swiftlab.data.local.SessionManager
import com.example.swiftlab.data.local.entity.ProductEntity
import com.example.swiftlab.data.local.entity.StockMovementEntity
import com.example.swiftlab.data.repository.AppRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    repository: AppRepository,
    sessionManager: SessionManager
) {
    val products by repository.products.collectAsState(initial = emptyList())
    val movements by repository.stockMovements.collectAsState(initial = emptyList())
    var selectedTab by remember { mutableIntStateOf(0) }
    var showAdjustDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Inventory Management", fontWeight = FontWeight.Bold) }
            )
        },
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(
                    onClick = { showAdjustDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Adjust Stock")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Stock Balances") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Movement Ledger") }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (selectedTab == 0) {
                // Stock Balances
                if (products.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize().padding(bottom = 80.dp), contentAlignment = Alignment.Center) {
                        Text("No inventory tracked yet. Add products to see stock balances.")
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        items(products) { prod ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(prod.name, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                                        Text("SKU: ${prod.sku}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Surface(
                                        color = if (prod.currentStock <= 5) Color(0xFFFEE2E2) else Color(0xFFDCFCE7),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = "${prod.currentStock.toInt()} units",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = if (prod.currentStock <= 5) Color(0xFFDC2626) else Color(0xFF16A34A),
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Movement Ledger
                if (movements.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize().padding(bottom = 80.dp), contentAlignment = Alignment.Center) {
                        Text("No stock movements recorded yet.")
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        items(movements) { m ->
                            MovementRow(m)
                        }
                    }
                }
            }
        }
    }

    if (showAdjustDialog) {
        StockAdjustDialog(
            products = products,
            onDismiss = { showAdjustDialog = false },
            onConfirm = { prodId, type, qty, reason ->
                repository.adjustStock(prodId, type, qty, reason)
                showAdjustDialog = false
            }
        )
    }
}

@Composable
fun MovementRow(m: StockMovementEntity) {
    val isPositive = m.type in listOf("STOCK_IN", "PURCHASE", "RETURN")
    val color = if (isPositive) Color(0xFF16A34A) else Color(0xFFDC2626)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(m.productName, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(
                    "${m.type} • ${m.referenceType.ifBlank { "N/A" }}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(m.createdAt, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${if (isPositive) "+" else "-"}${m.quantity.toInt()}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = color
                )
                Text(
                    text = "Bal: ${m.balanceAfter.toInt()}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun StockAdjustDialog(
    products: List<ProductEntity>,
    onDismiss: () -> Unit,
    onConfirm: suspend (productId: Long, type: String, quantity: Double, reason: String) -> Unit
) {
    var selectedProductId by remember { mutableStateOf(products.firstOrNull()?.id ?: 0L) }
    var type by remember { mutableStateOf("STOCK_IN") }
    var quantityText by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("Manual Replenishment") }

    val coroutineScope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Stock In / Adjust Inventory") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Select Product:", fontSize = 13.sp)
                LazyColumn(modifier = Modifier.heightIn(max = 140.dp)) {
                    items(products) { p ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            RadioButton(
                                selected = selectedProductId == p.id,
                                onClick = { selectedProductId = p.id }
                            )
                            Text("${p.name} (Stock: ${p.currentStock.toInt()})", fontSize = 14.sp)
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = type == "STOCK_IN",
                        onClick = { type = "STOCK_IN" },
                        label = { Text("Stock In (+)") }
                    )
                    FilterChip(
                        selected = type == "STOCK_OUT",
                        onClick = { type = "STOCK_OUT" },
                        label = { Text("Stock Out (-)") }
                    )
                }

                OutlinedTextField(
                    value = quantityText,
                    onValueChange = { quantityText = it },
                    label = { Text("Quantity") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Reason / Reference") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val qty = quantityText.toDoubleOrNull() ?: 0.0
                if (qty > 0 && selectedProductId > 0) {
                    coroutineScope.launch {
                        onConfirm(selectedProductId, type, qty, reason.trim())
                    }
                }
            }) {
                Text("Submit Adjustment")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
