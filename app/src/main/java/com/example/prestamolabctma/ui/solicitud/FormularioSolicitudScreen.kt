package com.example.prestamolabctma.ui.solicitud

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.prestamolabctma.ui.viewmodel.PrestamoValidations
import com.example.prestamolabctma.ui.viewmodel.PrestamoViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormularioSolicitudScreen(
    equipoId: Int,
    viewModel: PrestamoViewModel,
    onBack: () -> Unit,
    onSuccess: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val equipo = uiState.equipos.find { it.id == equipoId }

    var ambiente by remember { mutableStateOf("") }
    var proposito by remember { mutableStateOf("") }
    var duracionStr by remember { mutableStateOf("") }

    // Validaciones en tiempo real para feedback visual
    val ambienteError = if (ambiente.isNotEmpty() && !PrestamoValidations.ambienteValido(ambiente)) "El ambiente no puede estar vacío" else null
    val propositoError = if (proposito.isNotEmpty() && !PrestamoValidations.propositoValido(proposito)) "Debe tener entre 10 y 180 caracteres" else null
    val duracionInt = duracionStr.toIntOrNull() ?: 0
    val duracionError = if (duracionStr.isNotEmpty() && !PrestamoValidations.duracionValida(duracionInt)) "Debe ser entre 1 y 8 horas" else null

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Formulario de Préstamo") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Equipo: ${equipo?.nombre ?: "..."}",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )

            OutlinedTextField(
                value = ambiente,
                onValueChange = { ambiente = it },
                label = { Text("Ambiente de Destino (Ej: Aula 302)") },
                modifier = Modifier.fillMaxWidth(),
                isError = ambienteError != null,
                supportingText = { if (ambienteError != null) Text(ambienteError) }
            )

            OutlinedTextField(
                value = proposito,
                onValueChange = { proposito = it },
                label = { Text("Propósito (Mín. 10 caracteres)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                isError = propositoError != null,
                supportingText = { if (propositoError != null) Text(propositoError) }
            )

            OutlinedTextField(
                value = duracionStr,
                onValueChange = { if (it.all { char -> char.isDigit() }) duracionStr = it },
                label = { Text("Duración estimada (Horas)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = duracionError != null,
                supportingText = { if (duracionError != null) Text(duracionError) }
            )

            if (uiState.mensajeError != null) {
                Text(
                    text = uiState.mensajeError!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    viewModel.guardarSolicitud(
                        equipoId = equipoId,
                        ambiente = ambiente,
                        proposito = proposito,
                        duracion = duracionInt,
                        onSuccess = onSuccess
                    )
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = !uiState.guardando && 
                          ambiente.isNotEmpty() && proposito.isNotEmpty() && duracionStr.isNotEmpty() &&
                          ambienteError == null && propositoError == null && duracionError == null
            ) {
                if (uiState.guardando) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("ENVIAR SOLICITUD")
                }
            }
        }
    }
}
