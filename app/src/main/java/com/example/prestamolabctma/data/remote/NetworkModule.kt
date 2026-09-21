package com.example.prestamolabctma.data.remote

import com.example.prestamolabctma.BuildConfig
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

object NetworkModule {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        encodeDefaults = true
    }

    fun provideOkHttpClient(tokenProvider: TokenProvider): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            // Regla 9: No loguear cabeceras de autorización
            level = HttpLoggingInterceptor.Level.BASIC 
        }

        return OkHttpClient.Builder()
            .addInterceptor { chain ->
                val original = chain.request()
                val requestBuilder = original.newBuilder()
                
                // Inyectar Token si existe
                tokenProvider.getToken()?.let { token ->
                    requestBuilder.header("Authorization", "Bearer $token")
                }
                
                // Inyectar API Key de Supabase si está disponible (preparación futura)
                if (BuildConfig.SUPABASE_ANON_KEY.isNotEmpty()) {
                    requestBuilder.header("apikey", BuildConfig.SUPABASE_ANON_KEY)
                }

                chain.proceed(requestBuilder.build())
            }
            .addInterceptor(loggingInterceptor)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    fun providePrestamoApi(okHttpClient: OkHttpClient): PrestamoApi {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(PrestamoApi::class.java)
    }
}
