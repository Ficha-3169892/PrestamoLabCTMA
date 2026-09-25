package com.example.prestamolabctma.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.prestamolabctma.model.SolicitudPrestamo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MisPrestamosScreen(
    solicitudes: List<SolicitudPrestamo>,
    aprendizId: String,
    onBackClick: () -> Unit,
    onLogoutClick: () -> Unit = {}
) {
    Log.d("EquipoDebug", "MisPrestamosScreen: aprendizId='$aprendizId', totalSolicitudes=${solicitudes.size}")
    val misPrestamos = solicitudes.filter { it.aprendizId == aprendizId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mis Préstamos (Historial)") },
                navigationIcon = {
                    TextButton(onClick = onBackClick) {
                        Text("← Volver")
                    }
                },
                actions = {
                    IconButton(onClick = onLogoutClick) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Cerrar Sesión")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (misPrestamos.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Text("No tienes solicitudes de préstamo registradas")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(misPrestamos) { solicitud ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Equipo: ${solicitud.equipoNombre} (${solicitud.equipoPlaca})", style = MaterialTheme.typography.titleMedium)
                            Text("Categoría: ${solicitud.equipoCategoria}")
                            Text("Estado: ${solicitud.estado.name}")
                            Text("Fecha solicitud: ${solicitud.fechaSolicitud}")
                            if (solicitud.motivoRechazo != null) {
                                Text("Motivo: ${solicitud.motivoRechazo}", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}
