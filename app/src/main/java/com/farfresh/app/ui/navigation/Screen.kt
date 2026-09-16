package com.farfresh.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Home : Screen("home", "Inicio", Icons.Default.Home)
    object Sales : Screen("sales", "Ventas", Icons.AutoMirrored.Filled.ReceiptLong)
    object Pending : Screen("pending", "Pendientes", Icons.Default.PendingActions)
    object Products : Screen("products", "Productos", Icons.Default.Inventory)
    object Reports : Screen("reports", "Reportes", Icons.Default.Assessment)
    object NewSale : Screen("new_sale", "Nueva Venta", Icons.AutoMirrored.Filled.ReceiptLong)
    object Customers : Screen("customers", "Clientes", Icons.Default.PendingActions)
    object CustomerDetail : Screen("customer_detail/{customerId}", "Detalle Cliente", Icons.Default.PendingActions)
    object SaleDetail : Screen("sale_detail/{saleId}", "Detalle Venta", Icons.AutoMirrored.Filled.ReceiptLong)
    object StockHistory : Screen("stock_history?productId={productId}", "Movimientos", Icons.Default.Assessment)
    object Menu : Screen("menu", "Menú", Icons.Default.Menu)
    object AddProduct : Screen("add_product?productId={productId}", "Producto", Icons.Default.Add)
}
