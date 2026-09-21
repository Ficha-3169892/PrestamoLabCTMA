package com.example.prestamolabctma.data.remote

import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

interface PrestamoApi {
    @GET("v1/prestamos")
    suspend fun getPrestamos(): Response<List<PrestamoDto>>

    @GET("v1/prestamos/{id}")
    suspend fun getPrestamo(@Path("id") id: Int): Response<PrestamoDto>

    @POST("v1/prestamos")
    suspend fun createPrestamo(@Body prestamo: PrestamoDto): Response<PrestamoDto>

    @PUT("v1/prestamos/{id}")
    suspend fun updatePrestamo(@Path("id") id: Int, @Body prestamo: PrestamoDto): Response<PrestamoDto>

    // Semana 9: Subida de evidencia fotográfica
    @Multipart
    @POST("v1/prestamos/{id}/evidencia")
    suspend fun uploadEvidencia(
        @Path("id") solicitudId: Int,
        @Part evidencia: MultipartBody.Part
    ): Response<Unit>
}
