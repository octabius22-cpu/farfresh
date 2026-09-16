package com.farfresh.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.farfresh.app.data.model.Customer
import com.farfresh.app.data.model.SaleStatus
import com.farfresh.app.ui.viewmodel.CustomerViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerDetailScreen(
    customerId: String,
    onBack: () -> Unit,
    onSaleClick: (String) -> Unit,
    viewModel: CustomerViewModel = viewModel()
) {
    val customerRepository = com.farfresh.app.data.repository.CustomerRepository()
    var customer by remember { mutableStateOf<Customer?>(null) }
    
    LaunchedEffect(customerId) {
        customer = customerRepository.getCustomerById(customerId)
    }

    val sales by viewModel.getCustomerSales(customerId).collectAsStateWithLifecycle(emptyList())
    val totalPending by viewModel.getCustomerPendingBalance(customerId).collectAsStateWithLifecycle(0.0)

    val dateFormat = SimpleDateFormat("dd/MM/yyyy h:mm a", Locale.getDefault())

    if (customer == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val currentCustomer = customer!!

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalle de Cliente", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(currentCustomer.name, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black)
                Text("DNI: ${currentCustomer.dni}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                if (currentCustomer.phone != null) {
                    Text("Tel: ${currentCustomer.phone}", style = MaterialTheme.typography.bodyLarge, color = Color.Gray)
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Total pendiente", style = MaterialTheme.typography.labelLarge)
                        Text(
                            "S/ ${String.format("%.2f", totalPending)}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            val pendingSales = sales.filter { it.pendingBalance > 0 }
            val paidSales = sales.filter { it.pendingBalance <= 0 }

            if (pendingSales.isNotEmpty()) {
                item {
                    Text("Ventas pendientes", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }
                items(pendingSales) { sale ->
                    SaleListItem(sale, dateFormat, onSaleClick)
                }
            }

            if (paidSales.isNotEmpty()) {
                item {
                    Text("Historial (Pagadas)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }
                items(paidSales) { sale ->
                    SaleListItem(sale, dateFormat, onSaleClick)
                }
            }
        }
    }
}

@Composable
fun SaleListItem(sale: com.farfresh.app.data.model.Sale, dateFormat: SimpleDateFormat, onClick: (String) -> Unit) {
    ListItem(
        headlineContent = { Text("Venta ${dateFormat.format(Date(sale.timestamp))}") },
        supportingContent = {
            Text("Total: S/ ${String.format("%.2f", sale.totalAmount)} | Pagado: S/ ${String.format("%.2f", sale.paidAmount)}")
        },
        trailingContent = {
            if (sale.pendingBalance > 0) {
                Text("Debe S/ ${String.format("%.2f", sale.pendingBalance)}", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
            } else {
                Text("PAGADA", color = Color(0xFF4CAF50), style = MaterialTheme.typography.labelSmall)
            }
        },
        modifier = Modifier.clickable { onClick(sale.id) }
    )
    HorizontalDivider()
}
