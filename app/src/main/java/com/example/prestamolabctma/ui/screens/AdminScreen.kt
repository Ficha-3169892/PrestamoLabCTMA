package com.example.prestamolabctma.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.prestamolabctma.model.EstadoSolicitud
import com.example.prestamolabctma.model.SolicitudPrestamo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    solicitudes: List<SolicitudPrestamo>,
    onProcesar: (Int, Boolean, String?) -> Unit,
    onDevolver: (Int, String?, Boolean) -> Unit,
    onBackClick: () -> Unit
) {
    var tabSelected by remember { mutableStateOf(0) }
    val tabs = listOf("Pendientes", "Entregados")

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Panel Administrativo") },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Text("←")
                }
            }
        )

        TabRow(selectedTabIndex = tabSelected) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = tabSelected == index,
                    onClick = { tabSelected = index },
                    text = { Text(title) }
                )
            }
        }

        val listaMostrar = if (tabSelected == 0) {
            solicitudes.filter { it.estado == EstadoSolicitud.SOLICITADA }
        } else {
            solicitudes.filter { it.estado == EstadoSolicitud.ENTREGADA }
        }

        if (listaMostrar.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No hay solicitudes en esta categoría")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(listaMostrar) { solicitud ->
                    if (tabSelected == 0) {
                        AprobacionCard(solicitud, onProcesar)
                    } else {
                        DevolucionCard(solicitud, onDevolver)
                    }
                }
            }
        }
    }
}

@Composable
fun AprobacionCard(solicitud: SolicitudPrestamo, onProcesar: (Int, Boolean, String?) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Solicitud #${solicitud.id}", style = MaterialTheme.typography.titleMedium)
            Text("Aprendiz: ${solicitud.usuarioId}")
            Text("Equipo ID: ${solicitud.equipoId}")
            Text("Motivo: ${solicitud.proposito}")
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { onProcesar(solicitud.id, true, null) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                ) {
                    Icon(Icons.Default.Check, null)
                    Spacer(Modifier.width(4.dp))
                    Text("Aprobar")
                }
                OutlinedButton(
                    onClick = { onProcesar(solicitud.id, false, "No disponible") },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
                ) {
                    Icon(Icons.Default.Close, null)
                    Spacer(Modifier.width(4.dp))
                    Text("Rechazar")
                }
            }
        }
    }
}

@Composable
fun DevolucionCard(solicitud: SolicitudPrestamo, onDevolver: (Int, String?, Boolean) -> Unit) {
    var showDialog by remember { mutableStateOf(false) }
    var novedad by remember { mutableStateOf("") }
    var esGrave by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Préstamo en Curso #${solicitud.id}", style = MaterialTheme.typography.titleMedium)
            Text("Equipo ID: ${solicitud.equipoId}")
            Text("Ubicación: ${solicitud.ambienteDestino}")
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = { showDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Refresh, null)
                Spacer(Modifier.width(8.dp))
                Text("Registrar Devolución")
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Inspección de Devolución") },
            text = {
                Column {
                    Text("¿El equipo presenta novedades?")
                    OutlinedTextField(
                        value = novedad,
                        onValueChange = { novedad = it },
                        label = { Text("Descripción de novedad (opcional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = esGrave, onCheckedChange = { esGrave = it })
                        Text("¿Es una falla grave / Daño?")
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    onDevolver(solicitud.id, if(novedad.isBlank()) null else novedad, esGrave)
                    showDialog = false
                }) { Text("Confirmar") }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Cancelar") }
            }
        )
    }
}
