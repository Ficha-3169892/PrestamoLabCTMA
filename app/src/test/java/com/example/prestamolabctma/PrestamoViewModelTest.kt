package com.example.prestamolabctma

import com.example.prestamolabctma.viewmodel.duracionValida
import com.example.prestamolabctma.viewmodel.propositoValido
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PrestamoViewModelTest {

    @Test
    fun `validar duracion - limites inferiores y superiores`() {
        // Análisis de valores límite: 0, 1, 8, 9
        // Rango válido: 1 a 8 horas (RN-03)
        assertFalse("Duración 0 debería ser inválida (límite inferior - 1)", duracionValida(0))
        assertTrue("Duración 1 debería ser válida (límite inferior)", duracionValida(1))
        assertTrue("Duración 8 debería ser válida (límite superior)", duracionValida(8))
        assertFalse("Duración 9 debería ser inválida (límite superior + 1)", duracionValida(9))
    }

    @Test
    fun `validar proposito - limites inferiores y superiores`() {
        // Rango válido: 10 a 180 caracteres (RN-02)
        val proprosito9 = "A".repeat(9)
        val proprosito10 = "A".repeat(10)
        val proprosito180 = "A".repeat(180)
        val proprosito181 = "A".repeat(181)

        assertFalse("Propósito de 9 caracteres debería ser inválido (límite inferior - 1)", propositoValido(proprosito9))
        assertTrue("Propósito de 10 caracteres debería ser válido (límite inferior)", propositoValido(proprosito10))
        assertTrue("Propósito de 180 caracteres debería ser válido (límite superior)", propositoValido(proprosito180))
        assertFalse("Propósito de 181 caracteres debería ser inválido (límite superior + 1)", propositoValido(proprosito181))
    }
}
