package com.example.prestamolabctma.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.prestamolabctma.model.Equipo
import com.example.prestamolabctma.model.EstadoEquipo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EquipoDetalleScreen(
    equipo: Equipo?,
    onSolicitarClick: (Int) -> Unit,
    onBackClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Detalle de Herramienta") },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar")
                }
            }
        )
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (equipo == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Equipo no encontrado", style = MaterialTheme.typography.headlineSmall)
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        // HU 03: Ficha técnica detallada
                        Text(text = equipo.nombre, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Text(text = "Placa: ${equipo.placa}", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.secondary)
                        
                        Divider(modifier = Modifier.padding(vertical = 12.dp))
                        
                        Text(text = "Categoría: ${equipo.categoria}", style = MaterialTheme.typography.bodyMedium)
                        Text(text = "Ubicación: ${equipo.ubicacion}", style = MaterialTheme.typography.bodyMedium)
                        
                        if (equipo.observaciones.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = "Observaciones técnicas:", style = MaterialTheme.typography.labelLarge)
                            Text(text = equipo.observaciones, style = MaterialTheme.typography.bodySmall)
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // HU 03: Reglas de negocio (Disponibilidad y Reparación)
                        val puedeSolicitar = equipo.estado == EstadoEquipo.DISPONIBLE
                        val esReparacion = equipo.estado == EstadoEquipo.REPARACION || equipo.estado == EstadoEquipo.MANTENIMIENTO
                        
                        StatusBadgeLarge(equipo.estado)

                        if (equipo.estado == EstadoEquipo.PRESTADO) {
                            Text(
                                text = "Fecha estimada de devolución: Mañana 08:00 AM",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = { onSolicitarClick(equipo.id) },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = puedeSolicitar && !esReparacion,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (esReparacion) Color.Gray else MaterialTheme.colorScheme.primary
                            )
                        ) {
                            val btnText = when {
                                esReparacion -> "En mantenimiento / Reparación"
                                !puedeSolicitar -> "No disponible"
                                else -> "Iniciar Solicitud de Préstamo"
                            }
                            Text(btnText)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatusBadgeLarge(estado: EstadoEquipo) {
    val color = when (estado) {
        EstadoEquipo.DISPONIBLE -> Color(0xFF4CAF50)
        EstadoEquipo.RESERVADO -> Color(0xFFFF9800)
        EstadoEquipo.PRESTADO -> Color(0xFFF44336)
        EstadoEquipo.MANTENIMIENTO, EstadoEquipo.REPARACION -> Color(0xFF9E9E9E)
    }
    Surface(
        color = color.copy(alpha = 0.1f),
        contentColor = color,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        Text(
            text = "Estado: ${estado.name}",
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
    }
}
