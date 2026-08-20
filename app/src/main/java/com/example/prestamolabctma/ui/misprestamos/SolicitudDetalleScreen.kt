package com.example.prestamolabctma.ui.misprestamos

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.prestamolabctma.domain.model.EstadoSolicitud
import com.example.prestamolabctma.ui.viewmodel.PrestamoViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SolicitudDetalleScreen(
    solicitudId: Int,
    viewModel: PrestamoViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val solicitud = uiState.solicitudes.find { it.id == solicitudId }
    val equipo = uiState.equipos.find { it.id == (solicitud?.equipoId ?: -1) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalle de Solicitud") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { innerPadding ->
        solicitud?.let { sol ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Solicitud #${sol.id}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        SolicitudStatusBadge(estado = sol.estado)
                        
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        
                        DetailRow(label = "Equipo:", value = equipo?.nombre ?: "Desconocido")
                        DetailRow(label = "Ambiente:", value = sol.ambienteDestino)
                        DetailRow(label = "Duración:", value = "${sol.duracionHoras} horas")
                        DetailRow(label = "Propósito:", value = sol.proposito)
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // RN-07: Botón habilitado solo si el estado es SOLICITADA
                Button(
                    onClick = { 
                        viewModel.cancelarSolicitud(sol.id)
                        onBack()
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    enabled = sol.estado == EstadoSolicitud.SOLICITADA,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("CANCELAR SOLICITUD")
                }
                
                if (sol.estado != EstadoSolicitud.SOLICITADA) {
                    Text(
                        text = "Esta solicitud ya no puede ser cancelada debido a su estado actual.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Text(text = value, style = MaterialTheme.typography.bodyLarge)
    }
}
