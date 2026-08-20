package com.example.prestamolabctma.ui.misprestamos

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.prestamolabctma.domain.model.EstadoSolicitud
import com.example.prestamolabctma.domain.model.SolicitudPrestamo
import com.example.prestamolabctma.ui.viewmodel.PrestamoViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MisSolicitudesScreen(
    viewModel: PrestamoViewModel,
    onNavigateToDetail: (Int) -> Unit,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mis Solicitudes") }
            )
        }
    ) { innerPadding ->
        if (uiState.solicitudes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("Aún no has realizado ninguna solicitud.")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.solicitudes) { solicitud ->
                    val equipo = uiState.equipos.find { it.id == solicitud.equipoId }
                    SolicitudCard(
                        solicitud = solicitud,
                        nombreEquipo = equipo?.nombre ?: "Equipo ID: ${solicitud.equipoId}",
                        onClick = { onNavigateToDetail(solicitud.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun SolicitudCard(
    solicitud: SolicitudPrestamo,
    nombreEquipo: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = nombreEquipo,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "ID Solicitud: #${solicitud.id}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            
            SolicitudStatusBadge(estado = solicitud.estado)
        }
    }
}

@Composable
fun SolicitudStatusBadge(estado: EstadoSolicitud) {
    val color = when (estado) {
        EstadoSolicitud.SOLICITADA -> Color(0xFF2196F3) // Blue
        EstadoSolicitud.APROBADA -> Color(0xFF4CAF50)   // Green
        EstadoSolicitud.ENTREGADA -> Color(0xFF9C27B0)  // Purple
        EstadoSolicitud.DEVUELTA -> Color(0xFF757575)   // Grey
        EstadoSolicitud.CANCELADA -> Color(0xFFF44336)  // Red
        EstadoSolicitud.RECHAZADA -> Color(0xFFFF9800)  // Orange
    }

    Surface(
        shape = MaterialTheme.shapes.extraSmall,
        color = color.copy(alpha = 0.1f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color)
    ) {
        Text(
            text = estado.name,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}
