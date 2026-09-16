package com.farfresh.app.ui.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.SubcomposeAsyncImage
import androidx.compose.material.icons.filled.Error
import com.farfresh.app.data.model.Customer
import com.farfresh.app.data.model.PaymentMethod
import com.farfresh.app.data.model.Product
import com.farfresh.app.ui.viewmodel.NewSaleViewModel

import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewSaleScreen(
    onBack: () -> Unit,
    viewModel: NewSaleViewModel = viewModel()
) {
    val products by viewModel.products.collectAsStateWithLifecycle()
    var showProductDialog by remember { mutableStateOf(false) }
    var showCustomerDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

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
                title = { Text("Nueva Venta", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Sección Cliente
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Identificar Cliente", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = viewModel.customerIdInput,
                            onValueChange = { viewModel.customerIdInput = it },
                            label = { Text("Código de cliente") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            trailingIcon = {
                                IconButton(onClick = { viewModel.searchCustomerById(viewModel.customerIdInput) }) {
                                    Icon(Icons.Default.Search, contentDescription = "Buscar")
                                }
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(onClick = { showCustomerDialog = true }) {
                            Icon(Icons.Default.ChevronRight, contentDescription = "Seleccionar de lista")
                        }
                    }

                    if (viewModel.selectedCustomer != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = viewModel.selectedCustomer?.name ?: "",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        
                        var phoneInput by remember { mutableStateOf(viewModel.selectedCustomer?.phone ?: "") }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = phoneInput,
                                onValueChange = { phoneInput = it },
                                label = { Text("Teléfono (opcional)") },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                trailingIcon = {
                                    if (phoneInput != (viewModel.selectedCustomer?.phone ?: "")) {
                                        IconButton(onClick = { viewModel.updateCustomerPhone(phoneInput) }) {
                                            Icon(Icons.Default.Add, contentDescription = "Guardar teléfono")
                                        }
                                    }
                                }
                            )
                        }
                    } else {
                        Text(
                            text = "Consumidor general",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Lista de productos
            Text("Productos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(viewModel.cart) { item ->
                    CartItemRow(
                        item = item,
                        onIncrease = { viewModel.updateQuantity(item, 1.0) },
                        onDecrease = { viewModel.updateQuantity(item, -1.0) }
                    )
                }
                
                item {
                    Button(
                        onClick = { showProductDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Agregar producto")
                    }
                }
            }

            // Resumen y Pago
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("TOTAL", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                Text("S/ ${String.format("%.2f", viewModel.total)}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("¿Cómo pagó?", style = MaterialTheme.typography.titleSmall)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                com.farfresh.app.ui.components.PaymentChip("Efectivo", viewModel.paymentMethod == PaymentMethod.EFECTIVO, Color(0xFF4CAF50)) {
                    viewModel.paymentMethod = PaymentMethod.EFECTIVO
                }
                com.farfresh.app.ui.components.PaymentChip("Yape", viewModel.paymentMethod == PaymentMethod.YAPE, Color(0xFF2196F3)) {
                    viewModel.paymentMethod = PaymentMethod.YAPE
                }
                com.farfresh.app.ui.components.PaymentChip("Pendiente", viewModel.paymentMethod == PaymentMethod.OTRO, Color(0xFFFF9800)) {
                    viewModel.paymentMethod = PaymentMethod.OTRO
                }
            }

            if (viewModel.paymentMethod != PaymentMethod.OTRO) {
                OutlinedTextField(
                    value = viewModel.receivedAmount,
                    onValueChange = { viewModel.receivedAmount = it },
                    label = { Text(if (viewModel.paymentMethod == PaymentMethod.EFECTIVO) "Monto recibido" else "Monto pagado") },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
                
                if (viewModel.paymentMethod == PaymentMethod.EFECTIVO && viewModel.change > 0) {
                    Text(
                        "Vuelto: S/ ${String.format("%.2f", viewModel.change)}",
                        color = Color(0xFF4CAF50),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                if (viewModel.pendingBalance > 0) {
                    Text(
                        "Saldo pendiente: S/ ${String.format("%.2f", viewModel.pendingBalance)}",
                        color = Color(0xFFF44336),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { viewModel.registerSale(onSuccess = onBack) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Text("REGISTRAR VENTA", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        }
    }

    if (showProductDialog) {
        ProductSelectionDialog(
            products = products,
            onDismiss = { showProductDialog = false },
            onProductSelected = { prod, qty ->
                viewModel.addProduct(prod, qty)
                showProductDialog = false
            }
        )
    }

    if (showCustomerDialog) {
        CustomerSelectionDialog(
            onDismiss = { showCustomerDialog = false },
            onCustomerSelected = { 
                viewModel.selectedCustomer = it
                showCustomerDialog = false
            }
        )
    }
}

@Composable
fun CartItemRow(item: com.farfresh.app.ui.viewmodel.CartItem, onIncrease: () -> Unit, onDecrease: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(item.product.name, fontWeight = FontWeight.Bold)
                Text("S/ ${String.format("%.2f", item.product.sellingPrice)} x ${item.quantity.toInt()}", style = MaterialTheme.typography.bodySmall)
            }
            
            Text(
                "S/ ${String.format("%.2f", item.product.sellingPrice * item.quantity)}",
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            IconButton(onClick = onDecrease) {
                Icon(if (item.quantity > 1) Icons.Default.Remove else Icons.Default.Delete, contentDescription = null)
            }
            Text("${item.quantity.toInt()}")
            IconButton(onClick = onIncrease) {
                Icon(Icons.Default.Add, contentDescription = null)
            }
        }
    }
}

@Composable
fun ProductSelectionDialog(products: List<Product>, onDismiss: () -> Unit, onProductSelected: (Product, Double) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Seleccionar Producto") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(products.filter { it.isActive }) { product ->
                    ListItem(
                        headlineContent = { 
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(product.name)
                                if (product.volumeMl != null) {
                                    Text(" (${product.volumeMl}ml)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        },
                        supportingContent = { Text("Precio: S/ ${product.sellingPrice} | Stock: ${product.stock}") },
                        leadingContent = {
                            SubcomposeAsyncImage(
                                model = product.imageUrl,
                                contentDescription = null,
                                modifier = Modifier
                                    .width(50.dp)
                                    .height(70.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentScale = ContentScale.Crop,
                                loading = {
                                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 1.dp)
                                    }
                                },
                                error = {
                                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Error, contentDescription = null, tint = Color.LightGray)
                                    }
                                }
                            )
                        },
                        modifier = Modifier.clickable { onProductSelected(product, 1.0) }
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } }
    )
}

@Composable
fun CustomerSelectionDialog(onDismiss: () -> Unit, onCustomerSelected: (Customer?) -> Unit) {
    var searchId by remember { mutableStateOf("") }
    val customerRepository = com.farfresh.app.data.repository.CustomerRepository()
    val scope = rememberCoroutineScope()
    var foundCustomer by remember { mutableStateOf<Customer?>(null) }
    var isSearching by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Buscar Cliente") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = searchId,
                    onValueChange = { searchId = it },
                    label = { Text("Código de cliente") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                
                Button(
                    onClick = {
                        scope.launch {
                            isSearching = true
                            try {
                                foundCustomer = customerRepository.getCustomerById(searchId)
                            } catch (e: Exception) {
                                Log.e("NewSaleScreen", "Error searching", e)
                            } finally {
                                isSearching = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSearching && searchId.isNotBlank()
                ) {
                    if (isSearching) CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    else Text("BUSCAR")
                }

                if (foundCustomer != null) {
                    ListItem(
                        headlineContent = { Text(foundCustomer!!.name) },
                        supportingContent = { Text("DNI: ${foundCustomer!!.dni}") },
                        modifier = Modifier.clickable { onCustomerSelected(foundCustomer) }
                    )
                }
                
                ListItem(
                    headlineContent = { Text("Consumidor general") },
                    modifier = Modifier.clickable { onCustomerSelected(null) }
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } }
    )
}
