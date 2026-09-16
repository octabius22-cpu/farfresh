package com.farfresh.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
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
import com.farfresh.app.ui.viewmodel.PeriodFilter
import com.farfresh.app.ui.viewmodel.SaleListItem
import com.farfresh.app.ui.viewmodel.SalesViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesScreen(
    onSaleClick: (String) -> Unit,
    viewModel: SalesViewModel = viewModel()
) {
    val uiState by viewModel.salesState.collectAsStateWithLifecycle()
    val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    val dateFormat = SimpleDateFormat("dd/MM/yyyy h:mm a", Locale.getDefault())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ventas", fontWeight = FontWeight.Black) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Resumen del periodo
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    MetricColumn("Ventas", uiState.totalSales, MaterialTheme.colorScheme.primary)
                    MetricColumn("Cobrado", uiState.totalCollected, Color(0xFF4CAF50))
                    MetricColumn("Pendiente", uiState.totalPending, Color(0xFFF44336))
                }
            }

            // Filtros
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                PeriodFilter.entries.forEachIndexed { index, filter ->
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = PeriodFilter.entries.size),
                        onClick = { viewModel.selectedFilter = filter },
                        selected = viewModel.selectedFilter == filter
                    ) {
                        Text(filter.name.lowercase().replaceFirstChar { it.uppercase() })
                    }
                }
            }

            // Buscador
            OutlinedTextField(
                value = viewModel.searchQuery,
                onValueChange = { viewModel.searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("Buscar venta o cliente...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = MaterialTheme.shapes.medium
            )

            // Lista
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(uiState.sales) { item ->
                    SaleListCard(
                        item = item,
                        timeStr = if (viewModel.selectedFilter == PeriodFilter.HOY) 
                            timeFormat.format(Date(item.sale.timestamp)) 
                            else dateFormat.format(Date(item.sale.timestamp)),
                        onClick = { onSaleClick(item.sale.id) }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                }

                if (uiState.sales.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("No se encontraron ventas", color = MaterialTheme.colorScheme.outline)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MetricColumn(label: String, value: Double, color: Color) {
    val locale = Locale.getDefault()
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        Text(
            "S/ ${String.format(locale, "%.2f", value)}",
            fontWeight = FontWeight.Black,
            fontSize = 16.sp,
            color = color
        )
    }
}

@Composable
fun SaleListCard(item: SaleListItem, timeStr: String, onClick: () -> Unit) {
    val locale = Locale.getDefault()
    val statusColor = when(item.sale.status) {
        SaleStatus.PAGADA -> Color(0xFF4CAF50)
        SaleStatus.PENDIENTE -> Color(0xFFFF9800)
        SaleStatus.PARCIAL -> Color(0xFF2196F3)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(timeStr, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            Text(item.customerName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Text("${item.itemCount} productos", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
        
        Column(horizontalAlignment = Alignment.End) {
            Text("S/ ${String.format(locale, "%.2f", item.sale.totalAmount)}", fontWeight = FontWeight.Black, fontSize = 18.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Pagado: S/ ${String.format(locale, "%.2f", item.sale.paidAmount)}", style = MaterialTheme.typography.labelSmall)
                if (item.sale.pendingBalance > 0) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Debe: S/ ${String.format(locale, "%.2f", item.sale.pendingBalance)}", color = Color.Red, style = MaterialTheme.typography.labelSmall)
                }
            }
            Text(
                item.sale.status.name,
                color = statusColor,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
