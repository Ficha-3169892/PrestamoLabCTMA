package com.example.prestamolabctma.ui.equipo

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.prestamolabctma.model.Equipo
import com.example.prestamolabctma.model.EstadoEquipo

@Composable
fun EquipoDetalleScreen(
    equipo: Equipo?,
    onSolicitarClick: (Int) -> Unit
) {
    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(title = { Text("Detalle del Equipo") })
        }
    ) { padding ->
        if (equipo == null) {
            // RN-08: Si el equipo es nulo, muestra un mensaje de "Equipo no encontrado"
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "Equipo no encontrado", style = MaterialTheme.typography.headlineSmall)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(text = equipo.nombre, style = MaterialTheme.typography.headlineMedium)
                Text(text = "Categoría: ${equipo.categoria}", style = MaterialTheme.typography.bodyLarge)
                
                val esDisponible = equipo.estado == EstadoEquipo.DISPONIBLE
                
                Text(
                    text = "Estado: ${equipo.estado}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (esDisponible) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )

                if (!esDisponible) {
                    // RN-01: Mostrar mensaje claro si no está disponible
                    Text(
                        text = "Este equipo no se puede solicitar actualmente porque su estado es ${equipo.estado}.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = { onSolicitarClick(equipo.id) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = esDisponible // RN-01: Botón deshabilitado si no está DISPONIBLE
                ) {
                    Text(text = "Solicitar Préstamo")
                }
            }
        }
    }
}
