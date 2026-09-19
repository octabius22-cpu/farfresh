package com.farfresh.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.farfresh.app.data.model.PaymentMethod
import com.farfresh.app.data.model.SaleStatus
import com.farfresh.app.data.repository.SaleRepository
import com.farfresh.app.data.repository.CustomerRepository
import com.farfresh.app.ui.viewmodel.PendingViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import androidx.compose.material.icons.filled.Share
import com.farfresh.app.data.util.ReceiptUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaleDetailScreen(
    saleId: String,
    onBack: () -> Unit,
    viewModel: PendingViewModel = viewModel()
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val customerRepository = CustomerRepository()
    val allSales by SaleRepository.getSales().collectAsStateWithLifecycle(emptyList())
    val allCustomers by customerRepository.getCustomers().collectAsStateWithLifecycle(emptyList())
    
    val sale = allSales.find { it.id == saleId }
    val customer = allCustomers.find { it.id == sale?.customerId }
    val items by viewModel.getItemsForSale(saleId).collectAsStateWithLifecycle(emptyList())
    val payments by viewModel.getPaymentsForSale(saleId).collectAsStateWithLifecycle(emptyList())
    
    var showPaymentDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val dateFormat = SimpleDateFormat("dd/MM/yyyy h:mm a", Locale.getDefault())

    LaunchedEffect(viewModel.saleMessage) {
        viewModel.saleMessage?.let {
            // Notificar éxito/error
        }
    }

    if (sale == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Venta no encontrada")
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalle de Venta", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        scope.launch {
                            ReceiptUtils.generateAndShareReceipt(
                                context,
                                sale,
                                items,
                                customer?.name ?: "Consumidor general"
                            )
                        }
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Compartir recibo")
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Eliminar venta", tint = MaterialTheme.colorScheme.error)
                    }
                }
            )
        },
        floatingActionButton = {
            if (sale.pendingBalance > 0) {
                ExtendedFloatingActionButton(
                    onClick = { showPaymentDialog = true },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Registrar pago") }
                )
            }
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
                Text("Cliente", style = MaterialTheme.typography.labelMedium)
                Text(customer?.name ?: "Consumidor general", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                if (customer?.phone != null) {
                    Text(customer.phone, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Venta", fontWeight = FontWeight.Bold)
                            val statusColor = when(sale.status) {
                                SaleStatus.PAGADA -> Color(0xFF4CAF50)
                                SaleStatus.PENDIENTE -> Color(0xFFFF9800)
                                SaleStatus.PARCIAL -> Color(0xFF2196F3)
                            }
                            Text(sale.status.name, color = statusColor, fontWeight = FontWeight.Bold)
                        }
                        Text(dateFormat.format(Date(sale.timestamp)), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            item {
                Text("Productos", fontWeight = FontWeight.Bold)
            }

            items(items) { item ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(item.productName, fontWeight = FontWeight.Bold)
                        Text("S/ ${String.format("%.2f", item.sellingPriceAtSale)} x ${item.quantity.toInt()}", style = MaterialTheme.typography.bodySmall)
                    }
                    Text("S/ ${String.format("%.2f", item.subtotal)}", fontWeight = FontWeight.Bold)
                }
            }

            item {
                HorizontalDivider()
                SummaryRow("Total", "S/ ${String.format("%.2f", sale.totalAmount)}", isBold = true)
                SummaryRow("Costo", "S/ ${String.format("%.2f", sale.totalAmount - sale.totalProfit)}")
                SummaryRow("Ganancia", "S/ ${String.format("%.2f", sale.totalProfit)}", color = Color(0xFFFF9800), isBold = true)
                Spacer(modifier = Modifier.height(8.dp))
                SummaryRow("Pagado", "S/ ${String.format("%.2f", sale.paidAmount)}")
                SummaryRow("Pendiente", "S/ ${String.format("%.2f", sale.pendingBalance)}", color = MaterialTheme.colorScheme.error, isBold = true)
            }

            if (payments.isNotEmpty()) {
                item {
                    Text("Historial de pagos", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                }
                items(payments) { payment ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(payment.paymentMethod.name, fontWeight = FontWeight.Bold)
                                Text("S/ ${String.format("%.2f", payment.amount)}", fontWeight = FontWeight.Black)
                            }
                            Text(dateFormat.format(Date(payment.timestamp)), style = MaterialTheme.typography.bodySmall)
                            if (payment.note != null) {
                                Text(payment.note, style = MaterialTheme.typography.bodyMedium, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("¿Eliminar venta?") },
            text = { Text("Esta acción es permanente. El stock de los productos vendidos se restaurará automáticamente.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteSale(saleId, onSuccess = onBack)
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("ELIMINAR")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (showPaymentDialog) {
        RegisterPaymentDialog(
            maxAmount = sale.pendingBalance,
            onDismiss = { showPaymentDialog = false },
            onConfirm = { amount, method, note ->
                viewModel.registerPayment(saleId, amount, method, note)
                showPaymentDialog = false
            }
        )
    }
}

@Composable
fun SummaryRow(label: String, value: String, isBold: Boolean = false, color: Color = Color.Unspecified) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal)
        Text(value, fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal, color = color)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterPaymentDialog(
    maxAmount: Double,
    onDismiss: () -> Unit,
    onConfirm: (Double, PaymentMethod, String?) -> Unit
) {
    var amountText by remember { mutableStateOf(String.format("%.2f", maxAmount).replace(",", ".")) }
    var selectedMethod by remember { mutableStateOf(PaymentMethod.EFECTIVO) }
    var note by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Registrar pago") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Monto") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    isError = error != null,
                    supportingText = { error?.let { Text(it) } }
                )
                
                Text("Método de pago", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    com.farfresh.app.ui.components.PaymentChip("Efectivo", selectedMethod == PaymentMethod.EFECTIVO, Color(0xFF4CAF50)) {
                        selectedMethod = PaymentMethod.EFECTIVO
                    }
                    com.farfresh.app.ui.components.PaymentChip("Yape", selectedMethod == PaymentMethod.YAPE, Color(0xFF2196F3)) {
                        selectedMethod = PaymentMethod.YAPE
                    }
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Nota (opcional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val amount = amountText.toDoubleOrNull()
                if (amount == null || amount <= 0) {
                    error = "Monto inválido"
                } else if (amount > maxAmount + 0.001) {
                    error = "El pago no puede ser mayor que el saldo pendiente"
                } else {
                    onConfirm(amount, selectedMethod, if (note.isBlank()) null else note)
                }
            }) {
                Text("REGISTRAR PAGO")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
