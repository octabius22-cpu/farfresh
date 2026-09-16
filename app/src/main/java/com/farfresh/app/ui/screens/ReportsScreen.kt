package com.farfresh.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.farfresh.app.ui.components.SummaryMini
import com.farfresh.app.ui.viewmodel.ReportPeriod
import com.farfresh.app.ui.viewmodel.ReportViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    viewModel: ReportViewModel = viewModel()
) {
    val uiState by viewModel.reportState.collectAsStateWithLifecycle()
    var showDatePicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reportes", fontWeight = FontWeight.Black) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Selector de Período
            ScrollableTabRow(
                selectedTabIndex = viewModel.selectedPeriod.ordinal,
                edgePadding = 16.dp,
                divider = {}
            ) {
                ReportPeriod.entries.forEach { period ->
                    Tab(
                        selected = viewModel.selectedPeriod == period,
                        onClick = { 
                            viewModel.selectedPeriod = period
                            if (period == ReportPeriod.PERSONALIZADO) {
                                showDatePicker = true
                            }
                        },
                        text = { Text(period.name.lowercase().replace("_", " ").replaceFirstChar { it.uppercase() }) }
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (!uiState.hasData && viewModel.selectedPeriod != ReportPeriod.PERSONALIZADO) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("No hay datos para este período", color = Color.Gray)
                        }
                    }
                }

                item {
                    ReportSummarySection(uiState)
                }

                item {
                    PaymentMethodsSection(uiState)
                }

                item {
                    InventoryReportSection(uiState)
                }

                item {
                    CustomerReportSection(uiState)
                }

                if (uiState.topSold.isNotEmpty()) {
                    item {
                        Text("Productos más vendidos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    items(uiState.topSold.take(5)) { product ->
                        ProductReportCard(product)
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        // Por simplicidad en este paso, usaremos un diálogo básico o simularemos la selección.
        // En una app real usaríamos DateRangePicker de M3.
        AlertDialog(
            onDismissRequest = { showDatePicker = false },
            title = { Text("Seleccionar Rango") },
            text = { Text("Módulo de selección de fechas próximamente.") },
            confirmButton = { TextButton(onClick = { showDatePicker = false }) { Text("OK") } }
        )
    }
}

@Composable
fun ReportSummarySection(state: com.farfresh.app.ui.viewmodel.ReportUiState) {
    val locale = Locale.getDefault()
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            ReportCard("Ventas", "S/ ${String.format(locale, "%.2f", state.totalVentas)}", MaterialTheme.colorScheme.primary, Modifier.weight(1f))
            Spacer(modifier = Modifier.width(12.dp))
            ReportCard("Cobrado", "S/ ${String.format(locale, "%.2f", state.totalCobrado)}", Color(0xFF4CAF50), Modifier.weight(1f))
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            ReportCard("Por cobrar", "S/ ${String.format(locale, "%.2f", state.porCobrar)}", Color(0xFFF44336), Modifier.weight(1f))
            Spacer(modifier = Modifier.width(12.dp))
            ReportCard("Ganancia", "S/ ${String.format(locale, "%.2f", state.totalGanancia)}", Color(0xFFFF9800), Modifier.weight(1f))
        }
        
        Card(modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                SummaryMini(label = "Ventas", value = state.saleCount.toString())
                SummaryMini(label = "Unidades", value = state.unitsSold.toInt().toString())
            }
        }
    }
}

@Composable
fun PaymentMethodsSection(state: com.farfresh.app.ui.viewmodel.ReportUiState) {
    val locale = Locale.getDefault()
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Detalle de Cobros", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(8.dp))
            ReportRow("Efectivo", state.cash)
            ReportRow("Yape", state.yape)
            ReportRow("Otros", state.other)
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Total Cobrado", fontWeight = FontWeight.Bold)
                Text("S/ ${String.format(locale, "%.2f", state.totalCobrado)}", fontWeight = FontWeight.Black, color = Color(0xFF4CAF50))
            }
        }
    }
}

@Composable
fun InventoryReportSection(state: com.farfresh.app.ui.viewmodel.ReportUiState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Inventario", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Productos activos")
                Text("${state.activeProducts}", fontWeight = FontWeight.Bold)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Stock bajo", color = Color(0xFFFF9800))
                Text("${state.lowStockCount}", fontWeight = FontWeight.Bold, color = Color(0xFFFF9800))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Sin stock", color = Color.Red)
                Text("${state.noStockCount}", fontWeight = FontWeight.Bold, color = Color.Red)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("Movimientos del período", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                SummaryMini(label = "Entradas", value = state.inventoryEntries.toString())
                SummaryMini(label = "Salidas", value = state.inventoryExits.toString())
                SummaryMini(label = "Ajustes", value = state.inventoryAdjustments.toString())
            }
        }
    }
}

@Composable
fun CustomerReportSection(state: com.farfresh.app.ui.viewmodel.ReportUiState) {
    val locale = Locale.getDefault()
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Clientes", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Registrados")
                Text("${state.registeredCustomers}", fontWeight = FontWeight.Bold)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Con deuda")
                Text("${state.debtorsCount}", fontWeight = FontWeight.Bold, color = Color.Red)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Total por cobrar (Hoy)")
                Text("S/ ${String.format(locale, "%.2f", state.totalToCollectAll)}", fontWeight = FontWeight.Black, color = Color.Red)
            }
        }
    }
}

@Composable
fun ReportCard(title: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.height(90.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = title, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            Text(text = value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = color)
        }
    }
}

@Composable
fun ReportRow(label: String, amount: Double) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text("S/ ${String.format(Locale.getDefault(), "%.2f", amount)}", fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ProductReportCard(item: com.farfresh.app.ui.viewmodel.ProductReportItem) {
    val locale = Locale.getDefault()
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(item.name, fontWeight = FontWeight.Bold)
                Text("Vendidos: ${item.quantity.toInt()} unidades", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("Total: S/ ${String.format(locale, "%.2f", item.amount)}", fontWeight = FontWeight.Bold)
                Text("Ganancia: S/ ${String.format(locale, "%.2f", item.profit)}", color = Color(0xFFFF9800), fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}
