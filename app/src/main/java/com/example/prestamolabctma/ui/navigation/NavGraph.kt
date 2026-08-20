package com.example.prestamolabctma.ui.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.prestamolabctma.ui.catalogo.CatalogoScreen
import com.example.prestamolabctma.ui.equipo.EquipoDetalleScreen
import com.example.prestamolabctma.ui.misprestamos.MisSolicitudesScreen
import com.example.prestamolabctma.ui.misprestamos.SolicitudDetalleScreen
import com.example.prestamolabctma.ui.solicitud.FormularioSolicitudScreen
import com.example.prestamolabctma.ui.viewmodel.PrestamoViewModel

sealed class Destinos(val ruta: String) {
    object Catalogo : Destinos("Catalogo")
    object EquipoDetalle : Destinos("EquipoDetalle/{equipoId}") {
        fun crearRuta(id: Int) = "EquipoDetalle/$id"
    }
    object Solicitar : Destinos("Solicitar/{equipoId}") {
        fun crearRuta(id: Int) = "Solicitar/$id"
    }
    object MisSolicitudes : Destinos("MisSolicitudes")
    object SolicitudDetalle : Destinos("SolicitudDetalle/{solicitudId}") {
        fun crearRuta(id: Int) = "SolicitudDetalle/$id"
    }
}

@Composable
fun NavGraph(
    viewModel: PrestamoViewModel,
    navController: NavHostController = rememberNavController()
) {
    val uiState by viewModel.uiState.collectAsState()

    NavHost(
        navController = navController,
        startDestination = Destinos.Catalogo.ruta
    ) {
        composable(Destinos.Catalogo.ruta) {
            CatalogoScreen(
                viewModel = viewModel,
                onNavigateToDetail = { id ->
                    navController.navigate(Destinos.EquipoDetalle.crearRuta(id))
                },
                onNavigateToMisSolicitudes = {
                    navController.navigate(Destinos.MisSolicitudes.ruta)
                }
            )
        }

        composable(
            route = Destinos.EquipoDetalle.ruta,
            arguments = listOf(navArgument("equipoId") { type = NavType.IntType })
        ) { backStackEntry ->
            val equipoId = backStackEntry.arguments?.getInt("equipoId") ?: -1
            
            EquipoDetalleScreen(
                equipoId = equipoId,
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onNavigateToSolicitar = { id ->
                    navController.navigate(Destinos.Solicitar.crearRuta(id))
                }
            )
        }

        composable(
            route = Destinos.Solicitar.ruta,
            arguments = listOf(navArgument("equipoId") { type = NavType.IntType })
        ) { backStackEntry ->
            val equipoId = backStackEntry.arguments?.getInt("equipoId") ?: -1
            
            FormularioSolicitudScreen(
                equipoId = equipoId,
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onSuccess = {
                    navController.navigate(Destinos.MisSolicitudes.ruta) {
                        popUpTo(Destinos.Catalogo.ruta) { inclusive = false }
                    }
                }
            )
        }

        composable(Destinos.MisSolicitudes.ruta) {
            MisSolicitudesScreen(
                viewModel = viewModel,
                onNavigateToDetail = { id ->
                    navController.navigate(Destinos.SolicitudDetalle.crearRuta(id))
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Destinos.SolicitudDetalle.ruta,
            arguments = listOf(navArgument("solicitudId") { type = NavType.IntType })
        ) { backStackEntry ->
            val solicitudId = backStackEntry.arguments?.getInt("solicitudId") ?: -1
            
            val solicitud = uiState.solicitudes.find { it.id == solicitudId }

            if (solicitud == null) {
                ErrorRecursoNoEncontrado(onVolver = {
                    navController.navigate(Destinos.MisSolicitudes.ruta) {
                        popUpTo(Destinos.MisSolicitudes.ruta) { inclusive = true }
                    }
                })
            } else {
                SolicitudDetalleScreen(
                    solicitudId = solicitudId,
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

@Composable
fun ErrorRecursoNoEncontrado(onVolver: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "Recurso no encontrado",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "El elemento solicitado no existe o ha sido eliminado.",
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onVolver) {
            Text("Volver al Catálogo")
        }
    }
}
