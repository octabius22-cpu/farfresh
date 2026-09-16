package com.farfresh.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FilterList
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
import com.farfresh.app.data.model.StockMovementType
import com.farfresh.app.ui.viewmodel.InventoryViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockHistoryScreen(
    productId: String? = null,
    onBack: () -> Unit,
    viewModel: InventoryViewModel = viewModel()
) {
    val movements by viewModel.getMovements(productId).collectAsStateWithLifecycle()
    val dateFormat = SimpleDateFormat("dd/MM/yyyy h:mm a", Locale.getDefault())
    
    var filterType by remember { mutableStateOf<StockMovementType?>(null) }
    
    val filteredMovements = if (filterType == null) movements 
                            else movements.filter { it.type == filterType }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Historial de Stock", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar")
                    }
                },
                actions = {
                    var showFilterMenu by remember { mutableStateOf(false) }
                    IconButton(onClick = { showFilterMenu = true }) {
                        Icon(Icons.Default.FilterList, contentDescription = "Filtrar")
                    }
                    DropdownMenu(expanded = showFilterMenu, onDismissRequest = { showFilterMenu = false }) {
                        DropdownMenuItem(text = { Text("Todos") }, onClick = { filterType = null; showFilterMenu = false })
                        StockMovementType.entries.forEach { type ->
                            DropdownMenuItem(text = { Text(type.name) }, onClick = { filterType = type; showFilterMenu = false })
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (movements.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No hay movimientos registrados", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredMovements) { movement ->
                    MovementItem(movement, dateFormat)
                    HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.5f))
                }
            }
        }
    }
}

@Composable
fun MovementItem(movement: com.farfresh.app.data.model.StockMovement, dateFormat: SimpleDateFormat) {
    val color = when(movement.type) {
        StockMovementType.ENTRADA -> Color(0xFF4CAF50)
        StockMovementType.SALIDA -> Color(0xFFF44336)
        StockMovementType.AJUSTE -> Color(0xFF2196F3)
    }
    
    val prefix = when(movement.type) {
        StockMovementType.ENTRADA -> "+"
        StockMovementType.SALIDA -> "-"
        StockMovementType.AJUSTE -> if (movement.quantity >= 0) "+" else ""
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(movement.productName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(dateFormat.format(Date(movement.timestamp)), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            if (!movement.reason.isNullOrBlank()) {
                Text(movement.reason, style = MaterialTheme.typography.bodyMedium, color = Color.DarkGray)
            }
            Text("Stock: ${movement.stockBefore.toInt()} -> ${movement.stockAfter.toInt()}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        }
        
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "$prefix${movement.quantity.toInt()}",
                color = color,
                fontWeight = FontWeight.Black,
                fontSize = 18.sp
            )
            Text(
                text = movement.type.name,
                color = color,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
