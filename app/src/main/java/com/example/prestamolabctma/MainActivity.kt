package com.example.prestamolabctma

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.room.Room
import com.example.prestamolabctma.data.OfflineFirstPrestamoRepository
import com.example.prestamolabctma.data.datastore.UserPreferencesRepository
import com.example.prestamolabctma.data.local.AppDatabase
import com.example.prestamolabctma.data.remote.NetworkModule
import com.example.prestamolabctma.data.remote.RetrofitPrestamoDataSource
import com.example.prestamolabctma.data.remote.SimpleTokenProvider
import com.example.prestamolabctma.ui.navigation.PrestamoApp
import com.example.prestamolabctma.ui.theme.PrestamoLabCTMATheme
import com.example.prestamolabctma.ui.viewmodel.PrestamoViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Inicialización de la base de datos Room
        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "prestamo_lab_database"
        ).fallbackToDestructiveMigration().build()

        // Inicialización de red y cliente Retrofit
        val tokenProvider = SimpleTokenProvider()
        val okHttpClient = NetworkModule.provideOkHttpClient(tokenProvider)
        val api = NetworkModule.providePrestamoApi(okHttpClient)
        val remoteDataSource = RetrofitPrestamoDataSource(api)

        // Inicialización de DataStore
        val preferencesRepository = UserPreferencesRepository(applicationContext)

        // Inicialización del repositorio Offline-First
        val repository = OfflineFirstPrestamoRepository(
            remoteDataSource = remoteDataSource,
            localDataSource = db.prestamoDao(),
            equipoDao = db.equipoDao(),
            internalFilesDir = filesDir
        )

        val viewModel = PrestamoViewModel(repository, preferencesRepository)

        enableEdgeToEdge()
        setContent {
            PrestamoLabCTMATheme {
                // PrestamoApp integra el Scaffold, BottomBar, SnackbarHost y NavHost
                PrestamoApp(viewModel = viewModel)
            }
        }
    }
}
