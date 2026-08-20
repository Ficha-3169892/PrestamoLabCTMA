package com.example.prestamolabctma.ui.viewmodel

object PrestamoValidations {
    fun propositoValido(texto: String): Boolean = texto.trim().length in 10..180
    
    fun duracionValida(horas: Int): Boolean = horas in 1..8
    
    fun ambienteValido(texto: String): Boolean = texto.trim().isNotEmpty()
}
