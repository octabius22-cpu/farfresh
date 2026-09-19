package com.farfresh.app.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.SubcomposeAsyncImage
import com.farfresh.app.data.model.Product
import com.farfresh.app.ui.viewmodel.ProductsViewModel
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProductScreen(
    productId: String? = null,
    onBack: () -> Unit,
    viewModel: ProductsViewModel = viewModel()
) {
    val productsState by viewModel.products.collectAsStateWithLifecycle()
    
    var name by remember { mutableStateOf("") }
    var categoryText by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("unidad") }
    var cost by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var stock by remember { mutableStateOf("") }
    var volumeMl by remember { mutableStateOf("") }
    var imageUrl by remember { mutableStateOf("") }
    var controlStock by remember { mutableStateOf(true) }
    
    var isDataLoaded by remember { mutableStateOf(false) }

    // Cargar datos si estamos editando
    LaunchedEffect(productId, productsState) {
        if (productId != null && !isDataLoaded) {
            val existingProduct = productsState.find { it.id == productId }
            existingProduct?.let {
                Log.d("AddProductScreen", "Cargando producto para editar: ${it.name}")
                name = it.name
                categoryText = it.category
                unit = it.unit
                cost = it.cost.toString()
                price = it.sellingPrice.toString()
                stock = it.stock.toString()
                volumeMl = it.volumeMl?.toString() ?: ""
                imageUrl = it.imageUrl ?: ""
                isDataLoaded = true // Evita que se vuelva a cargar y sobrescriba cambios del usuario
            }
        }
    }

    val profit = (price.toDoubleOrNull() ?: 0.0) - (cost.toDoubleOrNull() ?: 0.0)
    val isDrink = categoryText.trim().equals("Bebidas", ignoreCase = true)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (productId == null) "Nuevo Producto" else "Editar Producto", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Vista previa de imagen si hay URL
            if (imageUrl.isNotBlank()) {
                Card(
                    modifier = Modifier.size(100.dp).align(Alignment.CenterHorizontally),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    SubcomposeAsyncImage(
                        model = imageUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        loading = {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            }
                        },
                        error = {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Error, contentDescription = null, tint = Color.LightGray)
                            }
                        }
                    )
                }
            }

            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nombre del Producto *") }, modifier = Modifier.fillMaxWidth())
            
            OutlinedTextField(value = categoryText, onValueChange = { categoryText = it }, label = { Text("Categoría (Bebidas, Snacks...)") }, modifier = Modifier.fillMaxWidth())

            if (isDrink) {
                OutlinedTextField(
                    value = volumeMl,
                    onValueChange = { volumeMl = it },
                    label = { Text("Contenido (ml) - Ej: 500") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }

            OutlinedTextField(
                value = imageUrl, 
                onValueChange = { imageUrl = it }, 
                label = { Text("URL de la imagen (Ej: https://...)") }, 
                modifier = Modifier.fillMaxWidth(),
                supportingText = { Text("Pega un enlace directo a la imagen") }
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = cost, onValueChange = { cost = it }, label = { Text("Costo (S/)") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                OutlinedTextField(value = price, onValueChange = { price = it }, label = { Text("Venta (S/)") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
            }

            if (profit != 0.0) {
                val locale = Locale.getDefault()
                Text("Ganancia por unidad: S/ ${String.format(locale, "%.2f", profit)}", color = if (profit > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
            }

            OutlinedTextField(value = stock, onValueChange = { stock = it }, label = { Text("Stock Inicial") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = controlStock, onCheckedChange = { controlStock = it })
                Text("Controlar inventario automático")
            }

            Button(
                onClick = {
                    if (name.isNotBlank() && price.isNotBlank()) {
                        val product = Product(
                            id = productId ?: "", 
                            name = name,
                            category = categoryText,
                            unit = unit,
                            volumeMl = if (isDrink) volumeMl.toIntOrNull() else null,
                            cost = cost.toDoubleOrNull() ?: 0.0,
                            sellingPrice = price.toDoubleOrNull() ?: 0.0,
                            stock = stock.toDoubleOrNull() ?: 0.0,
                            imageUrl = if (imageUrl.isBlank()) null else imageUrl,
                            isActive = true
                        )
                        
                        if (productId == null) {
                            viewModel.addProduct(product)
                        } else {
                            viewModel.updateProduct(product)
                        }
                        onBack()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = name.isNotBlank() && price.isNotBlank()
            ) {
                Text(if (productId == null) "GUARDAR PRODUCTO" else "ACTUALIZAR PRODUCTO")
            }
        }
    }
}
