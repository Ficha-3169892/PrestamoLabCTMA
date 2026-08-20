package com.example.prestamolabctma.ui.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.prestamolabctma.ui.screens.CatalogoScreen
import com.example.prestamolabctma.ui.screens.EquipoDetalleScreen
import com.example.prestamolabctma.ui.screens.MisPrestamosScreen
import com.example.prestamolabctma.ui.screens.SolicitudFormScreen
import com.example.prestamolabctma.ui.viewmodel.PrestamoViewModel

sealed class Screen(val route: String, val label: String = "") {
    data object Catalogo : Screen("catalogo", "Catálogo")
    data object EquipoDetalle : Screen("detalle/{equipoId}") {
        fun createRoute(equipoId: Int) = "detalle/$equipoId"
    }
    data object Solicitar : Screen("solicitar/{equipoId}") {
        fun createRoute(equipoId: Int) = "solicitar/$equipoId"
    }
    data object MisSolicitudes : Screen("mis_solicitudes", "Mis Préstamos")
}

@Composable
fun PrestamoApp(viewModel: PrestamoViewModel) {
    val navController = rememberNavController()
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.mensaje) {
        uiState.mensaje?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.limpiarMensaje()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination
            val mainScreens = listOf(Screen.Catalogo, Screen.MisSolicitudes)

            if (currentDestination?.route in listOf(Screen.Catalogo.route, Screen.MisSolicitudes.route)) {
                NavigationBar {
                    mainScreens.forEach { screen ->
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    imageVector = if (screen == Screen.Catalogo) Icons.Default.Home else Icons.AutoMirrored.Filled.List,
                                    contentDescription = null
                                )
                            },
                            label = { Text(screen.label) },
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
    ) { innerPadding: PaddingValues ->
        PrestamoNavHost(
            viewModel = viewModel,
            navController = navController,
            modifier = Modifier.padding(innerPadding)
        )
    }
}

@Composable
fun PrestamoNavHost(
    viewModel: PrestamoViewModel,
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    NavHost(
        navController = navController,
        startDestination = Screen.Catalogo.route,
        modifier = modifier
    ) {
        composable(Screen.Catalogo.route) { _: NavBackStackEntry ->
            CatalogoScreen(
                equipos = uiState.equipos,
                onEquipoClick = { id ->
                    navController.navigate(Screen.EquipoDetalle.createRoute(id))
                },
                onVerMisPrestamos = {
                    navController.navigate(Screen.MisSolicitudes.route)
                }
            )
        }

        composable(
            route = Screen.EquipoDetalle.route,
            arguments = listOf(navArgument("equipoId") { type = NavType.IntType })
        ) { backStackEntry: NavBackStackEntry ->
            val equipoId = backStackEntry.arguments?.getInt("equipoId") ?: -1
            val equipo = uiState.equipos.find { it.id == equipoId }

            if (equipo == null) {
                LaunchedEffect(Unit) {
                    navController.popBackStack()
                }
            } else {
                EquipoDetalleScreen(
                    equipo = equipo,
                    onSolicitarClick = { id ->
                        navController.navigate(Screen.Solicitar.createRoute(id))
                    },
                    onBackClick = { navController.popBackStack() }
                )
            }
        }

        composable(
            route = Screen.Solicitar.route,
            arguments = listOf(navArgument("equipoId") { type = NavType.IntType })
        ) { backStackEntry: NavBackStackEntry ->
            val equipoId = backStackEntry.arguments?.getInt("equipoId") ?: -1
            val equipo = uiState.equipos.find { it.id == equipoId }

            if (equipo == null) {
                LaunchedEffect(Unit) {
                    navController.popBackStack()
                }
            } else {
                SolicitudFormScreen(
                    equipo = equipo,
                    guardando = uiState.guardando,
                    onGuardar = { id, amb, prop, hor ->
                        viewModel.registrarSolicitud(id, amb, prop, hor)
                        navController.navigate(Screen.MisSolicitudes.route) {
                            popUpTo(Screen.Catalogo.route)
                        }
                    },
                    onCancelar = { navController.popBackStack() }
                )
            }
        }

        composable(Screen.MisSolicitudes.route) { _: NavBackStackEntry ->
            MisPrestamosScreen(
                solicitudes = uiState.solicitudes,
                onCancelarClick = { id -> viewModel.cancelarSolicitud(id) },
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
