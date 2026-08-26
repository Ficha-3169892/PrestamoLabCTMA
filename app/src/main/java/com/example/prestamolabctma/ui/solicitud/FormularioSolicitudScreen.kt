package com.example.prestamolabctma.ui.solicitud

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.prestamolabctma.model.Equipo
import com.example.prestamolabctma.viewmodel.duracionValida
import com.example.prestamolabctma.viewmodel.propositoValido

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormularioSolicitudScreen(
    equipo: Equipo,
    guardando: Boolean,
    onGuardar: (Int, String, String, Int) -> Unit,
    onCancelar: () -> Unit
) {
    var ambiente by remember { mutableStateOf("") }
    var proposito by remember { mutableStateOf("") }
    var duracionStr by remember { mutableStateOf("") }

    val duracion = duracionStr.toIntOrNull() ?: 0
    
    val ambienteError = ambiente.isEmpty()
    val propositoError = proposito.isNotEmpty() && !propositoValido(proposito)
    val duracionError = duracionStr.isNotEmpty() && !duracionValida(duracion)

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Solicitar ${equipo.nombre}") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = ambiente,
                onValueChange = { ambiente = it },
                label = { Text("Ambiente/Destino") },
                modifier = Modifier.fillMaxWidth(),
                isError = ambienteError,
                supportingText = { if (ambienteError) Text("El ambiente es obligatorio") }
            )

            OutlinedTextField(
                value = proposito,
                onValueChange = { proposito = it },
                label = { Text("Propósito del préstamo") },
                modifier = Modifier.fillMaxWidth(),
                isError = propositoError,
                supportingText = { 
                    if (propositoError) Text("Debe tener entre 10 y 180 caracteres")
                    else Text("${proposito.length}/180")
                }
            )

            OutlinedTextField(
                value = duracionStr,
                onValueChange = { if (it.all { char -> char.isDigit() }) duracionStr = it },
                label = { Text("Duración (horas)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = duracionError,
                supportingText = { if (duracionError) Text("La duración debe ser entre 1 y 8 horas") }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onCancelar,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancelar")
                }
                Button(
                    onClick = { onGuardar(equipo.id, ambiente, proposito, duracion) },
                    modifier = Modifier.weight(1f),
                    enabled = !guardando && !ambienteError && propositoValido(proposito) && duracionValida(duracion)
                ) {
                    if (guardando) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Text("Guardar")
                    }
                }
            }
        }
    }
}
