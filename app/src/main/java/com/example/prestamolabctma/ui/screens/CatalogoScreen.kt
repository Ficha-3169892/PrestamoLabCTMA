package com.example.prestamolabctma.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.prestamolabctma.model.CategoriaEquipo
import com.example.prestamolabctma.model.Equipo
import com.example.prestamolabctma.model.EstadoEquipo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogoScreen(
    equipos: List<Equipo>,
    busqueda: String,
    onBusquedaChange: (String) -> Unit,
    categoriaSeleccionada: CategoriaEquipo?,
    onCategoriaChange: (CategoriaEquipo?) -> Unit,
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

        // HU 02: SearchBar y Filtros
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            OutlinedTextField(
                value = busqueda,
                onValueChange = onBusquedaChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Buscar por nombre o placa...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                singleLine = true,
                shape = MaterialTheme.shapes.medium
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                item {
                    FilterChip(
                        selected = categoriaSeleccionada == null,
                        onClick = { onCategoriaChange(null) },
                        label = { Text("Todos") }
                    )
                }
                items(CategoriaEquipo.values()) { categoria ->
                    FilterChip(
                        selected = categoriaSeleccionada == categoria,
                        onClick = { onCategoriaChange(categoria) },
                        label = { Text(categoria.name) }
                    )
                }
            }
        }

        if (equipos.isEmpty()) {
            // HU 02: Empty State
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No se encontraron equipos", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
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
}

@Composable
fun EquipoItem(equipo: Equipo, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(equipo.nombre, style = MaterialTheme.typography.titleMedium)
                Text("Placa: ${equipo.placa}", style = MaterialTheme.typography.bodySmall)
                Text(equipo.categoria.name, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
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
        EstadoEquipo.MANTENIMIENTO -> Color(0xFF9E9E9E)
        EstadoEquipo.REPARACION -> Color(0xFF795548)
    }
    Surface(
        color = color.copy(alpha = 0.1f),
        contentColor = color,
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = estado.name,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall
        )
    }
}
