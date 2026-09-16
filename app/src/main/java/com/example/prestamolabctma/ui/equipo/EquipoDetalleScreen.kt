package com.example.prestamolabctma.ui.equipo

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.prestamolabctma.model.EstadoEquipo
import com.example.prestamolabctma.ui.catalogo.StatusBadge
import com.example.prestamolabctma.ui.viewmodel.PrestamoViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EquipoDetalleScreen(
    equipoId: Int,
    viewModel: PrestamoViewModel,
    onBack: () -> Unit,
    onNavigateToSolicitar: (Int) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val equipo = uiState.equipos.find { it.id == equipoId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalle del Equipo") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { innerPadding ->
        equipo?.let {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = it.nombre,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Categoría:",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = it.categoria.name.replace("_", " "),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Estado actual:",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            StatusBadge(estado = it.estado)
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "Descripción del equipo",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Este equipo se encuentra bajo la normativa de préstamos del laboratorio CTMA. Asegúrese de cumplir con los requisitos de seguridad antes de solicitarlo.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = { onNavigateToSolicitar(it.id) },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    enabled = it.estado == EstadoEquipo.DISPONIBLE
                ) {
                    Text("SOLICITAR PRÉSTAMO")
                }
            }
        }
    }
}
