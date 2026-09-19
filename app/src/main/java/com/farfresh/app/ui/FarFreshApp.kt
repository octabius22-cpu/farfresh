package com.farfresh.app.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.farfresh.app.ui.navigation.FarFreshNavGraph
import com.farfresh.app.ui.navigation.Screen

/** Punto de entrada de la interfaz. Implementación de navegación base. */
@Composable
fun FarFreshApp() {
    val navController = rememberNavController()
    val screens = listOf(
        Screen.Home,
        Screen.Sales,
        Screen.Pending,
        Screen.Products,
        Screen.Reports
    )

    Surface(modifier = Modifier.fillMaxSize()) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination
        
        // Ocultar barra de navegación en la pantalla de Nueva Venta
        val showBottomBar = screens.any { it.route == currentDestination?.route }

        Scaffold(
            bottomBar = {
                if (showBottomBar) {
                    NavigationBar {
                        screens.forEach { screen ->
                            NavigationBarItem(
                                icon = { Icon(screen.icon, contentDescription = null) },
                                label = { Text(screen.title) },
                                selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                                onClick = {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            FarFreshNavGraph(
                navController = navController,
                modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())
            )
        }
    }
}
