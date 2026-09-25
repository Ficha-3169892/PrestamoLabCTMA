package com.example.prestamolabctma.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.prestamolabctma.model.CategoriaEquipo
import com.example.prestamolabctma.model.Equipo
import com.example.prestamolabctma.ui.viewmodel.GestionCatalogoViewModel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GestionCatalogoScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLogoutClick: () -> Unit = {},
    viewModel: GestionCatalogoViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var equipoAEliminar by remember { mutableStateOf<Equipo?>(null) }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Gestión de Mis Equipos") },
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
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.abrirFormularioCrear() }) {
                Icon(Icons.Default.Add, contentDescription = "Crear Equipo")
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (uiState.isLoading && uiState.equiposInstructor.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (uiState.equiposInstructor.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No tienes equipos registrados. Usa el botón '+' para agregar uno.")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.equiposInstructor) { equipo ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(text = equipo.nombre, style = MaterialTheme.typography.titleMedium)
                                Text(text = "Placa: ${equipo.placa}", style = MaterialTheme.typography.bodyMedium)
                                Text(text = "Categoría: ${equipo.categoria}", style = MaterialTheme.typography.bodySmall)
                                Text(text = "Estado: ${equipo.estado}", style = MaterialTheme.typography.bodySmall)
                                if (equipo.descripcion.isNotBlank()) {
                                    Text(text = "Descripción: ${equipo.descripcion}", style = MaterialTheme.typography.bodySmall)
                                }
                                
                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(onClick = { viewModel.abrirFormularioEditar(equipo) }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Editar", tint = MaterialTheme.colorScheme.primary)
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    IconButton(onClick = { equipoAEliminar = equipo }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Form Dialog (Create / Edit)
            if (uiState.showFormDialog) {
                AlertDialog(
                    onDismissRequest = { viewModel.cerrarFormulario() },
                    title = { Text(if (uiState.editingEquipo == null) "Nuevo Equipo" else "Editar Equipo") },
                    text = {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = uiState.placa,
                                onValueChange = { viewModel.updatePlaca(it) },
                                label = { Text("Placa (Única)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = uiState.nombre,
                                onValueChange = { viewModel.updateNombre(it) },
                                label = { Text("Nombre del Equipo") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            
                            Text("Categoría:", style = MaterialTheme.typography.bodySmall)
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(CategoriaEquipo.entries) { cat ->
                                    FilterChip(
                                        selected = uiState.categoria == cat,
                                        onClick = { viewModel.updateCategoria(cat) },
                                        label = { Text(cat.name, style = MaterialTheme.typography.labelSmall) }
                                    )
                                }
                            }

                            OutlinedTextField(
                                value = uiState.descripcion,
                                onValueChange = { viewModel.updateDescripcion(it) },
                                label = { Text("Descripción / Observaciones") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = { viewModel.guardarEquipo() },
                            enabled = viewModel.isFormValid && !uiState.isLoading
                        ) {
                            Text("Guardar")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { viewModel.cerrarFormulario() }) {
                            Text("Cancelar")
                        }
                    }
                )
            }

            // Confirm Delete Dialog
            equipoAEliminar?.let { equipo ->
                AlertDialog(
                    onDismissRequest = { equipoAEliminar = null },
                    title = { Text("Confirmar Eliminación") },
                    text = { Text("¿Estás seguro de eliminar permanentemente el equipo '${equipo.nombre}' (Placa: ${equipo.placa})? Esta acción es irreversible.") },
                    confirmButton = {
                        Button(
                            onClick = {
                                viewModel.eliminarEquipo(equipo.id)
                                equipoAEliminar = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Eliminar")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { equipoAEliminar = null }) {
                            Text("Cancelar")
                        }
                    }
                )
            }
        }
    }
}
