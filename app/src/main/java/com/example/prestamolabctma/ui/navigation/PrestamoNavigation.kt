package com.example.prestamolabctma.ui.navigation

import android.Manifest
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import com.example.prestamolabctma.util.LocationHelper
import com.example.prestamolabctma.util.NotificationHelper

sealed class Screen(val route: String, val label: String = "") {
    data object Login : Screen("login")
    data object Register : Screen("register")
    data object Catalogo : Screen("catalogo", "Catálogo")
    data object EquipoDetalle : Screen("detalle/{equipoId}") {
        fun createRoute(equipoId: String) = "detalle/$equipoId"
    }
    data object Solicitar : Screen("solicitar/{equipoId}") {
        fun createRoute(equipoId: String) = "solicitar/$equipoId"
    }
    data object MisSolicitudes : Screen("mis_solicitudes", "Mis Préstamos")
    data object EquiposPrestados : Screen("equipos_prestados", "Prestados")
    data object ReportarFalla : Screen("reportar_falla/{solicitudId}") {
        fun createRoute(solicitudId: String) = "reportar_falla/$solicitudId"
    }
    data object VerReporte : Screen("ver_reporte/{reporteId}", "Ver Reporte") {
        fun createRoute(reporteId: String) = "ver_reporte/$reporteId"
    }
    data object Admin : Screen("admin", "Gestión")
    data object Historial : Screen("historial", "Historial")
    data object GestionCatalogo : Screen("gestion_catalogo", "Gestión Catálogo")
    data object Reportes : Screen("reportes_novedad", "Reportes")
}

@Composable
fun PrestamoApp(viewModel: PrestamoViewModel) {
    val navController = rememberNavController()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showLogoutDialog by remember { mutableStateOf(false) }

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

            if (usuario != null && currentDestination?.route != Screen.Login.route && currentDestination?.route != Screen.Register.route) {
                Log.d("EquipoDebug", "BottomBar: usuario=${usuario.correo}, rol=${usuario.rol}")
                NavigationBar {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Home, null) },
                        label = { Text(Screen.Catalogo.label) },
                        selected = currentDestination?.hierarchy?.any { it.route == Screen.Catalogo.route } == true,
                        onClick = { navigateBottom(navController, Screen.Catalogo.route) }
                    )
                    
                    if (usuario.rol == Role.APRENDIZ) {
                        Log.d("EquipoDebug", "BottomBar: mostrando items para rol APRENDIZ (Prestados, MisSolicitudes)")
                        NavigationBarItem(
                            icon = { Icon(Icons.AutoMirrored.Filled.List, null) },
                            label = { Text("Prestados") },
                            selected = currentDestination?.hierarchy?.any { it.route == Screen.EquiposPrestados.route } == true,
                            onClick = { 
                                Log.d("EquipoDebug", "BottomBar: click en Equipos Prestados")
                                navigateBottom(navController, Screen.EquiposPrestados.route) 
                            }
                        )
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.Person, null) },
                            label = { Text(Screen.MisSolicitudes.label) },
                            selected = currentDestination?.hierarchy?.any { it.route == Screen.MisSolicitudes.route } == true,
                            onClick = { navigateBottom(navController, Screen.MisSolicitudes.route) }
                        )
                    }

                    if (usuario.rol == Role.INSTRUCTOR) {
                        Log.d("EquipoDebug", "BottomBar: mostrando items para rol INSTRUCTOR (Admin, Reportes, Historial)")
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.Settings, null) },
                            label = { Text(Screen.Admin.label) },
                            selected = currentDestination?.hierarchy?.any { it.route == Screen.Admin.route } == true,
                            onClick = { navigateBottom(navController, Screen.Admin.route) }
                        )
                        NavigationBarItem(
                            icon = { Icon(Icons.AutoMirrored.Filled.List, null) },
                            label = { Text("Reportes") },
                            selected = currentDestination?.hierarchy?.any { it.route == Screen.Reportes.route } == true,
                            onClick = { 
                                Log.d("EquipoDebug", "BottomBar: click en Reportes (Instructor)")
                                navigateBottom(navController, Screen.Reportes.route) 
                            }
                        )
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
            onLogoutClick = { showLogoutDialog = true },
            modifier = Modifier.padding(innerPadding)
        )
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Cerrar Sesión") },
            text = { Text("¿Estás seguro de que deseas cerrar sesión de forma segura?") },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        viewModel.logout()
                        navController.navigate(Screen.Login.route) {
                            popUpTo(navController.graph.id) { inclusive = true }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Cerrar Sesión")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

private fun navigateBottom(navController: NavHostController, route: String) {
    navController.navigate(route) {
        popUpTo(Screen.Catalogo.route) {
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
    onLogoutClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    NavHost(
        navController = navController,
        startDestination = Screen.Login.route,
        modifier = modifier
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.Catalogo.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onRegisterClick = {
                    navController.navigate(Screen.Register.route)
                }
            )
        }

        composable(Screen.Register.route) {
            RegisterScreen(
                onNavigateBack = { navController.popBackStack() },
                onRegisterSuccess = { navController.popBackStack() }
            )
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
                },
                onGestionCatalogo = if (uiState.usuarioLogueado?.rol == Role.INSTRUCTOR) {
                    { navController.navigate(Screen.GestionCatalogo.route) }
                } else null,
                onLogoutClick = onLogoutClick
            )
        }

        composable(
            route = Screen.EquipoDetalle.route,
            arguments = listOf(navArgument("equipoId") { type = NavType.StringType })
        ) { backStackEntry ->
            val equipoId = backStackEntry.arguments?.getString("equipoId") ?: ""
            val equipo = uiState.equipos.find { it.id == equipoId }

            EquipoDetalleScreen(
                equipo = equipo,
                userRole = uiState.usuarioLogueado?.rol,
                onSolicitarClick = { equipoObj ->
                    val usuario = uiState.usuarioLogueado
                    if (usuario != null) {
                        viewModel.solicitarPrestamo(equipoObj)
                        navController.navigate(Screen.MisSolicitudes.route) {
                            popUpTo(Screen.Catalogo.route)
                        }
                    }
                },
                onUpdateEstado = { eq, nuevoEstado ->
                    viewModel.actualizarEstadoEquipoInstructor(eq, nuevoEstado)
                },
                onBackClick = { navController.popBackStack() },
                onLogoutClick = onLogoutClick
            )
        }

        composable(Screen.MisSolicitudes.route) {
            val usuario = uiState.usuarioLogueado
            if (usuario != null && usuario.rol != Role.APRENDIZ) {
                LaunchedEffect(Unit) {
                    navController.navigate(Screen.Catalogo.route) {
                        popUpTo(Screen.Catalogo.route) { inclusive = true }
                    }
                }
            } else {
                MisPrestamosScreen(
                    solicitudes = uiState.solicitudes,
                    aprendizId = usuario?.id ?: "",
                    onBackClick = { navController.popBackStack() },
                    onLogoutClick = onLogoutClick
                )
            }
        }

        composable(Screen.EquiposPrestados.route) {
            val usuario = uiState.usuarioLogueado
            if (usuario != null && usuario.rol != Role.APRENDIZ) {
                LaunchedEffect(Unit) {
                    navController.navigate(Screen.Catalogo.route) {
                        popUpTo(Screen.Catalogo.route) { inclusive = true }
                    }
                }
            } else {
                EquiposPrestadosScreen(
                    solicitudes = uiState.solicitudes,
                    aprendizId = usuario?.id ?: "",
                    viewModel = viewModel,
                    onReportarNovedadClick = { solicitudId ->
                        Log.d("EquipoDebug", "PrestamoNavigation: navegando a ReportarFalla para solicitudId=$solicitudId")
                        navController.navigate(Screen.ReportarFalla.createRoute(solicitudId))
                    },
                    onVerReporteClick = { reporteId ->
                        Log.d("EquipoDebug", "PrestamoNavigation: navegando a VerReporte para reporteId=$reporteId")
                        navController.navigate(Screen.VerReporte.createRoute(reporteId))
                    },
                    onBackClick = { navController.popBackStack() },
                    onLogoutClick = onLogoutClick
                )
            }
        }

        composable(
            route = Screen.ReportarFalla.route,
            arguments = listOf(navArgument("solicitudId") { type = NavType.StringType })
        ) { backStackEntry ->
            val usuario = uiState.usuarioLogueado
            if (usuario != null && usuario.rol != Role.APRENDIZ) {
                LaunchedEffect(Unit) {
                    navController.navigate(Screen.Catalogo.route) {
                        popUpTo(Screen.Catalogo.route) { inclusive = true }
                    }
                }
            } else {
                val solicitudId = backStackEntry.arguments?.getString("solicitudId") ?: ""
                ReportarFallaScreen(
                    solicitudId = solicitudId,
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }

        composable(
            route = Screen.VerReporte.route,
            arguments = listOf(navArgument("reporteId") { type = NavType.StringType })
        ) { backStackEntry ->
            val usuario = uiState.usuarioLogueado
            if (usuario != null && usuario.rol != Role.APRENDIZ) {
                LaunchedEffect(Unit) {
                    navController.navigate(Screen.Catalogo.route) {
                        popUpTo(Screen.Catalogo.route) { inclusive = true }
                    }
                }
            } else {
                val reporteId = backStackEntry.arguments?.getString("reporteId") ?: ""
                VerReporteScreen(
                    reporteId = reporteId,
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }

        composable(Screen.Admin.route) {
            val usuario = uiState.usuarioLogueado
            if (usuario != null && usuario.rol != Role.INSTRUCTOR) {
                LaunchedEffect(Unit) {
                    navController.navigate(Screen.Catalogo.route) {
                        popUpTo(Screen.Catalogo.route) { inclusive = true }
                    }
                }
            } else {
                AdminScreen(
                    solicitudes = uiState.solicitudes,
                    instructorId = usuario?.id ?: "",
                    viewModel = viewModel,
                    onAprobar = { id, eqId -> viewModel.aprobarPrestamo(id, eqId) },
                    onRechazar = { id -> viewModel.rechazarPrestamo(id) },
                    onEntregar = { id -> viewModel.entregarPrestamo(id) },
                    onDevolver = { id, eqId -> viewModel.devolverPrestamo(id, eqId) },
                    onBackClick = { navController.popBackStack() },
                    onLogoutClick = onLogoutClick
                )
            }
        }
        
        composable(Screen.Historial.route) {
            val usuario = uiState.usuarioLogueado
            if (usuario != null && usuario.rol != Role.INSTRUCTOR) {
                LaunchedEffect(Unit) {
                    navController.navigate(Screen.Catalogo.route) {
                        popUpTo(Screen.Catalogo.route) { inclusive = true }
                    }
                }
            } else {
                Log.d("EquipoDebug", "Historial route: instructorId logueado = '${usuario?.id}', total solicitudes = ${uiState.solicitudes.size}")
                uiState.solicitudes.forEach { s ->
                    Log.d("EquipoDebug", " - Solicitud id=${s.id}, equipo=${s.equipoNombre}, sol.instructorId='${s.instructorId}', match=${s.instructorId == usuario?.id}")
                }
                val filtered = uiState.solicitudes.filter { it.instructorId == usuario?.id }
                Log.d("EquipoDebug", "Historial route: filtradas count = ${filtered.size}")
                HistorialScreen(
                    solicitudes = filtered,
                    viewModel = viewModel,
                    onBackClick = { navController.popBackStack() },
                    onLogoutClick = onLogoutClick
                )
            }
        }

        composable(Screen.Reportes.route) {
            val usuario = uiState.usuarioLogueado
            if (usuario != null && usuario.rol != Role.INSTRUCTOR) {
                LaunchedEffect(Unit) {
                    navController.navigate(Screen.Catalogo.route) {
                        popUpTo(Screen.Catalogo.route) { inclusive = true }
                    }
                }
            } else {
                ReportesScreen(
                    viewModel = viewModel,
                    instructorId = usuario?.id ?: "",
                    onBackClick = { navController.popBackStack() },
                    onLogoutClick = onLogoutClick
                )
            }
        }

        composable(Screen.GestionCatalogo.route) {
            val usuario = uiState.usuarioLogueado
            if (usuario != null && usuario.rol != Role.INSTRUCTOR) {
                LaunchedEffect(Unit) {
                    navController.navigate(Screen.Catalogo.route) {
                        popUpTo(Screen.Catalogo.route) { inclusive = true }
                    }
                }
            } else {
                GestionCatalogoScreen(
                    onBackClick = { navController.popBackStack() },
                    onLogoutClick = onLogoutClick
                )
            }
        }
    }
}
