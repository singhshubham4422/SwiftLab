package com.example.swiftlab.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.swiftlab.data.local.SessionManager
import com.example.swiftlab.data.local.entity.PaymentEntity
import com.example.swiftlab.data.repository.AppRepository
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentsScreen(
    repository: AppRepository,
    sessionManager: SessionManager
) {
    val payments by repository.payments.collectAsState(initial = emptyList())
    val sym = sessionManager.currencySymbol

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Payment Transactions", fontWeight = FontWeight.Bold) }
            )
        }
    ) { paddingValues ->
        if (payments.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(bottom = 80.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No payments recorded yet.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp)
            ) {
                items(payments) { p ->
                    PaymentCard(p, sym)
                }
            }
        }
    }
}

@Composable
fun PaymentCard(payment: PaymentEntity, sym: String) {
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
            Column {
                Text(payment.paymentNumber, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(
                    text = "Invoice: ${payment.invoiceNumber.ifBlank { "N/A" }} • ${payment.paymentDate}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (payment.referenceNumber.isNotBlank()) {
                    Text(
                        text = "Ref: ${payment.referenceNumber}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "+$sym${String.format(Locale.US, "%.2f", payment.amount)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color(0xFF16A34A)
                )
                Text(
                    text = payment.method,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
