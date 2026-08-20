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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.prestamolabctma.model.EstadoSolicitud
import com.example.prestamolabctma.model.SolicitudPrestamo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MisPrestamosScreen(
    solicitudes: List<SolicitudPrestamo>,
    onCancelarClick: (Int) -> Unit,
    onBackClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Mis Solicitudes") },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar")
                }
            }
        )
        
        if (solicitudes.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("No hay solicitudes registradas", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(solicitudes) { solicitud ->
                    SolicitudCard(solicitud, onCancelar = { onCancelarClick(solicitud.id) })
                }
            }
        }
    }
}

@Composable
fun SolicitudCard(solicitud: SolicitudPrestamo, onCancelar: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Solicitud #${solicitud.id}", style = MaterialTheme.typography.titleMedium)
                StatusChip(solicitud.estado)
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            Text("Equipo ID: ${solicitud.equipoId}", style = MaterialTheme.typography.bodyMedium)
            Text("Destino: ${solicitud.ambienteDestino}", style = MaterialTheme.typography.bodySmall)
            
            if (solicitud.estado == EstadoSolicitud.SOLICITADA) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onCancelar,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Cancelar Solicitud")
                }
            }
        }
    }
}

@Composable
fun StatusChip(estado: EstadoSolicitud) {
    val color = when (estado) {
        EstadoSolicitud.SOLICITADA -> Color(0xFF1976D2)
        EstadoSolicitud.APROBADA -> Color(0xFF388E3C)
        EstadoSolicitud.CANCELADA -> Color(0xFF757575)
        else -> Color.Gray
    }
    Surface(
        color = color.copy(alpha = 0.1f),
        contentColor = color,
        shape = MaterialTheme.shapes.extraSmall
    ) {
        Text(estado.name, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall)
    }
}
