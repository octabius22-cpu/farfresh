package com.farfresh.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.farfresh.app.data.model.Product
import com.farfresh.app.data.model.StockMovementType

@Composable
fun StockActionDialog(
    product: Product,
    type: StockMovementType,
    onDismiss: () -> Unit,
    onConfirm: (Double, String?) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    val title = when(type) {
        StockMovementType.ENTRADA -> "Entrada de Mercadería"
        StockMovementType.SALIDA -> "Salida de Mercadería"
        StockMovementType.AJUSTE -> "Ajuste de Inventario"
    }

    val label = when(type) {
        StockMovementType.AJUSTE -> "Nuevo Stock Físico"
        else -> "Cantidad"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(product.name, style = MaterialTheme.typography.titleMedium)
                Text("Stock actual: ${product.stock.toInt()}", style = MaterialTheme.typography.bodyMedium)
                
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it; error = null },
                    label = { Text(label) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    isError = error != null,
                    supportingText = { error?.let { Text(it) } }
                )

                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Motivo (opcional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val amount = amountText.toDoubleOrNull()
                if (amount == null) {
                    error = "Ingresa una cantidad válida"
                } else if (amount <= 0 && type != StockMovementType.AJUSTE) {
                    error = "La cantidad debe ser mayor a cero"
                } else if (type == StockMovementType.SALIDA && amount > product.stock) {
                    error = "No puedes sacar más de lo disponible (${product.stock.toInt()})"
                } else {
                    onConfirm(amount, if (reason.isBlank()) null else reason)
                }
            }) {
                Text("CONFIRMAR")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
