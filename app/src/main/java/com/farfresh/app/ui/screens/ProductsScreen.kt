package com.farfresh.app.ui.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.History
import coil.compose.SubcomposeAsyncImage
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.farfresh.app.data.model.Product
import com.farfresh.app.data.model.StockMovementType
import com.farfresh.app.ui.viewmodel.InventoryViewModel
import com.farfresh.app.ui.viewmodel.ProductsViewModel
import kotlinx.coroutines.launch

import androidx.compose.material.icons.filled.Share
import com.farfresh.app.data.util.CatalogUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductsScreen(
    onHistoryClick: (String?) -> Unit,
    onAddProductClick: () -> Unit,
    onEditProductClick: (String) -> Unit,
    productsViewModel: ProductsViewModel = viewModel(),
    inventoryViewModel: InventoryViewModel = viewModel()
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val products by productsViewModel.products.collectAsStateWithLifecycle()
    
    var selectedProductForAction by remember { mutableStateOf<Pair<Product, StockMovementType>?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    
    LaunchedEffect(inventoryViewModel.inventoryMessage) {
        inventoryViewModel.inventoryMessage?.let {
            snackbarHostState.showSnackbar(it)
            inventoryViewModel.inventoryMessage = null
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Productos", fontWeight = FontWeight.Black) },
                actions = {
                    IconButton(onClick = { 
                        scope.launch {
                            CatalogUtils.generateAndShareCatalog(context, products)
                        }
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Compartir catálogo")
                    }
                    IconButton(onClick = { onHistoryClick(null) }) {
                        Icon(Icons.Default.History, contentDescription = "Historial completo")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddProductClick, 
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Agregar producto")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Buscador
            OutlinedTextField(
                value = productsViewModel.searchQuery,
                onValueChange = { productsViewModel.searchQuery = it },
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                placeholder = { Text("Buscar producto...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = MaterialTheme.shapes.medium
            )

            val filteredProducts = products.filter {
                it.name.contains(productsViewModel.searchQuery, ignoreCase = true)
            }

            if (productsViewModel.isSyncing && products.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Sincronizando productos...", color = Color.Gray)
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredProducts) { product ->
                    ProductCard(
                        product = product,
                        onAction = { type -> selectedProductForAction = Pair(product, type) },
                        onHistory = { onHistoryClick(product.id) },
                        onEdit = { onEditProductClick(product.id) }
                    )
                }
                
                if (filteredProducts.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("No se encontraron productos", color = Color.Gray)
                        }
                    }
                }
            }
        }
    }

    selectedProductForAction?.let { (product, type) ->
        StockActionDialog(
            product = product,
            type = type,
            onDismiss = { selectedProductForAction = null },
            onConfirm = { amount, reason ->
                inventoryViewModel.registerMovement(product.id, product.name, amount, type, reason)
                selectedProductForAction = null
            }
        )
    }
}

@Composable
fun ProductCard(
    product: Product,
    onAction: (StockMovementType) -> Unit,
    onHistory: () -> Unit,
    onEdit: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val stockColor = when {
        product.stock <= 0 -> Color.Red
        product.stock <= 10 -> Color(0xFFFF9800) // Naranja (Stock Bajo)
        else -> Color(0xFF4CAF50) // Verde
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Imagen del producto
                SubcomposeAsyncImage(
                    model = product.imageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .width(50.dp)
                        .height(70.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentScale = ContentScale.Crop,
                    error = {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Image, contentDescription = null, tint = Color.LightGray)
                        }
                    }
                )
                
                if (product.imageUrl.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .width(50.dp)
                            .height(70.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null, tint = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(product.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(product.category, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        if (product.volumeMl != null) {
                            Text(" • ", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            Text("${product.volumeMl}ml", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Opciones")
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Entrada de stock") },
                            onClick = { onAction(StockMovementType.ENTRADA); showMenu = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Salida manual") },
                            onClick = { onAction(StockMovementType.SALIDA); showMenu = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Ajustar stock") },
                            onClick = { onAction(StockMovementType.AJUSTE); showMenu = false }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Ver movimientos") },
                            onClick = { onHistory(); showMenu = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Editar producto") },
                            onClick = { onEdit(); showMenu = false }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Precio Venta", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Text("S/ ${String.format("%.2f", product.sellingPrice)}", fontWeight = FontWeight.Bold)
                }
                Column {
                    Text("Costo", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Text("S/ ${String.format("%.2f", product.cost)}", fontWeight = FontWeight.Bold)
                }
                Column {
                    Text("Ganancia", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Text("S/ ${String.format("%.2f", product.sellingPrice - product.cost)}", color = Color(0xFFFF9800), fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Stock Actual", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Text(
                        text = "${product.stock.toInt()} ${product.unit}",
                        color = stockColor,
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp
                    )
                }
            }
            
            if (product.stock <= 10) {
                Text(
                    text = if (product.stock <= 0) "SIN STOCK" else "STOCK BAJO",
                    color = stockColor,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
fun StockActionDialog(
    product: Product,
    type: StockMovementType,
    onDismiss: () -> Unit,
    onConfirm: (Double, String) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }
    
    val title = when(type) {
        StockMovementType.ENTRADA -> "Entrada de Stock"
        StockMovementType.SALIDA -> "Salida de Stock"
        StockMovementType.AJUSTE -> "Ajuste de Stock"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Producto: ${product.name}", fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Cantidad (${product.unit})") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                )
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Motivo / Nota") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { 
                    val qty = amount.toDoubleOrNull() ?: 0.0
                    if (qty > 0) onConfirm(qty, reason)
                },
                enabled = amount.isNotBlank()
            ) {
                Text("CONFIRMAR")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCELAR") }
        }
    )
}
