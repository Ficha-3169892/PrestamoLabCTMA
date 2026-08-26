package com.example.prestamolabctma.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.prestamolabctma.model.Equipo
import com.example.prestamolabctma.ui.viewmodel.duracionValida
import com.example.prestamolabctma.ui.viewmodel.propositoValido
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SolicitudFormScreen(
    equipo: Equipo,
    guardando: Boolean,
    onGuardar: (Int, String, String, Int, LocalDateTime) -> Unit,
    onCancelar: () -> Unit
) {
    var ambiente by remember { mutableStateOf("") }
    var proposito by remember { mutableStateOf("") }
    var duracionStr by remember { mutableStateOf("1") }
    
    // Simplificación para HU 04: En una app real usaríamos DatePickerDialog
    var fechaTexto by remember { mutableStateOf(LocalDateTime.now().plusHours(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))) }

    val duracion = duracionStr.toIntOrNull() ?: 0
    val isPropositoOk = propositoValido(proposito)
    val isDuracionOk = duracionValida(duracion)
    
    val canSubmit = ambiente.isNotBlank() && isPropositoOk && isDuracionOk && !guardando

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Solicitar Préstamo") },
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
            Text("Equipo: ${equipo.nombre} (${equipo.placa})", style = MaterialTheme.typography.titleLarge)
            
            OutlinedTextField(
                value = ambiente,
                onValueChange = { ambiente = it },
                label = { Text("Ambiente / Taller de Destino *") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = proposito,
                onValueChange = { proposito = it },
                label = { Text("Motivo / Propósito de uso *") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                supportingText = { Text("Min 10 caracteres") }
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = duracionStr,
                    onValueChange = { if (it.all { c -> c.isDigit() }) duracionStr = it },
                    label = { Text("Horas (1-8)") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = fechaTexto,
                    onValueChange = { fechaTexto = it },
                    label = { Text("Fecha Inicio (YYYY-MM-DD HH:mm)") },
                    modifier = Modifier.weight(2f),
                    supportingText = { Text("Reserva previa máx 24h") }
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = { 
                    // En el prototipo asumimos que la fecha es válida o usamos NOW
                    onGuardar(equipo.id, ambiente, proposito, duracion, LocalDateTime.now()) 
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = canSubmit
            ) {
                if (guardando) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Confirmar Reserva")
                }
            }
        }
    }
}
