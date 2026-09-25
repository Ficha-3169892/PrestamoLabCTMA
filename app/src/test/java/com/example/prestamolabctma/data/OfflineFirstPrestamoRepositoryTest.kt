package com.example.prestamolabctma.data

import com.example.prestamolabctma.data.local.EquipoDao
import com.example.prestamolabctma.data.local.EquipoEntity
import com.example.prestamolabctma.data.local.PrestamoDao
import com.example.prestamolabctma.data.local.PrestamoEntity
import com.example.prestamolabctma.data.remote.PrestamoApi
import com.example.prestamolabctma.data.remote.RetrofitPrestamoDataSource
import com.example.prestamolabctma.model.EstadoEquipo
import com.example.prestamolabctma.model.EstadoSolicitud
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import java.io.File

class OfflineFirstPrestamoRepositoryTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var repository: OfflineFirstPrestamoRepository
    private lateinit var api: PrestamoApi
    private lateinit var tempDir: File
    
    // Fake DAO usando MutableStateFlow para simular la reactividad de Room
    private val fakeDao = object : PrestamoDao {
        private val _data = MutableStateFlow<List<PrestamoEntity>>(emptyList())
        
        override fun getAllPrestamos() = _data
        
        override suspend fun getPrestamoById(id: Int) = _data.value.find { it.id == id }
        
        override suspend fun insertPrestamos(prestamos: List<PrestamoEntity>) {
            _data.update { it + prestamos }
        }
        
        override suspend fun insertPrestamo(prestamo: PrestamoEntity) {
            _data.update { it + prestamo }
        }
        
        override suspend fun deleteAllPrestamos() {
            _data.value = emptyList()
        }
        
        override suspend fun replaceAll(prestamos: List<PrestamoEntity>) {
            _data.value = prestamos.toList()
        }
    }

    private val fakeEquipoDao = object : EquipoDao {
        private val _equipos = MutableStateFlow<List<EquipoEntity>>(emptyList())

        override fun getAllEquipos() = _equipos

        override suspend fun getEquipoById(id: Int) = _equipos.value.find { it.id == id }

        override suspend fun insertEquipos(equipos: List<EquipoEntity>) {
            _equipos.update { it + equipos }
        }

        override suspend fun insertEquipo(equipo: EquipoEntity) {
            _equipos.update { it + equipo }
        }

        override suspend fun updateEstadoEquipo(id: Int, nuevoEstado: EstadoEquipo) {
            _equipos.update { list ->
                list.map { if (it.id == id) it.copy(estado = nuevoEstado) else it }
            }
        }

        override suspend fun deleteAllEquipos() {
            _equipos.value = emptyList()
        }

        override suspend fun replaceAll(equipos: List<EquipoEntity>) {
            _equipos.value = equipos.toList()
        }
    }

    @Before
    fun setup() {
        mockWebServer = MockWebServer()
        tempDir = File(System.getProperty("java.io.tmpdir"), "test_evidencias")
        val contentType = "application/json".toMediaType()
        val jsonConfig = Json { 
            ignoreUnknownKeys = true 
            coerceInputValues = true
        }
        api = Retrofit.Builder()
            .baseUrl(mockWebServer.url("/"))
            .addConverterFactory(jsonConfig.asConverterFactory(contentType))
            .build()
            .create(PrestamoApi::class.java)
        
        val remoteDataSource = RetrofitPrestamoDataSource(api)
        repository = OfflineFirstPrestamoRepository(remoteDataSource, fakeDao, fakeEquipoDao, tempDir)
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `refresh exitoso actualiza cache local`() = runTest {
        val json = """
            [
                {
                    "id": 1,
                    "equipo_id": 101,
                    "usuario_id": "user123",
                    "ambiente_destino": "Lab 1",
                    "proposito": "Clase",
                    "fecha_solicitud": "2023-10-01T10:00:00",
                    "fecha_inicio": "2023-10-01T14:00:00",
                    "duracion_horas": 2,
                    "estado": "APROBADA"
                }
            ]
        """.trimIndent()
        
        mockWebServer.enqueue(MockResponse().setBody(json).setResponseCode(200))
        
        val result = repository.refreshPrestamos()
        
        assertTrue("El resultado del refresh debe ser Success", result.isSuccess)
        
        val cached = repository.obtenerSolicitudesFlow().first()
        assertEquals("Debería haber 1 elemento en la caché", 1, cached.size)
        assertEquals("El ambiente de destino debe coincidir", "Lab 1", cached[0].ambienteDestino)
    }

    @Test
    fun `error 500 no borra cache previo`() = runTest {
        fakeDao.insertPrestamo(PrestamoEntity(1, 101, "u1", "Ambiente A", "P", "2023-10-01T10:00:00", "2023-10-01T10:00:00", 1, EstadoSolicitud.SOLICITADA, null, null, 0))
        
        mockWebServer.enqueue(MockResponse().setResponseCode(500))
        
        val result = repository.refreshPrestamos()
        
        assertTrue("El resultado debe ser Failure", result.isFailure)
        val cached = repository.obtenerSolicitudesFlow().first()
        assertEquals("Los datos previos deben conservarse", 1, cached.size)
        assertEquals("Ambiente A", cached[0].ambienteDestino)
    }

    @Test
    fun `json invalido es capturado como fallo`() = runTest {
        mockWebServer.enqueue(MockResponse().setBody("{ \"id\": \"no_soy_un_int\" }").setResponseCode(200))
        
        val result = repository.refreshPrestamos()
        
        assertTrue("Debe fallar por error de parsing", result.isFailure)
    }
}
