package com.example.prestamolabctma.ui.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.example.prestamolabctma.model.Role
import com.example.prestamolabctma.ui.screens.*
import com.example.prestamolabctma.ui.viewmodel.PrestamoViewModel

sealed class Screen(val route: String, val label: String = "") {
    data object Login : Screen("login")
    data object Catalogo : Screen("catalogo", "Catálogo")
    data object EquipoDetalle : Screen("detalle/{equipoId}") {
        fun createRoute(equipoId: Int) = "detalle/$equipoId"
    }
    data object Solicitar : Screen("solicitar/{equipoId}") {
        fun createRoute(equipoId: Int) = "solicitar/$equipoId"
    }
    data object MisSolicitudes : Screen("mis_solicitudes", "Mis Préstamos")
    data object Admin : Screen("admin", "Gestión")
    data object Historial : Screen("historial", "Historial")
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
            val usuario = uiState.usuarioLogueado

            if (usuario != null && currentDestination?.route != Screen.Login.route) {
                NavigationBar {
                    // Item Catálogo
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Home, null) },
                        label = { Text(Screen.Catalogo.label) },
                        selected = currentDestination?.hierarchy?.any { it.route == Screen.Catalogo.route } == true,
                        onClick = { navigateBottom(navController, Screen.Catalogo.route) }
                    )
                    
                    // Item Mis Préstamos (Solo Aprendiz/Instructor)
                    if (usuario.rol == Role.APRENDIZ || usuario.rol == Role.INSTRUCTOR) {
                        NavigationBarItem(
                            icon = { Icon(Icons.AutoMirrored.Filled.List, null) },
                            label = { Text(Screen.MisSolicitudes.label) },
                            selected = currentDestination?.hierarchy?.any { it.route == Screen.MisSolicitudes.route } == true,
                            onClick = { navigateBottom(navController, Screen.MisSolicitudes.route) }
                        )
                    }

                    // Item Gestión (Solo Admin/Cuentadante) - HU 06
                    if (usuario.rol == Role.ADMIN || usuario.rol == Role.CUENTADANTE) {
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.Settings, null) },
                            label = { Text(Screen.Admin.label) },
                            selected = currentDestination?.hierarchy?.any { it.route == Screen.Admin.route } == true,
                            onClick = { navigateBottom(navController, Screen.Admin.route) }
                        )
                    }

                    // Item Historial (Solo Admin/Instructor) - HU 10
                    if (usuario.rol == Role.ADMIN || usuario.rol == Role.INSTRUCTOR) {
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.Person, null) },
                            label = { Text(Screen.Historial.label) },
                            selected = currentDestination?.hierarchy?.any { it.route == Screen.Historial.route } == true,
                            onClick = { navigateBottom(navController, Screen.Historial.route) }
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

private fun navigateBottom(navController: NavHostController, route: String) {
    navController.navigate(route) {
        popUpTo(navController.graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
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
        startDestination = Screen.Login.route,
        modifier = modifier
    ) {
        composable(Screen.Login.route) {
            LoginScreen(onLoginSuccess = { user, pass ->
                val success = viewModel.login(user, pass)
                if (success) {
                    navController.navigate(Screen.Catalogo.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
                success
            })
        }

        composable(Screen.Catalogo.route) {
            CatalogoScreen(
                equipos = viewModel.obtenerEquiposFiltrados(),
                busqueda = uiState.filtroBusqueda,
                onBusquedaChange = { viewModel.setFiltroBusqueda(it) },
                categoriaSeleccionada = uiState.filtroCategoria,
                onCategoriaChange = { viewModel.setFiltroCategoria(it) },
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
        ) { backStackEntry ->
            val equipoId = backStackEntry.arguments?.getInt("equipoId") ?: -1
            val equipo = uiState.equipos.find { it.id == equipoId }

            EquipoDetalleScreen(
                equipo = equipo,
                onSolicitarClick = { id ->
                    navController.navigate(Screen.Solicitar.createRoute(id))
                },
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.Solicitar.route,
            arguments = listOf(navArgument("equipoId") { type = NavType.IntType })
        ) { backStackEntry ->
            val equipoId = backStackEntry.arguments?.getInt("equipoId") ?: -1
            val equipo = uiState.equipos.find { it.id == equipoId }

            equipo?.let {
                SolicitudFormScreen(
                    equipo = it,
                    guardando = uiState.guardando,
                    onGuardar = { id, amb, prop, hor, fecha ->
                        viewModel.registrarSolicitud(id, amb, prop, hor, fecha)
                        navController.navigate(Screen.MisSolicitudes.route) {
                            popUpTo(Screen.Catalogo.route)
                        }
                    },
                    onCancelar = { navController.popBackStack() }
                )
            }
        }

        composable(Screen.MisSolicitudes.route) {
            MisPrestamosScreen(
                solicitudes = uiState.solicitudes.filter { it.usuarioId == uiState.usuarioLogueado?.id },
                onCancelarClick = { viewModel.procesarSolicitud(it, false, "Cancelada por usuario") },
                onExtenderClick = { viewModel.solicitarExtension(it) },
                onReportarFalla = { /* HU 09 */ viewModel.registrarDevolucion(it, "Falla reportada por usuario", true) },
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Screen.Admin.route) {
            AdminScreen(
                solicitudes = uiState.solicitudes,
                onProcesar = { id, ok, mot -> viewModel.procesarSolicitud(id, ok, mot) },
                onDevolver = { id, nov, grave -> viewModel.registrarDevolucion(id, nov, grave) },
                onBackClick = { navController.popBackStack() }
            )
        }
        
        composable(Screen.Historial.route) {
            // HU 10: Historial simplificado
            HistorialScreen(
                solicitudes = uiState.solicitudes,
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
