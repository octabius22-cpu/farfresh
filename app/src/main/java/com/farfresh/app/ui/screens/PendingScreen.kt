package com.farfresh.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.farfresh.app.data.model.SaleStatus
import com.farfresh.app.ui.components.SummaryMini
import com.farfresh.app.ui.viewmodel.PendingViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PendingScreen(
    onSaleDetailClick: (String) -> Unit,
    onCustomersClick: () -> Unit,
    viewModel: PendingViewModel = viewModel()
) {
    val pendingSales by viewModel.pendingSales.collectAsStateWithLifecycle()
    val totalPending by viewModel.totalPendingBalance.collectAsStateWithLifecycle()
    val debtorCount by viewModel.debtorCount.collectAsStateWithLifecycle()
    val saleCount by viewModel.pendingSaleCount.collectAsStateWithLifecycle()
    
    val snackbarHostState = remember { SnackbarHostState() }
    val dateFormat = SimpleDateFormat("dd/MM/yyyy h:mm a", Locale.getDefault())

    LaunchedEffect(viewModel.saleMessage) {
        viewModel.saleMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.saleMessage = null
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Pendientes", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onCustomersClick) {
                        Icon(Icons.Default.Group, contentDescription = "Clientes")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Resumen de Pendientes
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Total por cobrar", style = MaterialTheme.typography.labelLarge)
                    Text(
                        "S/ ${String.format(Locale.getDefault(), "%.2f", totalPending)}",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        SummaryMini(label = "Clientes", value = debtorCount.toString())
                        SummaryMini(label = "Ventas", value = saleCount.toString())
                    }
                }
            }

            // Buscador
            OutlinedTextField(
                value = viewModel.searchQuery,
                onValueChange = { viewModel.searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                placeholder = { Text("Buscar cliente...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = MaterialTheme.shapes.medium
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Lista
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(pendingSales) { item ->
                    PendingSaleCard(
                        customerName = item.customerName,
                        amount = item.sale.pendingBalance,
                        date = dateFormat.format(Date(item.sale.timestamp)),
                        status = item.sale.status,
                        onClick = { onSaleDetailClick(item.sale.id) }
                    )
                }
                
                if (pendingSales.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("No hay cobros pendientes", color = MaterialTheme.colorScheme.outline)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PendingSaleCard(
    customerName: String,
    amount: Double,
    date: String,
    status: SaleStatus,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(customerName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text(date, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                
                val statusColor = if (status == SaleStatus.PENDIENTE) Color(0xFFFF9800) else Color(0xFF2196F3)
                Text(
                    status.name,
                    color = statusColor,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text("Debe", style = MaterialTheme.typography.labelSmall)
                Text(
                    "S/ ${String.format(Locale.getDefault(), "%.2f", amount)}",
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.error
                )
                
                Button(
                    onClick = onClick,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    modifier = Modifier.height(32.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Pagar", fontSize = 12.sp)
                }
            }
        }
    }
}
