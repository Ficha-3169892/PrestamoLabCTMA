package com.example.prestamolabctma.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.prestamolabctma.model.EstadoSolicitud
import com.example.prestamolabctma.model.SolicitudPrestamo
import com.example.prestamolabctma.ui.viewmodel.PrestamoViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    solicitudes: List<SolicitudPrestamo>,
    instructorId: String,
    viewModel: PrestamoViewModel,
    onAprobar: (String, String?) -> Unit,
    onRechazar: (String) -> Unit,
    onEntregar: (String) -> Unit,
    onDevolver: (String, String?) -> Unit,
    onBackClick: () -> Unit,
    onLogoutClick: () -> Unit = {}
) {
    var tabSelected by remember { mutableStateOf(0) }
    val tabs = listOf("Pendientes", "Por Entregar", "En Curso")
    var nombresAprendices by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    val misSolicitudes = solicitudes.filter { it.instructorId == instructorId }

    LaunchedEffect(misSolicitudes) {
        val ids = misSolicitudes.map { it.aprendizId }.distinct()
        val mapa = mutableMapOf<String, String>()
        for (id in ids) {
            val nombre = viewModel.obtenerNombreUsuario(id)
            mapa[id] = nombre
        }
        nombresAprendices = mapa
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Panel de Gestión") },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Text("←")
                }
            },
            actions = {
                IconButton(onClick = onLogoutClick) {
                    Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Cerrar Sesión")
                }
            }
        )

        TabRow(selectedTabIndex = tabSelected) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = tabSelected == index,
                    onClick = { tabSelected = index },
                    text = { Text(title) }
                )
            }
        }

        val listaMostrar = when (tabSelected) {
            0 -> misSolicitudes.filter { it.estado == EstadoSolicitud.SOLICITADA }
            1 -> misSolicitudes.filter { it.estado == EstadoSolicitud.APROBADA }
            else -> misSolicitudes.filter { it.estado == EstadoSolicitud.ENTREGADA }
        }

        if (listaMostrar.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No hay solicitudes en esta categoría")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(listaMostrar) { solicitud ->
                    val nombreAprendiz = nombresAprendices[solicitud.aprendizId] ?: solicitud.aprendizId.take(8)
                    when (tabSelected) {
                        0 -> SolicitudPendienteCard(solicitud, nombreAprendiz, onAprobar, onRechazar)
                        1 -> SolicitudAprobadaCard(solicitud, nombreAprendiz, onEntregar)
                        else -> PrestamoEnCursoCard(solicitud, nombreAprendiz, onDevolver)
                    }
                }
            }
        }
    }
}

@Composable
fun SolicitudPendienteCard(
    solicitud: SolicitudPrestamo,
    nombreAprendiz: String,
    onAprobar: (String, String?) -> Unit,
    onRechazar: (String) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Solicitud #${solicitud.id.take(8)}", style = MaterialTheme.typography.titleMedium)
            Text("Equipo: ${solicitud.equipoNombre} (${solicitud.equipoPlaca})")
            Text("Aprendiz: $nombreAprendiz")
            Text("Fecha: ${solicitud.fechaSolicitud}")
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { onAprobar(solicitud.id, solicitud.equipoId) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                ) {
                    Icon(Icons.Default.Check, null)
                    Spacer(Modifier.width(4.dp))
                    Text("Aprobar")
                }
                OutlinedButton(
                    onClick = { onRechazar(solicitud.id) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
                ) {
                    Icon(Icons.Default.Close, null)
                    Spacer(Modifier.width(4.dp))
                    Text("Rechazar")
                }
            }
        }
    }
}

@Composable
fun SolicitudAprobadaCard(
    solicitud: SolicitudPrestamo,
    nombreAprendiz: String,
    onEntregar: (String) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Aprobada #${solicitud.id.take(8)}", style = MaterialTheme.typography.titleMedium)
            Text("Equipo: ${solicitud.equipoNombre} (${solicitud.equipoPlaca})")
            Text("Aprendiz: $nombreAprendiz")
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = { onEntregar(solicitud.id) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.LocalShipping, null)
                Spacer(Modifier.width(8.dp))
                Text("Marcar Entregado")
            }
        }
    }
}

@Composable
fun PrestamoEnCursoCard(
    solicitud: SolicitudPrestamo,
    nombreAprendiz: String,
    onDevolver: (String, String?) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("En Curso #${solicitud.id.take(8)}", style = MaterialTheme.typography.titleMedium)
            Text("Equipo: ${solicitud.equipoNombre} (${solicitud.equipoPlaca})")
            Text("Aprendiz: $nombreAprendiz")
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = { onDevolver(solicitud.id, solicitud.equipoId) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Refresh, null)
                Spacer(Modifier.width(8.dp))
                Text("Marcar Devuelto")
            }
        }
    }
}
