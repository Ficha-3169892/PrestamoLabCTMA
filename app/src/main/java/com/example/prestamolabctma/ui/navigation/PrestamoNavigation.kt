package com.example.prestamolabctma.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.prestamolabctma.ui.catalogo.CatalogoScreen
import com.example.prestamolabctma.ui.equipo.EquipoDetalleScreen
import com.example.prestamolabctma.ui.solicitud.FormularioSolicitudScreen
import com.example.prestamolabctma.ui.viewmodel.PrestamoViewModel

sealed class Screen(val route: String) {
    object Catalogo : Screen("catalogo")
    object Detalle : Screen("detalle/{equipoId}") {
        fun createRoute(equipoId: Int) = "detalle/$equipoId"
    }
    object Solicitud : Screen("solicitud/{equipoId}") {
        fun createRoute(equipoId: Int) = "solicitud/$equipoId"
    }
}

@Composable
fun NavGraph(
    viewModel: PrestamoViewModel,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Catalogo.route
    ) {
        composable(Screen.Catalogo.route) {
            CatalogoScreen(
                viewModel = viewModel,
                onNavigateToDetail = { id ->
                    navController.navigate(Screen.Detalle.createRoute(id))
                },
                onNavigateToMisSolicitudes = {
                    // Por implementar
                }
            )
        }
        composable(
            route = Screen.Detalle.route,
            arguments = listOf(navArgument("equipoId") { type = NavType.IntType })
        ) { backStackEntry ->
            val equipoId = backStackEntry.arguments?.getInt("equipoId") ?: 0
            EquipoDetalleScreen(
                equipoId = equipoId,
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onNavigateToSolicitar = { id ->
                    navController.navigate(Screen.Solicitud.createRoute(id))
                }
            )
        }
        composable(
            route = Screen.Solicitud.route,
            arguments = listOf(navArgument("equipoId") { type = NavType.IntType })
        ) { backStackEntry ->
            val equipoId = backStackEntry.arguments?.getInt("equipoId") ?: 0
            FormularioSolicitudScreen(
                equipoId = equipoId,
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onSuccess = {
                    navController.popBackStack(Screen.Catalogo.route, false)
                }
            )
        }
    }
}
