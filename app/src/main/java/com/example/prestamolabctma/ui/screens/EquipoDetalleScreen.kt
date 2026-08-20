package com.example.prestamolabctma.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
            title = { Text("Detalle de Equipo") },
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
                        Text(text = equipo.nombre, style = MaterialTheme.typography.headlineMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "Categoría: ${equipo.categoria}", style = MaterialTheme.typography.bodyLarge)
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        val isDisponible = equipo.estado == EstadoEquipo.DISPONIBLE
                        
                        Text(
                            text = "Estado: ${equipo.estado}",
                            color = if (isDisponible) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.titleMedium
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = { onSolicitarClick(equipo.id) },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = isDisponible
                        ) {
                            Text(if (isDisponible) "Solicitar Préstamo" else "No disponible")
                        }
                    }
                }
            }
        }
    }
}
