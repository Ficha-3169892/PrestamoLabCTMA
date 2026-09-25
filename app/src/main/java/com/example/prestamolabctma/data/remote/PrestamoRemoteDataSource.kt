package com.example.prestamolabctma.data.remote

import okhttp3.MultipartBody

interface PrestamoRemoteDataSource {
    suspend fun fetchPrestamos(): Result<List<PrestamoDto>>
    suspend fun fetchPrestamo(id: Int): Result<PrestamoDto>
    suspend fun createPrestamo(dto: PrestamoDto): Result<PrestamoDto>
    suspend fun updatePrestamo(id: Int, dto: PrestamoDto): Result<PrestamoDto>
    suspend fun uploadEvidencia(solicitudId: Int, filePart: MultipartBody.Part): Result<Unit>
}

class RetrofitPrestamoDataSource(
    private val api: PrestamoApi
) : PrestamoRemoteDataSource {

    override suspend fun fetchPrestamos(): Result<List<PrestamoDto>> {
        return safeApiCall { api.getPrestamos() }
    }

    override suspend fun fetchPrestamo(id: Int): Result<PrestamoDto> {
        return safeApiCall { api.getPrestamo(id) }
    }

    override suspend fun createPrestamo(dto: PrestamoDto): Result<PrestamoDto> {
        return safeApiCall { api.createPrestamo(dto) }
    }

    override suspend fun updatePrestamo(id: Int, dto: PrestamoDto): Result<PrestamoDto> {
        return safeApiCall { api.updatePrestamo(id, dto) }
    }

    override suspend fun uploadEvidencia(solicitudId: Int, filePart: MultipartBody.Part): Result<Unit> {
        return safeApiCall { api.uploadEvidencia(solicitudId, filePart) }
    }

    private suspend fun <T> safeApiCall(call: suspend () -> retrofit2.Response<T>): Result<T> {
        return try {
            val response = call()
            val body = response.body()
            if (response.isSuccessful && body != null) {
                Result.success(body)
            } else if (response.isSuccessful && response.code() == 204) {
                @Suppress("UNCHECKED_CAST")
                Result.success(Unit as T)
            } else {
                Result.failure(ApiException(response.code(), response.message()))
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: java.io.IOException) {
            Result.failure(NetworkException(e.message ?: "Network error"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
