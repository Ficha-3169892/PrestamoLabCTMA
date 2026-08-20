package com.example.prestamolabctma.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.prestamolabctma.model.Equipo
import com.example.prestamolabctma.model.EstadoEquipo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogoScreen(
    equipos: List<Equipo>,
    onEquipoClick: (Int) -> Unit,
    onVerMisPrestamos: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Catálogo CTMA") },
            actions = {
                IconButton(onClick = onVerMisPrestamos) {
                    Icon(Icons.Default.List, contentDescription = "Mis Préstamos")
                }
            }
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(equipos) { equipo ->
                EquipoItem(equipo, onClick = { onEquipoClick(equipo.id) })
            }
        }
    }
}

@Composable
fun EquipoItem(equipo: Equipo, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(equipo.nombre, style = MaterialTheme.typography.titleMedium)
                Text(equipo.categoria.name, style = MaterialTheme.typography.bodySmall)
            }
            StatusBadge(equipo.estado)
        }
    }
}

@Composable
fun StatusBadge(estado: EstadoEquipo) {
    val color = when (estado) {
        EstadoEquipo.DISPONIBLE -> Color(0xFF4CAF50)
        EstadoEquipo.RESERVADO -> Color(0xFFFF9800)
        EstadoEquipo.PRESTADO -> Color(0xFFF44336)
    }
    Surface(
        color = color.copy(alpha = 0.1f),
        contentColor = color,
        shape = MaterialTheme.shapes.small
    ) {
        Text(estado.name, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall)
    }
}
