package com.example.prestamolabctma.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.prestamolabctma.model.SolicitudPrestamo
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistorialScreen(
    solicitudes: List<SolicitudPrestamo>,
    onBackClick: () -> Unit
) {
    val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Trazabilidad de Préstamos") },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar")
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
                // HU 10: Lista cronológica
                val historial = solicitudes.sortedByDescending { it.fechaSolicitud }
                items(historial) { sol ->
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
                                Text("Ticket: RES-${sol.id}", fontWeight = FontWeight.Bold)
                                StatusChip(sol.estado)
                            }
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Equipo: ${sol.equipoId}", style = MaterialTheme.typography.bodySmall)
                            Text("Aprendiz: ${sol.usuarioId}", style = MaterialTheme.typography.bodySmall)
                            Text("Solicitado: ${sol.fechaSolicitud.format(dateFormatter)}", style = MaterialTheme.typography.bodySmall)
                            
                            if (sol.novedadDevolucion != null) {
                                Divider(modifier = Modifier.padding(vertical = 8.dp))
                                Text("Novedad reportada:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                                Text(sol.novedadDevolucion!!, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}
