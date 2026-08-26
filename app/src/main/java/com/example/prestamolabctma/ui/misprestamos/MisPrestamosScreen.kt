package com.example.prestamolabctma.ui.misprestamos

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.prestamolabctma.model.EstadoSolicitud
import com.example.prestamolabctma.model.SolicitudPrestamo
import com.example.prestamolabctma.model.Equipo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MisPrestamosScreen(
    solicitudes: List<SolicitudPrestamo>,
    equipos: List<Equipo>,
    onCancelarSolicitud: (Int) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Mis Préstamos") })
        }
    ) { padding ->
        if (solicitudes.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Text("No tienes solicitudes activas")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(solicitudes) { solicitud ->
                    val equipo = equipos.find { it.id == solicitud.equipoId }
                    SolicitudItem(
                        solicitud = solicitud,
                        nombreEquipo = equipo?.nombre ?: "Equipo Desconocido",
                        onCancelar = { onCancelarSolicitud(solicitud.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun SolicitudItem(
    solicitud: SolicitudPrestamo,
    nombreEquipo: String,
    onCancelar: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = nombreEquipo, style = MaterialTheme.typography.titleMedium)
            Text(text = "Ambiente: ${solicitud.ambienteDestino}", style = MaterialTheme.typography.bodyMedium)
            Text(text = "Estado: ${solicitud.estado}", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
            
            if (solicitud.estado == EstadoSolicitud.SOLICITADA) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onCancelar,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cancelar Solicitud")
                }
            }
        }
    }
}
