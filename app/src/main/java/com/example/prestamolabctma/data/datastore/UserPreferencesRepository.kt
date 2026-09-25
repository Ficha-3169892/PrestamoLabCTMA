package com.example.prestamolabctma.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

data class UserPreferences(
    val busquedaFiltro: String = "",
    val categoriaFiltro: String? = null,
    val ultimoRolUsado: String? = null,
    val modoOscuro: Boolean = false
)

class UserPreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val BUSQUEDA_FILTRO = stringPreferencesKey("busqueda_filtro")
        val CATEGORIA_FILTRO = stringPreferencesKey("categoria_filtro")
        val ULTIMO_ROL_USADO = stringPreferencesKey("ultimo_rol_usado")
        val MODO_OSCURO = booleanPreferencesKey("modo_oscuro")
    }

    val userPreferencesFlow: Flow<UserPreferences> = context.dataStore.data.map { preferences ->
        UserPreferences(
            busquedaFiltro = preferences[PreferencesKeys.BUSQUEDA_FILTRO] ?: "",
            categoriaFiltro = preferences[PreferencesKeys.CATEGORIA_FILTRO],
            ultimoRolUsado = preferences[PreferencesKeys.ULTIMO_ROL_USADO],
            modoOscuro = preferences[PreferencesKeys.MODO_OSCURO] ?: false
        )
    }

    suspend fun guardarBusquedaFiltro(busqueda: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.BUSQUEDA_FILTRO] = busqueda
        }
    }

    suspend fun guardarCategoriaFiltro(categoria: String?) {
        context.dataStore.edit { preferences ->
            if (categoria != null) {
                preferences[PreferencesKeys.CATEGORIA_FILTRO] = categoria
            } else {
                preferences.remove(PreferencesKeys.CATEGORIA_FILTRO)
            }
        }
    }

    suspend fun guardarUltimoRolUsado(rol: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ULTIMO_ROL_USADO] = rol
        }
    }

    suspend fun guardarModoOscuro(modoOscuro: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.MODO_OSCURO] = modoOscuro
        }
    }
}
