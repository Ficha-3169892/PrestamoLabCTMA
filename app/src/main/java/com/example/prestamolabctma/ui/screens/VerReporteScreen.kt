package com.example.prestamolabctma.ui.screens

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.prestamolabctma.model.ReporteNovedad
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
fun VerReporteScreen(
    reporteId: String,
    viewModel: PrestamoViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var reporte by remember { mutableStateOf<ReporteNovedad?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedFotoUrl by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(reporteId) {
        isLoading = true
        val usuario = viewModel.uiState.value.usuarioLogueado
        val reportes = viewModel.obtenerReportesAprendiz(usuario?.id ?: "")
        reporte = reportes.find { it.id == reporteId }
        if (reporte == null) {
            val repInst = viewModel.obtenerReportesInstructor(usuario?.id ?: "")
            reporte = repInst.find { it.id == reporteId }
        }
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalle de Reporte Activo") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (reporte == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No se encontró el reporte.")
                }
            } else {
                val rep = reporte!!
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Equipo: ${rep.equipoNombre} (${rep.equipoPlaca})", style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Descripción de la falla:", style = MaterialTheme.typography.labelMedium)
                            Text(rep.descripcion, style = MaterialTheme.typography.bodyLarge)
                            
                            if (rep.devolucionSolicitada) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Surface(
                                    color = MaterialTheme.colorScheme.errorContainer,
                                    shape = MaterialTheme.shapes.small
                                ) {
                                    Text(
                                        text = "⚠️ El instructor solicitó que devuelvas este equipo.",
                                        modifier = Modifier.padding(8.dp),
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }

                    if (rep.fotos.isNotEmpty()) {
                        Text("Evidencia Fotográfica:", style = MaterialTheme.typography.titleSmall)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(rep.fotos) { foto ->
                                if (foto.signedUrl != null) {
                                    AsyncImage(
                                        model = foto.signedUrl,
                                        contentDescription = "Evidencia",
                                        modifier = Modifier
                                            .size(100.dp)
                                            .clickable { selectedFotoUrl = foto.signedUrl },
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                        }
                    } else {
                        Text("Sin fotos adjuntas.", style = MaterialTheme.typography.bodySmall)
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
