package com.farfresh.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.farfresh.app.ui.viewmodel.DashboardViewModel
import com.farfresh.app.ui.viewmodel.SaleDisplayItem
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNewSaleClick: () -> Unit,
    onSeeAllPendingClick: () -> Unit,
    onSaleClick: (String) -> Unit,
    onMenuClick: () -> Unit, // Añadido
    viewModel: DashboardViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val todayDate = SimpleDateFormat("EEEE, d 'de' MMMM", Locale("es", "PE")).format(Date())

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Mi Tienda", fontWeight = FontWeight.Black)
                        Text(
                            text = todayDate.replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                },
                navigationIcon = { // Añadido Icono de Menú
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Default.Menu, contentDescription = "Menú")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White // Asegurar visibilidad
                )
            )
        }
    ) { padding ->
        if (!uiState.hasSales) {
            EmptyDashboard(padding, onNewSaleClick)
        } else {
            DashboardContent(padding, uiState, onNewSaleClick, onSeeAllPendingClick, onSaleClick)
        }
    }
}

@Composable
fun EmptyDashboard(padding: PaddingValues, onNewSaleClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "Todavía no tienes ventas registradas",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.outline
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onNewSaleClick) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Registrar primera venta")
            }
        }
    }
}

@Composable
fun DashboardContent(
    padding: PaddingValues,
    uiState: com.farfresh.app.ui.viewmodel.DashboardUiState,
    onNewSaleClick: () -> Unit,
    onSeeAllPendingClick: () -> Unit,
    onSaleClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "Resumen de hoy",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Resumen principal
        Row(modifier = Modifier.fillMaxWidth()) {
            SummaryCard(
                title = "Ventas de hoy",
                value = "S/ ${String.format("%.2f", uiState.dailyVentas)}",
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(12.dp))
            SummaryCard(
                title = "Cobrado hoy",
                value = "S/ ${String.format("%.2f", uiState.dailyCobrado)}",
                color = Color(0xFF4CAF50),
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            SummaryCard(
                title = "Por cobrar",
                value = "S/ ${String.format("%.2f", uiState.totalPendiente)}",
                color = Color(0xFFF44336),
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(12.dp))
            SummaryCard(
                title = "Ganancia hoy",
                value = "S/ ${String.format("%.2f", uiState.dailyProfit)}",
                color = Color(0xFFFF9800),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Botón Nueva Venta
        Button(
            onClick = onNewSaleClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("NUEVA VENTA", fontWeight = FontWeight.Black, fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Pagos de hoy
        Text(
            text = "Pagos de hoy",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                PaymentMethodRow("Efectivo", uiState.cashToday)
                PaymentMethodRow("Yape", uiState.yapeToday)
                PaymentMethodRow("Otros", uiState.otherToday)
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Total cobrado", fontWeight = FontWeight.Bold)
                    Text(
                        "S/ ${String.format("%.2f", uiState.dailyCobrado)}",
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF4CAF50)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Últimas Ventas
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Últimas ventas",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Column {
                uiState.latestSales.forEachIndexed { index, item ->
                    LatestSaleItem(item, onSaleClick)
                    if (index < uiState.latestSales.size - 1) HorizontalDivider()
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Pendientes
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Pendientes",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            TextButton(onClick = onSeeAllPendingClick) {
                Text("Ver todos")
                Icon(Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(16.dp))
            }
        }
        
        if (uiState.recentPending.isEmpty()) {
            Text(
                "No tienes pagos pendientes.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        } else {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Column {
                    uiState.recentPending.forEachIndexed { index, item ->
                        ListItem(
                            headlineContent = { Text(item.customerName, fontWeight = FontWeight.Bold) },
                            supportingContent = { Text("Debe S/ ${String.format("%.2f", item.pendingAmount)}", color = Color(0xFFF44336)) },
                            modifier = Modifier.clickable { onSaleClick(item.saleId) }
                        )
                        if (index < uiState.recentPending.size - 1) HorizontalDivider()
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun SummaryCard(title: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.height(100.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = title, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = color
            )
        }
    }
}

@Composable
fun PaymentMethodRow(label: String, amount: Double) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text("S/ ${String.format("%.2f", amount)}", fontWeight = FontWeight.Bold)
    }
}

@Composable
fun LatestSaleItem(item: SaleDisplayItem, onClick: (String) -> Unit) {
    val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    val statusColor = when(item.sale.status) {
        SaleStatus.PAGADA -> Color(0xFF4CAF50)
        SaleStatus.PENDIENTE -> Color(0xFFFF9800)
        SaleStatus.PARCIAL -> Color(0xFF2196F3)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(item.sale.id) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(timeFormat.format(Date(item.sale.timestamp)), style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            Text(item.customerName, fontWeight = FontWeight.Bold)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("S/ ${String.format("%.2f", item.sale.totalAmount)}", fontWeight = FontWeight.Black)
            Text(item.sale.status.name, color = statusColor, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        }
    }
}
