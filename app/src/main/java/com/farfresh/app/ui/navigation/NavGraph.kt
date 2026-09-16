package com.farfresh.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.farfresh.app.ui.screens.*

import androidx.navigation.navArgument
import androidx.navigation.NavType

@Composable
fun FarFreshNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onNewSaleClick = { navController.navigate(Screen.NewSale.route) },
                onSeeAllPendingClick = { navController.navigate(Screen.Pending.route) },
                onSaleClick = { saleId -> navController.navigate("sale_detail/$saleId") },
                onMenuClick = { navController.navigate(Screen.Menu.route) }
            )
        }
        composable(Screen.Menu.route) {
            MenuScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.Sales.route) {
            SalesScreen(onSaleClick = { saleId -> navController.navigate("sale_detail/$saleId") })
        }
        composable(Screen.Pending.route) {
            PendingScreen(
                onSaleDetailClick = { saleId -> navController.navigate("sale_detail/$saleId") },
                onCustomersClick = { navController.navigate(Screen.Customers.route) }
            )
        }
        composable(Screen.Products.route) {
            ProductsScreen(
                onHistoryClick = { productId -> 
                    val route = if (productId != null) "stock_history?productId=$productId" else "stock_history"
                    navController.navigate(route)
                },
                onAddProductClick = { navController.navigate("add_product") },
                onEditProductClick = { productId ->
                    navController.navigate("add_product?productId=$productId")
                }
            )
        }
        composable(
            route = Screen.AddProduct.route,
            arguments = listOf(navArgument("productId") { 
                type = NavType.StringType
                nullable = true
                defaultValue = null
            })
        ) { backStackEntry ->
            val productId = backStackEntry.arguments?.getString("productId")
            AddProductScreen(
                productId = productId,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.StockHistory.route) { backStackEntry ->
            val productId = backStackEntry.arguments?.getString("productId")
            StockHistoryScreen(
                productId = productId,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Reports.route) {
            ReportsScreen()
        }
        composable(Screen.NewSale.route) {
            NewSaleScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.Customers.route) {
            CustomersScreen(
                onBack = { navController.popBackStack() },
                onCustomerClick = { customerId -> navController.navigate("customer_detail/$customerId") }
            )
        }
        composable(Screen.CustomerDetail.route) { backStackEntry ->
            val customerId = backStackEntry.arguments?.getString("customerId") ?: ""
            CustomerDetailScreen(
                customerId = customerId,
                onBack = { navController.popBackStack() },
                onSaleClick = { saleId -> navController.navigate("sale_detail/$saleId") }
            )
        }
        composable(Screen.SaleDetail.route) { backStackEntry ->
            val saleId = backStackEntry.arguments?.getString("saleId") ?: ""
            SaleDetailScreen(
                saleId = saleId,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
