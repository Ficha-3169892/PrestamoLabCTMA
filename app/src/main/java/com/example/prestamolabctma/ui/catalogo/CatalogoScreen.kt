package com.example.prestamolabctma.ui.catalogo

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.prestamolabctma.model.Equipo
import com.example.prestamolabctma.model.EstadoEquipo
import com.example.prestamolabctma.ui.viewmodel.PrestamoViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogoScreen(
    viewModel: PrestamoViewModel,
    onNavigateToDetail: (Int) -> Unit,
    onNavigateToMisSolicitudes: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Catálogo de Equipos") },
                actions = {
                    IconButton(onClick = onNavigateToMisSolicitudes) {
                        Icon(Icons.Default.List, contentDescription = "Mis Solicitudes")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (uiState.equipos.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Text("No hay equipos disponibles.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.equipos) { equipo ->
                    EquipoCard(equipo = equipo, onClick = { onNavigateToDetail(equipo.id) })
                }
            }
        }
    }
}

@Composable
fun EquipoCard(equipo: Equipo, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = equipo.nombre, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(text = equipo.categoria.name, style = MaterialTheme.typography.bodyMedium)
            }
            StatusBadge(estado = equipo.estado)
        }
    }
}

@Composable
fun StatusBadge(estado: EstadoEquipo) {
    val (text, icon, color) = when (estado) {
        EstadoEquipo.DISPONIBLE -> Triple("Disponible", Icons.Default.CheckCircle, MaterialTheme.colorScheme.primary)
        EstadoEquipo.RESERVADO -> Triple("Reservado", Icons.Default.Lock, MaterialTheme.colorScheme.secondary)
        EstadoEquipo.PRESTADO -> Triple("Prestado", Icons.Default.Info, MaterialTheme.colorScheme.error)
        EstadoEquipo.MANTENIMIENTO -> Triple("Mantenimiento", Icons.Default.Info, MaterialTheme.colorScheme.tertiary)
    }
    Surface(shape = MaterialTheme.shapes.small, color = color.copy(alpha = 0.1f), border = androidx.compose.foundation.BorderStroke(1.dp, color)) {
        Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = color)
            Spacer(Modifier.width(4.dp))
            Text(text = text, style = MaterialTheme.typography.labelMedium, color = color)
        }
    }
}
