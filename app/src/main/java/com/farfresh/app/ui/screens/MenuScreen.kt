package com.farfresh.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.farfresh.app.data.repository.CustomerRepository
import com.farfresh.app.data.repository.ProductRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Repositorios para las acciones de sincronización
    val productRepo = remember { ProductRepository() }
    val customerRepo = remember { CustomerRepository() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Menú Principal", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            item { 
                Text(
                    "General", 
                    style = MaterialTheme.typography.labelLarge, 
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(16.dp)
                )
            }
            item { MenuItem("Perfil de Tienda", Icons.Default.Store) { } }
            item { MenuItem("Ayuda y Soporte", Icons.AutoMirrored.Filled.Help) { } }
            
            item { 
                Text(
                    "Mantenimiento y Datos", 
                    style = MaterialTheme.typography.labelLarge, 
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(16.dp)
                )
            }
            item { 
                MenuItem("Sincronizar Productos", Icons.Default.Sync) { 
                    scope.launch {
                        productRepo.syncProducts()
                        snackbarHostState.showSnackbar("Productos sincronizados con éxito")
                    }
                } 
            }
            item { 
                MenuItem("Sincronizar Clientes", Icons.Default.CloudDownload) { 
                    scope.launch {
                        customerRepo.syncCustomers()
                        snackbarHostState.showSnackbar("Lista de clientes actualizada")
                    }
                } 
            }
            
            item { Spacer(modifier = Modifier.height(24.dp)) }
            item { MenuItem("Cerrar Sesión", Icons.AutoMirrored.Filled.ExitToApp) { } }
        }
    }
}

@Composable
fun MenuItem(title: String, icon: ImageVector, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        leadingContent = { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        modifier = Modifier.clickable(onClick = onClick)
    )
    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
}
