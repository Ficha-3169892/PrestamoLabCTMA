package com.example.prestamolabctma.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.prestamolabctma.model.EstadoSolicitud
import com.example.prestamolabctma.model.SolicitudPrestamo
import java.time.Duration
import java.time.LocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MisPrestamosScreen(
    solicitudes: List<SolicitudPrestamo>,
    onCancelarClick: (Int) -> Unit,
    onExtenderClick: (Int) -> Unit,
    onReportarFalla: (Int) -> Unit,
    onBackClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Mi Seguimiento") },
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
                Text("No tienes solicitudes activas", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Ordenar por fecha de vencimiento próxima (HU 05)
                val ordenadas = solicitudes.sortedBy { it.fechaInicio.plusHours(it.duracionHoras.toLong()) }
                items(ordenadas) { solicitud ->
                    SolicitudSeguimientoCard(
                        solicitud = solicitud, 
                        onCancelar = { onCancelarClick(solicitud.id) },
                        onExtender = { onExtenderClick(solicitud.id) },
                        onReportar = { onReportarFalla(solicitud.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun SolicitudSeguimientoCard(
    solicitud: SolicitudPrestamo, 
    onCancelar: () -> Unit,
    onExtender: () -> Unit,
    onReportar: () -> Unit
) {
    val fechaVencimiento = solicitud.fechaInicio.plusHours(solicitud.duracionHoras.toLong())
    val tiempoRestante = Duration.between(LocalDateTime.now(), fechaVencimiento)
    val minutosRestantes = tiempoRestante.toMinutes()
    
    // HU 05: Indicador visual de advertencia
    val colorAlerta = when {
        minutosRestantes < 0 -> MaterialTheme.colorScheme.error
        minutosRestantes < 15 -> Color(0xFFFF9800) // Naranja
        else -> MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = if (minutosRestantes < 15 && solicitud.estado == EstadoSolicitud.ENTREGADA) 
            CardDefaults.cardColors(containerColor = colorAlerta.copy(alpha = 0.05f))
        else CardDefaults.cardColors()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Reserva RES-${solicitud.id}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                StatusChip(solicitud.estado)
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            Text("Equipo ID: ${solicitud.equipoId}", style = MaterialTheme.typography.bodyMedium)
            
            if (solicitud.estado == EstadoSolicitud.ENTREGADA) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (minutosRestantes < 15) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = colorAlerta, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        text = if (minutosRestantes > 0) "Entrega en: $minutosRestantes min" else "¡ENTREGA ATRASADA!",
                        color = colorAlerta,
                        fontWeight = FontWeight.Bold
                    )
                }
                LinearProgressIndicator(
                    progress = (minutosRestantes.toFloat() / (solicitud.duracionHoras * 60)).coerceIn(0f, 1f),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    color = colorAlerta
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (solicitud.estado == EstadoSolicitud.SOLICITADA) {
                    OutlinedButton(onClick = onCancelar, modifier = Modifier.weight(1f)) {
                        Text("Cancelar")
                    }
                }
                
                if (solicitud.estado == EstadoSolicitud.ENTREGADA) {
                    // HU 08: Extensión y HU 09: Reporte
                    OutlinedButton(onClick = onExtender, modifier = Modifier.weight(1f)) {
                        Text("Renovar")
                    }
                    Button(onClick = onReportar, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                        Text("Reportar Falla")
                    }
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
        EstadoSolicitud.ENTREGADA -> Color(0xFFFBC02D)
        EstadoSolicitud.CANCELADA -> Color(0xFF757575)
        EstadoSolicitud.RECHAZADA -> Color(0xFFD32F2F)
        EstadoSolicitud.DEVUELTA -> Color(0xFF4CAF50)
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
