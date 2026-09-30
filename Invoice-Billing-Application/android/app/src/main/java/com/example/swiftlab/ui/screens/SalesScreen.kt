package com.example.swiftlab.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ShoppingBag
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
fun SalesScreen(
    repository: AppRepository,
    sessionManager: SessionManager
) {
    val products by repository.products.collectAsState(initial = emptyList())
    val customers by repository.customers.collectAsState(initial = emptyList())

    var customerName by remember { mutableStateOf("Walk-in Customer") }
    var selectedCustomerId by remember { mutableStateOf<Long?>(null) }
    var paymentMethod by remember { mutableStateOf("CASH") }

    val cartItems = remember { mutableStateListOf<AppRepository.SaleItemInput>() }
    var showProductPicker by remember { mutableStateOf(false) }
    var saleSuccessMessage by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()
    val sym = sessionManager.currencySymbol

    val subtotal = cartItems.sumOf { it.quantity * it.unitPrice }
    var paidAmountText by remember(subtotal) {
        mutableStateOf(String.format(Locale.US, "%.2f", subtotal))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Point of Sale (POS)", fontWeight = FontWeight.Bold) }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            if (saleSuccessMessage != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFDCFCE7)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    Text(
                        text = saleSuccessMessage!!,
                        color = Color(0xFF16A34A),
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // Customer selection
            OutlinedTextField(
                value = customerName,
                onValueChange = {
                    customerName = it
                    selectedCustomerId = null
                },
                label = { Text("Customer Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Cart Header & Add Item Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Items (${cartItems.size})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Button(
                    onClick = { showProductPicker = true },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Item")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Cart Items List
            if (cartItems.isEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.ShoppingBag, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Cart is empty", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(cartItems) { item ->
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
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = item.product.name, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        text = "${item.quantity.toInt()} x $sym${String.format(Locale.US, "%.2f", item.unitPrice)}",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = "$sym${String.format(Locale.US, "%.2f", item.quantity * item.unitPrice)}",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                IconButton(onClick = { cartItems.remove(item) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Payment summary & checkout controls
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 80.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total Amount:", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(
                            "$sym${String.format(Locale.US, "%.2f", subtotal)}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Payment Method selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("CASH", "UPI", "CARD").forEach { method ->
                            FilterChip(
                                selected = paymentMethod == method,
                                onClick = { paymentMethod = method },
                                label = { Text(method) }
                            )
                        }
                    }

                    Button(
                        onClick = {
                            if (cartItems.isNotEmpty()) {
                                val paid = paidAmountText.toDoubleOrNull() ?: subtotal
                                coroutineScope.launch {
                                    val saleId = repository.recordSale(
                                        customerId = selectedCustomerId,
                                        customerName = customerName.ifBlank { "Walk-in Customer" },
                                        items = cartItems.toList(),
                                        paymentMethod = paymentMethod,
                                        paidAmount = paid
                                    )
                                    saleSuccessMessage = "Sale #$saleId completed! Stock depleted & invoice generated."
                                    cartItems.clear()
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        enabled = cartItems.isNotEmpty()
                    ) {
                        Text("Complete Sale & Invoicing", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showProductPicker) {
        AlertDialog(
            onDismissRequest = { showProductPicker = false },
            title = { Text("Select Product") },
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                    items(products) { prod ->
                        Card(
                            onClick = {
                                cartItems.add(AppRepository.SaleItemInput(prod, 1.0, prod.sellingPrice))
                                showProductPicker = false
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(prod.name, fontWeight = FontWeight.SemiBold)
                                    Text("Stock: ${prod.currentStock.toInt()}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text("$sym${String.format(Locale.US, "%.2f", prod.sellingPrice)}", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showProductPicker = false }) { Text("Close") }
            }
        )
    }
}
