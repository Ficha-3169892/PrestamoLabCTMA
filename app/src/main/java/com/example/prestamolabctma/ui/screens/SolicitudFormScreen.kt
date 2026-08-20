package com.example.prestamolabctma.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.prestamolabctma.model.Equipo
import com.example.prestamolabctma.ui.viewmodel.duracionValida
import com.example.prestamolabctma.ui.viewmodel.propositoValido

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SolicitudFormScreen(
    equipo: Equipo,
    guardando: Boolean,
    onGuardar: (Int, String, String, Int) -> Unit,
    onCancelar: () -> Unit
) {
    var ambiente by remember { mutableStateOf("") }
    var proposito by remember { mutableStateOf("") }
    var duracionStr by remember { mutableStateOf("1") }

    val duracion = duracionStr.toIntOrNull() ?: 0
    val isPropositoOk = propositoValido(proposito)
    val isDuracionOk = duracionValida(duracion)
    
    val canSubmit = ambiente.isNotBlank() && isPropositoOk && isDuracionOk && !guardando

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Solicitar: ${equipo.nombre}") },
            navigationIcon = {
                IconButton(onClick = onCancelar) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar")
                }
            }
        )
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = ambiente,
                onValueChange = { ambiente = it },
                label = { Text("Ambiente / Destino *") },
                modifier = Modifier.fillMaxWidth(),
                isError = ambiente.isEmpty()
            )

            OutlinedTextField(
                value = proposito,
                onValueChange = { proposito = it },
                label = { Text("Propósito *") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                isError = proposito.isNotEmpty() && !isPropositoOk,
                supportingText = {
                    Text("Min 10, max 180 caracteres. (${proposito.length})")
                }
            )

            OutlinedTextField(
                value = duracionStr,
                onValueChange = { if (it.all { c -> c.isDigit() }) duracionStr = it },
                label = { Text("Duración (1-8 horas) *") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = duracionStr.isNotEmpty() && !isDuracionOk
            )

            Spacer(modifier = Modifier.weight(1f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onCancelar,
                    modifier = Modifier.weight(1f),
                    enabled = !guardando
                ) {
                    Text("Cancelar")
                }
                Button(
                    onClick = { onGuardar(equipo.id, ambiente, proposito, duracion) },
                    modifier = Modifier.weight(1f),
                    enabled = canSubmit
                ) {
                    if (guardando) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Guardar")
                    }
                }
            }
        }
    }
}
