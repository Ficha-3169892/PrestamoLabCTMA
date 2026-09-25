package com.example.prestamolabctma.data.remote

import retrofit2.Response
import java.io.IOException
import java.util.concurrent.TimeoutException
import kotlinx.coroutines.CancellationException

class RemotePrestamoDataSource(
    private val api: PrestamoApi
) {
    suspend fun fetchPrestamos(): Result<List<PrestamoDto>> {
        return safeApiCall { api.getPrestamos() }
    }

    suspend fun fetchPrestamo(id: Int): Result<PrestamoDto> {
        return safeApiCall { api.getPrestamo(id) }
    }

    suspend fun createPrestamo(dto: PrestamoDto): Result<PrestamoDto> {
        return safeApiCall { api.createPrestamo(dto) }
    }

    suspend fun updatePrestamo(id: Int, dto: PrestamoDto): Result<PrestamoDto> {
        return safeApiCall { api.updatePrestamo(id, dto) }
    }

    private suspend fun <T> safeApiCall(call: suspend () -> Response<T>): Result<T> {
        return try {
            val response = call()
            val body = response.body()
            if (response.isSuccessful && body != null) {
                Result.success(body)
            } else {
                Result.failure(ApiException(response.code(), response.message()))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            Result.failure(NetworkException(e.message ?: "Network error"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class ApiException(val code: Int, message: String) : Exception("API Error $code: $message")
class NetworkException(message: String) : Exception(message)
