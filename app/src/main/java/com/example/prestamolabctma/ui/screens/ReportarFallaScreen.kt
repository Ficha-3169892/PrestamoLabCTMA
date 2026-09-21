package com.example.prestamolabctma.ui.screens

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.prestamolabctma.ui.viewmodel.PrestamoViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportarFallaScreen(
    solicitudId: Int,
    viewModel: PrestamoViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val evidenciaEstado = uiState.evidenciaEstado
    
    // URI temporal para la captura de cámara
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }

    // Launcher para Galería (Photo Picker - Mínimo Privilegio)
    val pickMedia = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            procesarUri(context, uri, viewModel)
        }
    }

    // Launcher para Cámara (TakePicture - FileProvider)
    val takePicture = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            tempCameraUri?.let { uri ->
                procesarUri(context, uri, viewModel)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reportar Falla Grave") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Atrás")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Adjunte evidencia fotográfica de la falla para inhabilitar el equipo.",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Previsualización de Imagen
            if (evidenciaEstado.uriPreview != null) {
                Box(modifier = Modifier.fillMaxWidth().height(250.dp)) {
                    AsyncImage(
                        model = evidenciaEstado.uriPreview,
                        contentDescription = "Evidencia",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    IconButton(
                        onClick = { viewModel.eliminarEvidencia() },
                        modifier = Modifier.align(Alignment.TopEnd),
                        colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                    ) {
                        Icon(Icons.Default.Delete, "Eliminar", tint = MaterialTheme.colorScheme.error)
                    }
                }
            } else {
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth().height(150.dp),
                    onClick = { /* Opcional: abrir selector */ }
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Sin evidencia adjunta", color = MaterialTheme.colorScheme.outline)
                    }
                }
            }

            if (evidenciaEstado.mensajeError != null) {
                Text(
                    text = evidenciaEstado.mensajeError,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Botones de Acción
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Image, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Galería")
                }

                Button(
                    onClick = {
                        val uri = crearUriTemporal(context)
                        tempCameraUri = uri
                        takePicture.launch(uri)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.CameraAlt, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Cámara")
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    viewModel.confirmarYSubirEvidencia(solicitudId)
                    onNavigateBack()
                },
                enabled = evidenciaEstado.uriPreview != null && !evidenciaEstado.procesando,
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                if (evidenciaEstado.procesando) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Confirmar y Enviar Reporte")
                }
            }
        }
    }
}

/**
 * Crea una URI segura usando FileProvider para guardar la captura de cámara.
 */
private fun crearUriTemporal(context: Context): Uri {
    val tempFile = File.createTempFile("CAPTURA_", ".jpg", context.cacheDir).apply {
        deleteOnExit()
    }
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        tempFile
    )
}

/**
 * Extrae metadatos de la URI y envía el stream al ViewModel para persistencia interna.
 */
private fun procesarUri(context: Context, uri: Uri, viewModel: PrestamoViewModel) {
    val contentResolver = context.contentResolver
    val inputStream = contentResolver.openInputStream(uri) ?: return
    
    var fileName = "evidencia.jpg"
    var size = 0L
    val mimeType = contentResolver.getType(uri) ?: "image/jpeg"

    contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
        if (cursor.moveToFirst()) {
            fileName = cursor.getString(nameIndex)
            size = cursor.getLong(sizeIndex)
        }
    }

    viewModel.adjuntarEvidencia(inputStream, fileName, mimeType, size)
}
