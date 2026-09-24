package com.example.prestamolabctma.ui.navigation

import android.Manifest
import android.content.Context
import android.os.Build
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
    data object Catalogo : Screen("catalogo", "Catálogo")
    data object EquipoDetalle : Screen("detalle/{equipoId}") {
        fun createRoute(equipoId: Int) = "detalle/$equipoId"
    }
    data object Solicitar : Screen("solicitar/{equipoId}") {
        fun createRoute(equipoId: Int) = "solicitar/$equipoId"
    }
    data object MisSolicitudes : Screen("mis_solicitudes", "Mis Préstamos")
    data object ReportarFalla : Screen("reportar_falla/{solicitudId}") {
        fun createRoute(solicitudId: Int) = "reportar_falla/$solicitudId"
    }
    data object Admin : Screen("admin", "Gestión")
    data object Historial : Screen("historial", "Historial")
}

@Composable
fun PrestamoApp(viewModel: PrestamoViewModel) {
    val navController = rememberNavController()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
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
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Home, null) },
                        label = { Text(Screen.Catalogo.label) },
                        selected = currentDestination?.hierarchy?.any { it.route == Screen.Catalogo.route } == true,
                        onClick = { navigateBottom(navController, Screen.Catalogo.route) }
                    )
                    
                    if (usuario.rol == Role.APRENDIZ || usuario.rol == Role.INSTRUCTOR) {
                        NavigationBarItem(
                            icon = { Icon(Icons.AutoMirrored.Filled.List, null) },
                            label = { Text(Screen.MisSolicitudes.label) },
                            selected = currentDestination?.hierarchy?.any { it.route == Screen.MisSolicitudes.route } == true,
                            onClick = { navigateBottom(navController, Screen.MisSolicitudes.route) }
                        )
                    }

                    if (usuario.rol == Role.ADMIN || usuario.rol == Role.CUENTADANTE) {
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.Settings, null) },
                            label = { Text(Screen.Admin.label) },
                            selected = currentDestination?.hierarchy?.any { it.route == Screen.Admin.route } == true,
                            onClick = { navigateBottom(navController, Screen.Admin.route) }
                        )
                    }

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
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val locationHelper = remember { LocationHelper(context) }
    val notificationHelper = remember { NotificationHelper(context) }

    // Launcher para solicitar permiso de Ubicación bajo demanda
    var pendingDevolucionParams by remember { mutableStateOf<Triple<Int, String?, Boolean>?>(null) }
    val requestLocationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        pendingDevolucionParams?.let { (id, nov, grave) ->
            viewModel.registrarDevolucionConUbicacion(id, nov, grave, locationHelper)
            pendingDevolucionParams = null
        }
    }

    // Launcher para solicitar permiso de Notificaciones bajo demanda
    var pendingNotificationSolicitudId by remember { mutableStateOf<Int?>(null) }
    val requestNotificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        pendingNotificationSolicitudId?.let { id ->
            if (granted) {
                viewModel.activarRecordatorioNotificacion(id, notificationHelper)
            }
            pendingNotificationSolicitudId = null
        }
    }

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
                onExtenderClick = { id ->
                    if (!notificationHelper.tienePermisoNotificaciones() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        pendingNotificationSolicitudId = id
                        requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        viewModel.activarRecordatorioNotificacion(id, notificationHelper)
                    }
                },
                onReportarFalla = { solicitudId -> 
                    navController.navigate(Screen.ReportarFalla.createRoute(solicitudId))
                },
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.ReportarFalla.route,
            arguments = listOf(navArgument("solicitudId") { type = NavType.IntType })
        ) { backStackEntry ->
            val solicitudId = backStackEntry.arguments?.getInt("solicitudId") ?: -1
            ReportarFallaScreen(
                solicitudId = solicitudId,
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Admin.route) {
            AdminScreen(
                solicitudes = uiState.solicitudes,
                onProcesar = { id, ok, mot -> viewModel.procesarSolicitud(id, ok, mot) },
                onDevolver = { id, nov, grave ->
                    if (!locationHelper.tienePermisoUbicacion()) {
                        pendingDevolucionParams = Triple(id, nov, grave)
                        requestLocationPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    } else {
                        viewModel.registrarDevolucionConUbicacion(id, nov, grave, locationHelper)
                    }
                },
                onBackClick = { navController.popBackStack() }
            )
        }
        
        composable(Screen.Historial.route) {
            HistorialScreen(
                solicitudes = uiState.solicitudes,
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
