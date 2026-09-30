package com.example.swiftlab.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
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
import com.example.swiftlab.data.repository.AppRepository
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductsScreen(
    repository: AppRepository,
    sessionManager: SessionManager
) {
    val products by repository.products.collectAsState(initial = emptyList())
    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }

    val filtered = products.filter {
        it.name.contains(searchQuery, ignoreCase = true) || it.sku.contains(searchQuery, ignoreCase = true)
    }

    val sym = sessionManager.currencySymbol

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Product Catalog", fontWeight = FontWeight.Bold) }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Product")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search by name or SKU") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )

            if (filtered.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (searchQuery.isBlank()) "No products yet. Tap '+' to create one." else "No products matching '$searchQuery'",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(filtered) { product ->
                        ProductCard(product, sym)
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddProductDialog(
            sym = sym,
            onDismiss = { showAddDialog = false },
            onConfirm = { sku, name, desc, cost, sell, stock ->
                repository.createProduct(sku, name, desc, cost, sell, stock)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun ProductCard(product: ProductEntity, sym: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = product.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(
                    text = "SKU: ${product.sku}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Cost: $sym${String.format(Locale.US, "%.2f", product.costPrice)}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Sell: $sym${String.format(Locale.US, "%.2f", product.sellingPrice)}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Stock Pill
            Surface(
                color = if (product.currentStock <= 5) Color(0xFFFEE2E2) else Color(0xFFDCFCE7),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "${product.currentStock.toInt()}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = if (product.currentStock <= 5) Color(0xFFDC2626) else Color(0xFF16A34A)
                    )
                    Text(
                        text = "in stock",
                        fontSize = 10.sp,
                        color = if (product.currentStock <= 5) Color(0xFFDC2626) else Color(0xFF16A34A)
                    )
                }
            }
        }
    }
}

@Composable
fun AddProductDialog(
    sym: String,
    onDismiss: () -> Unit,
    onConfirm: suspend (sku: String, name: String, desc: String, cost: Double, sell: Double, stock: Double) -> Unit
) {
    var sku by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var costPrice by remember { mutableStateOf("") }
    var sellingPrice by remember { mutableStateOf("") }
    var initialStock by remember { mutableStateOf("") }

    val coroutineScope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Product", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Product Name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = sku,
                    onValueChange = { sku = it },
                    label = { Text("SKU * (e.g. PRD-001)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = costPrice,
                        onValueChange = { costPrice = it },
                        label = { Text("Cost ($sym)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = sellingPrice,
                        onValueChange = { sellingPrice = it },
                        label = { Text("Price ($sym) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = initialStock,
                    onValueChange = { initialStock = it },
                    label = { Text("Initial Stock Qty") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && sku.isNotBlank()) {
                        val cost = costPrice.toDoubleOrNull() ?: 0.0
                        val sell = sellingPrice.toDoubleOrNull() ?: 0.0
                        val stock = initialStock.toDoubleOrNull() ?: 0.0
                        coroutineScope.launch {
                            onConfirm(sku.trim(), name.trim(), desc.trim(), cost, sell, stock)
                        }
                    }
                },
                enabled = name.isNotBlank() && sku.isNotBlank()
            ) {
                Text("Save Product")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
