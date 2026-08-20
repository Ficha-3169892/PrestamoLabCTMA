package com.example.prestamolabctma.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.prestamolabctma.ui.catalogo.CatalogoScreen
import com.example.prestamolabctma.ui.equipo.EquipoDetalleScreen
import com.example.prestamolabctma.ui.misprestamos.MisPrestamosScreen
import com.example.prestamolabctma.ui.solicitud.FormularioSolicitudScreen
import com.example.prestamolabctma.viewmodel.PrestamoViewModel

sealed class Screen(val route: String, val title: String) {
    object Catalogo : Screen("catalogo", "Catálogo")
    object EquipoDetalle : Screen("detalle/{equipoId}", "Detalle") {
        fun createRoute(equipoId: Int) = "detalle/$equipoId"
    }
    object Solicitar : Screen("solicitar/{equipoId}", "Solicitar") {
        fun createRoute(equipoId: Int) = "solicitar/$equipoId"
    }
    object MisSolicitudes : Screen("mis_solicitudes", "Mis Préstamos")
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val viewModel: PrestamoViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                    label = { Text("Catálogo") },
                    selected = currentRoute == Screen.Catalogo.route,
                    onClick = {
                        navController.navigate(Screen.Catalogo.route) {
                            popUpTo(Screen.Catalogo.route) { inclusive = true }
                        }
                    }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.List, contentDescription = null) },
                    label = { Text("Mis Préstamos") },
                    selected = currentRoute == Screen.MisSolicitudes.route,
                    onClick = {
                        navController.navigate(Screen.MisSolicitudes.route) {
                            launchSingleTop = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Catalogo.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Catalogo.route) {
                CatalogoScreen(
                    equipos = uiState.equipos,
                    onEquipoClick = { id ->
                        navController.navigate(Screen.EquipoDetalle.createRoute(id))
                    }
                )
            }

            composable(
                route = Screen.EquipoDetalle.route,
                arguments = listOf(navArgument("equipoId") { type = NavType.IntType })
            ) { backStackEntry ->
                val equipoId = backStackEntry.arguments?.getInt("equipoId")
                val equipo = uiState.equipos.find { it.id == equipoId }
                
                EquipoDetalleScreen(
                    equipo = equipo,
                    onSolicitarClick = { id ->
                        navController.navigate(Screen.Solicitar.createRoute(id))
                    }
                )
            }

            composable(
                route = Screen.Solicitar.route,
                arguments = listOf(navArgument("equipoId") { type = NavType.IntType })
            ) { backStackEntry ->
                val equipoId = backStackEntry.arguments?.getInt("equipoId")
                val equipo = uiState.equipos.find { it.id == equipoId }

                if (equipo != null) {
                    FormularioSolicitudScreen(
                        equipo = equipo,
                        guardando = uiState.guardando,
                        onGuardar = { id, ambiente, proposito, duracion ->
                            viewModel.registrarSolicitud(id, ambiente, proposito, duracion)
                            navController.popBackStack(Screen.Catalogo.route, false)
                        },
                        onCancelar = { navController.popBackStack() }
                    )
                } else {
                    navController.popBackStack()
                }
            }

            composable(Screen.MisSolicitudes.route) {
                MisPrestamosScreen(
                    solicitudes = uiState.solicitudes,
                    equipos = uiState.equipos,
                    onCancelarSolicitud = { id -> viewModel.cancelarSolicitud(id) }
                )
            }
        }
    }
}
