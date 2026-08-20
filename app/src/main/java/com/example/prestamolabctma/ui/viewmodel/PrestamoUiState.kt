package com.example.prestamolabctma.ui.viewmodel

import com.example.prestamolabctma.domain.model.Equipo
import com.example.prestamolabctma.domain.model.SolicitudPrestamo

data class PrestamoUiState(
    val equipos: List<Equipo> = emptyList(),
    val solicitudes: List<SolicitudPrestamo> = emptyList(),
    val mensajeError: String? = null,
    val guardando: Boolean = false
)
