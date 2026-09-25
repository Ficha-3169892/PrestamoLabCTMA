package com.example.prestamolabctma.ui.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.prestamolabctma.model.SolicitudPrestamo
import com.example.prestamolabctma.ui.viewmodel.PrestamoViewModel
import java.time.format.DateTimeFormatter

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistorialScreen(
    solicitudes: List<SolicitudPrestamo>,
    viewModel: PrestamoViewModel,
    onBackClick: () -> Unit,
    onLogoutClick: () -> Unit = {}
) {
    val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
    var nombresAprendices by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    LaunchedEffect(solicitudes) {
        val ids = solicitudes.map { it.aprendizId }.distinct()
        val mapa = mutableMapOf<String, String>()
        for (id in ids) {
            val nombre = viewModel.obtenerNombreUsuario(id)
            mapa[id] = nombre
        }
        nombresAprendices = mapa
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Trazabilidad de Préstamos") },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar")
                }
            },
            actions = {
                IconButton(onClick = onLogoutClick) {
                    Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Cerrar Sesión")
                }
            }
        )

        if (solicitudes.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No hay registros en el historial")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val historial = solicitudes.sortedByDescending { it.fechaSolicitud }
                items(historial) { sol ->
                    val nombreAprendiz = nombresAprendices[sol.aprendizId] ?: sol.aprendizId.take(8)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Ticket: RES-${sol.id.take(8)}", fontWeight = FontWeight.Bold)
                                Text(sol.estado.name, style = MaterialTheme.typography.labelSmall)
                            }
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Equipo: ${sol.equipoNombre} (${sol.equipoPlaca})", style = MaterialTheme.typography.bodySmall)
                            Text("Aprendiz: $nombreAprendiz", style = MaterialTheme.typography.bodySmall)
                            Text("Solicitado: ${sol.fechaSolicitud.format(dateFormatter)}", style = MaterialTheme.typography.bodySmall)
                            
                            if (sol.motivoRechazo != null) {
                                Divider(modifier = Modifier.padding(vertical = 8.dp))
                                Text("Motivo / Novedad:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                                Text(sol.motivoRechazo!!, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}
