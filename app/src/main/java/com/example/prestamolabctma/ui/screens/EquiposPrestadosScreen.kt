package com.example.prestamolabctma.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.prestamolabctma.model.EstadoSolicitud
import com.example.prestamolabctma.model.ReporteNovedad
import com.example.prestamolabctma.model.SolicitudPrestamo
import com.example.prestamolabctma.ui.viewmodel.PrestamoViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EquiposPrestadosScreen(
    solicitudes: List<SolicitudPrestamo>,
    aprendizId: String,
    viewModel: PrestamoViewModel,
    onReportarNovedadClick: (String) -> Unit,
    onVerReporteClick: (String) -> Unit,
    onBackClick: () -> Unit,
    onLogoutClick: () -> Unit = {}
) {
    Log.d("EquipoDebug", "EquiposPrestadosScreen: renderizando con ${solicitudes.size} solicitudes totales para aprendizId=$aprendizId")
    val prestamosEntregados = solicitudes.filter { it.aprendizId == aprendizId && it.estado == EstadoSolicitud.ENTREGADA }

    var reportesAprendiz by remember { mutableStateOf<List<ReporteNovedad>>(emptyList()) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(aprendizId) {
        coroutineScope.launch {
            val reps = viewModel.obtenerReportesAprendiz(aprendizId)
            Log.d("EquipoDebug", "EquiposPrestadosScreen: obtenidos ${reps.size} reportes del aprendiz")
            reportesAprendiz = reps
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Equipos en Mi Poder (Prestados)") },
                navigationIcon = {
                    TextButton(onClick = onBackClick) {
                        Text("← Volver")
                    }
                },
                actions = {
                    IconButton(onClick = onLogoutClick) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Cerrar Sesión")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (prestamosEntregados.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Text("No tienes equipos entregados en este momento.")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(prestamosEntregados) { solicitud ->
                    val reporteActivo = reportesAprendiz.find { it.prestamoId == solicitud.id && it.estado == "activo" }
                    Log.d("EquipoDebug", "EquiposPrestadosScreen: prestamoId=${solicitud.id} -> reporteActivo encontrado: ${reporteActivo != null}")

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(text = solicitud.equipoNombre, style = MaterialTheme.typography.titleMedium)
                            Text(text = "Placa: ${solicitud.equipoPlaca}", style = MaterialTheme.typography.bodyMedium)
                            Text(text = "Categoría: ${solicitud.equipoCategoria}", style = MaterialTheme.typography.bodySmall)

                            if (reporteActivo?.devolucionSolicitada == true) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Surface(
                                    color = MaterialTheme.colorScheme.errorContainer,
                                    shape = MaterialTheme.shapes.small
                                ) {
                                    Text(
                                        text = "⚠️ El instructor solicitó que devuelvas este equipo.",
                                        modifier = Modifier.padding(8.dp),
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))

                            if (reporteActivo != null) {
                                Button(
                                    onClick = {
                                        Log.d("EquipoDebug", "EquiposPrestadosScreen: botón Ver Reporte Activo click reporteId=${reporteActivo.id}")
                                        onVerReporteClick(reporteActivo.id)
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                                ) {
                                    Text("Ver Reporte Activo")
                                }
                            } else {
                                Button(
                                    onClick = {
                                        Log.d("EquipoDebug", "EquiposPrestadosScreen: botón Reportar Novedad click id=${solicitud.id}")
                                        onReportarNovedadClick(solicitud.id)
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Reportar Novedad / Falla")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
