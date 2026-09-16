package com.farfresh.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.farfresh.app.ui.viewmodel.CustomerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomersScreen(
    onBack: () -> Unit,
    onCustomerClick: (String) -> Unit,
    viewModel: CustomerViewModel = viewModel()
) {
    var searchId by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel.customerMessage) {
        viewModel.customerMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.customerMessage = null
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Buscar Cliente", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { /* Lógica para agregar nuevo cliente si es necesario */ }) {
                Icon(Icons.Default.Add, contentDescription = "Nuevo cliente")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Card(
                modifier = Modifier.padding(16.dp).fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Búsqueda por código", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = searchId,
                        onValueChange = { searchId = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Escribe el código del cliente...") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        trailingIcon = {
                            if (viewModel.isSearching) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            } else {
                                IconButton(onClick = { viewModel.searchCustomer(searchId) }) {
                                    Icon(Icons.Default.Search, contentDescription = "Buscar")
                                }
                            }
                        }
                    )
                    
                    Button(
                        onClick = { viewModel.searchCustomer(searchId) },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        enabled = !viewModel.isSearching && searchId.isNotBlank()
                    ) {
                        Text("BUSCAR CLIENTE")
                    }
                }
            }

            if (viewModel.foundCustomer != null) {
                val customer = viewModel.foundCustomer!!
                Card(
                    modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth()
                        .clickable { onCustomerClick(customer.id) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Cliente encontrado:", style = MaterialTheme.typography.labelSmall)
                        Text(customer.name, fontWeight = FontWeight.Black, fontSize = 20.sp)
                        Text("DNI: ${customer.dni}", style = MaterialTheme.typography.bodyMedium)
                        Text("Telf: ${customer.phone ?: "No registrado"}", style = MaterialTheme.typography.bodyMedium)
                        
                        TextButton(
                            onClick = { onCustomerClick(customer.id) },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("VER DETALLE / DEUDAS")
                        }
                    }
                }
            }
        }
    }
}
