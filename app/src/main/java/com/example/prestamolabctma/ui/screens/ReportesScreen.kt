package com.example.prestamolabctma.ui.screens

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.prestamolabctma.ui.viewmodel.PrestamoViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportesScreen(
    viewModel: PrestamoViewModel,
    instructorId: String,
    onBackClick: () -> Unit,
    onLogoutClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val reportes = uiState.reportesInstructor
    var selectedFotoUrl by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(instructorId) {
        Log.d("EquipoDebug", "ReportesScreen LaunchedEffect: cargando reportes para instructorId=$instructorId")
        viewModel.cargarReportesInstructor(instructorId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reportes de Novedad") },
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
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            if (uiState.guardando && reportes.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (reportes.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No hay reportes de novedad activos.")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(reportes) { reporte ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Equipo: ${reporte.equipoNombre} (${reporte.equipoPlaca})", style = MaterialTheme.typography.titleMedium)
                                Text("Descripción: ${reporte.descripcion}", style = MaterialTheme.typography.bodyMedium)
                                Text("Aprendiz ID: ${reporte.aprendizId}", style = MaterialTheme.typography.bodySmall)
                                
                                if (reporte.devolucionSolicitada) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Devolución solicitada al aprendiz", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                                }

                                if (reporte.fotos.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        items(reporte.fotos) { foto ->
                                            if (foto.signedUrl != null) {
                                                AsyncImage(
                                                    model = foto.signedUrl,
                                                    contentDescription = "Evidencia",
                                                    modifier = Modifier
                                                        .size(80.dp)
                                                        .clickable { selectedFotoUrl = foto.signedUrl },
                                                    contentScale = ContentScale.Crop
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (!reporte.devolucionSolicitada) {
                                        Button(
                                            onClick = {
                                                Log.d("EquipoDebug", "ReportesScreen: botón Solicitar Devolución click id=${reporte.id}")
                                                viewModel.solicitarDevolucionReporte(reporte.id, instructorId)
                                            },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Solicitar Devolución")
                                        }
                                    } else if (reporte.fechaRecepcion == null) {
                                        Button(
                                            onClick = {
                                                Log.d("EquipoDebug", "ReportesScreen: botón Marcar Recibido click id=${reporte.id}")
                                                viewModel.marcarRecibidoReporte(reporte.id, reporte.prestamoId, reporte.equipoId, instructorId)
                                            },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                                        ) {
                                            Text("Marcar Recibido")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Dialogo Zoom imagen
            selectedFotoUrl?.let { url ->
                AlertDialog(
                    onDismissRequest = { selectedFotoUrl = null },
                    title = { Text("Evidencia Fotográfica") },
                    text = {
                        Box(modifier = Modifier.fillMaxWidth().height(300.dp)) {
                            AsyncImage(
                                model = url,
                                contentDescription = "Zoom",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        }
                    },
                    confirmButton = {
                        Button(onClick = {
                            descargarImagen(context, url)
                            selectedFotoUrl = null
                        }) {
                            Text("Descargar Imagen")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { selectedFotoUrl = null }) {
                            Text("Cerrar")
                        }
                    }
                )
            }
        }
    }
}

private fun descargarImagen(context: Context, imageUrl: String) {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val url = URL(imageUrl)
            val connection = url.openConnection()
            val inputStream = connection.getInputStream()
            val bitmap = BitmapFactory.decodeStream(inputStream)
            
            val filename = "novedad_${System.currentTimeMillis()}.jpg"
            val fos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/PrestamoLab")
                }
                val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                imageUri?.let { resolver.openOutputStream(it) }
            } else {
                val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                val image = File(imagesDir, filename)
                FileOutputStream(image)
            }

            fos?.use {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Imagen descargada en Imágenes/PrestamoLab", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Error al descargar imagen: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
