package com.example.swiftlab.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import com.example.swiftlab.data.local.entity.InvoiceEntity
import com.example.swiftlab.data.repository.AppRepository
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoicesScreen(
    repository: AppRepository,
    sessionManager: SessionManager
) {
    val invoices by repository.invoices.collectAsState(initial = emptyList())
    var selectedInvoiceForPayment by remember { mutableStateOf<InvoiceEntity?>(null) }

    val sym = sessionManager.currencySymbol

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Invoices & Billing", fontWeight = FontWeight.Bold) }
            )
        }
    ) { paddingValues ->
        if (invoices.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(bottom = 80.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No invoices generated yet. Create a sale to issue an invoice.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp)
            ) {
                items(invoices) { inv ->
                    InvoiceCard(
                        invoice = inv,
                        sym = sym,
                        onRecordPayment = { selectedInvoiceForPayment = inv }
                    )
                }
            }
        }
    }

    if (selectedInvoiceForPayment != null) {
        val inv = selectedInvoiceForPayment!!
        val balance = (inv.totalAmount - inv.paidAmount).coerceAtLeast(0.0)
        RecordPaymentDialog(
            invoice = inv,
            balance = balance,
            sym = sym,
            onDismiss = { selectedInvoiceForPayment = null },
            onConfirm = { amount, method, ref ->
                repository.recordPayment(
                    invoiceId = inv.id,
                    invoiceNumber = inv.invoiceNumber,
                    amount = amount,
                    method = method,
                    referenceNumber = ref
                )
                selectedInvoiceForPayment = null
            }
        )
    }
}

@Composable
fun InvoiceCard(
    invoice: InvoiceEntity,
    sym: String,
    onRecordPayment: () -> Unit
) {
    val balance = (invoice.totalAmount - invoice.paidAmount).coerceAtLeast(0.0)

    val (badgeBg, badgeFg) = when (invoice.status) {
        "PAID" -> Color(0xFFDCFCE7) to Color(0xFF16A34A)
        "PARTIALLY_PAID" -> Color(0xFFFEF3C7) to Color(0xFFD97706)
        else -> Color(0xFFFEE2E2) to Color(0xFFDC2626)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = invoice.invoiceNumber,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Surface(
                    color = badgeBg,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = invoice.status,
                        color = badgeFg,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Text(
                text = "${invoice.customerName} • Issue: ${invoice.issueDate}",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Total: $sym${String.format(Locale.US, "%.2f", invoice.totalAmount)}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Balance: $sym${String.format(Locale.US, "%.2f", balance)}",
                        fontSize = 13.sp,
                        color = if (balance > 0) Color(0xFFDC2626) else Color(0xFF16A34A)
                    )
                }

                if (balance > 0) {
                    Button(
                        onClick = onRecordPayment,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("Pay", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun RecordPaymentDialog(
    invoice: InvoiceEntity,
    balance: Double,
    sym: String,
    onDismiss: () -> Unit,
    onConfirm: suspend (amount: Double, method: String, ref: String) -> Unit
) {
    var amountText by remember { mutableStateOf(String.format(Locale.US, "%.2f", balance)) }
    var method by remember { mutableStateOf("CASH") }
    var reference by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record Payment for ${invoice.invoiceNumber}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Outstanding Balance: $sym${String.format(Locale.US, "%.2f", balance)}")

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Payment Amount ($sym)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("CASH", "UPI", "CARD").forEach { m ->
                        FilterChip(
                            selected = method == m,
                            onClick = { method = m },
                            label = { Text(m) }
                        )
                    }
                }

                OutlinedTextField(
                    value = reference,
                    onValueChange = { reference = it },
                    label = { Text("Reference / Txn ID (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val amt = amountText.toDoubleOrNull() ?: 0.0
                if (amt > 0) {
                    coroutineScope.launch {
                        onConfirm(amt, method, reference.trim())
                    }
                }
            }) {
                Text("Confirm Payment")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
