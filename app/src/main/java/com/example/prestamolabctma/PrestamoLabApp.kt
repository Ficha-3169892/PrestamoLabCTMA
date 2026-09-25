package com.example.prestamolabctma

import android.app.Application
import android.util.Log
import com.example.prestamolabctma.data.PrestamoRepository
import com.example.prestamolabctma.data.auth.AuthRepository
import com.example.prestamolabctma.data.auth.SessionRepository
import com.example.prestamolabctma.data.remote.PrestamoRemoteDataSource
import com.example.prestamolabctma.data.remote.SupabaseEquipoDataSource
import com.example.prestamolabctma.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class PrestamoLabApp : Application() {
    override fun onCreate() {
        super.onCreate()
        val koinApp = startKoin {
            androidLogger()
            androidContext(this@PrestamoLabApp)
            modules(appModule)
        }

        // Verificación explícita de resolución de dependencias en arranque
        try {
            val koin = koinApp.koin
            koin.get<PrestamoRepository>()
            koin.get<AuthRepository>()
            koin.get<SessionRepository>()
            koin.get<PrestamoRemoteDataSource>()
            koin.get<SupabaseEquipoDataSource>()
            Log.d("KoinVerify", "¡Todas las dependencias principales de Koin se resolvieron exitosamente al iniciar!")
        } catch (e: Exception) {
            Log.e("KoinVerify", "Error crítico al resolver dependencias de Koin en arranque: ${e.message}", e)
        }
    }
}
