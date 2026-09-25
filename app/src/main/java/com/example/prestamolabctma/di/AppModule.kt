package com.example.prestamolabctma.di

import androidx.room.Room
import com.example.prestamolabctma.BuildConfig
import com.example.prestamolabctma.data.OfflineFirstPrestamoRepository
import com.example.prestamolabctma.data.PrestamoRepository
import com.example.prestamolabctma.data.ReporteNovedadRepository
import com.example.prestamolabctma.data.ReporteNovedadRepositoryImpl
import com.example.prestamolabctma.data.auth.AuthRepository
import com.example.prestamolabctma.data.auth.AuthRepositoryImpl
import com.example.prestamolabctma.data.auth.SessionRepository
import com.example.prestamolabctma.data.auth.SessionRepositoryImpl
import com.example.prestamolabctma.data.datastore.UserPreferencesRepository
import com.example.prestamolabctma.data.local.AppDatabase
import com.example.prestamolabctma.data.remote.NetworkModule
import com.example.prestamolabctma.data.remote.PrestamoRemoteDataSource
import com.example.prestamolabctma.data.remote.RetrofitPrestamoDataSource
import com.example.prestamolabctma.data.remote.SimpleTokenProvider
import com.example.prestamolabctma.data.remote.SupabaseEquipoDataSource
import com.example.prestamolabctma.data.remote.SupabasePrestamoDataSource
import com.example.prestamolabctma.data.remote.SupabaseReporteDataSource
import com.example.prestamolabctma.data.remote.TokenProvider
import com.example.prestamolabctma.ui.viewmodel.AuthViewModel
import com.example.prestamolabctma.ui.viewmodel.GestionCatalogoViewModel
import com.example.prestamolabctma.ui.viewmodel.PrestamoViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    // Room Database & DAOs
    single {
        Room.databaseBuilder(
            androidContext(),
            AppDatabase::class.java,
            "prestamo_lab_database"
        ).fallbackToDestructiveMigration().build()
    }
    single { get<AppDatabase>().prestamoDao() }
    single { get<AppDatabase>().equipoDao() }

    // DataStore
    single { UserPreferencesRepository(androidContext()) }

    // Retrofit / Network
    single<TokenProvider> { SimpleTokenProvider() }
    single { NetworkModule.provideOkHttpClient(get()) }
    single { NetworkModule.providePrestamoApi(get()) }
    single<PrestamoRemoteDataSource> { RetrofitPrestamoDataSource(get()) }

    // Supabase
    single<SupabaseClient> {
        createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_ANON_KEY
        ) {
            install(Auth)
            install(Postgrest)
            install(Storage)
        }
    }

    single<AuthRepository> { AuthRepositoryImpl(get()) }
    single<SessionRepository> { SessionRepositoryImpl(get(), get()) }
    single { SupabaseEquipoDataSource(get()) }
    single { SupabasePrestamoDataSource(get()) }
    single { SupabaseReporteDataSource(get()) }

    // Repository
    single<PrestamoRepository> {
        OfflineFirstPrestamoRepository(
            remoteDataSource = get(),
            localDataSource = get(),
            equipoDao = get(),
            internalFilesDir = androidContext().filesDir,
            supabaseEquipoDataSource = get(),
            supabasePrestamoDataSource = get(),
            supabaseReporteDataSource = get(),
            supabaseClient = get()
        )
    }

    single<ReporteNovedadRepository> { ReporteNovedadRepositoryImpl(get()) }

    // ViewModels
    viewModel { AuthViewModel(get(), get()) }
    viewModel { GestionCatalogoViewModel(get(), get()) }
    viewModel { PrestamoViewModel(get(), get(), get(), get()) }
}
