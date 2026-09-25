# PrestamoLabCTMA 📱🧪

![Android CI](https://github.com/YOUR_GITHUB_USER/PrestamoLabCTMA/actions/workflows/android-ci.yml/badge.svg)

**PrestamoLabCTMA** es un prototipo educativo desarrollado en Android para la gestión de préstamos de equipos en los laboratorios del CTMA. La aplicación permite a los usuarios visualizar el catálogo de equipos disponibles, ver detalles técnicos, solicitar préstamos, adjuntar evidencia fotográfica y registrar devoluciones con validación GPS.

## 🚀 Características por Semana / Módulo

*   **Semana 5 — Arquitectura UDF y Navegación:** Separación limpia UDF (UI → ViewModel → Repository → DataSource). Flujo completo con Compose Navigation: Catálogo → Detalle de Equipo → Solicitar Préstamo → Mis Préstamos.
*   **Semana 6 — Room & DataStore:** Base de datos Room (`PrestamoEntity`, `EquipoEntity`) funcionando como Single Source of Truth. Persistencia de preferencias con Preferences DataStore (`UserPreferencesRepository` para filtros, búsquedas y último rol).
*   **Semana 7 — Corrutinas, Flow y Estados de UI:** Funciones `suspend` con `Dispatchers.IO`, flujos reactivos expuestos como `Flow` y recolectados en Compose mediante `collectAsStateWithLifecycle()`. Modelado de estados explícitos (`UiStatus`: `Loading`, `Content`, `Empty`, `Error`, `Operation`).
*   **Semana 8 — Retrofit, OkHttp y Offline-First:** Configuración de cliente REST con DTOs, mapeadores (`DTO -> Domain -> Entity`) y estrategia offline-first (`OfflineFirstPrestamoRepository`). Manejo clasificado de errores HTTP (401, 404, 5xx) y falta de conexión.
*   **Semana 9 — Dispositivo, Notificaciones, Capacidad Física y Seguridad:**
    *   **Evidencia Fotográfica:** Integración de Photo Picker y Cámara usando `FileProvider`, procesando URIs y guardando metadatos en Room (sin cargar Bitmaps directos en memoria).
    *   **Capacidad Física Adicional (GPS/Ubicación):** Integración con `FusedLocationProviderClient` para capturar latitud/longitud y timestamp al confirmar la entrega o devolución del préstamo. *(Decisión: Se eligió GPS porque encaja naturalmente con la verificación del espacio/laboratorio físico de entrega/devolución del equipo)*.
    *   **Notificaciones Locales:** Canal y recordatorios de devolución con `NotificationHelper`.
    *   **Seguridad:** Solicitud de permisos en tiempo de ejecución bajo demanda (`ACCESS_FINE_LOCATION`, `POST_NOTIFICATIONS`), comunicación obligatoria por HTTPS y variables de entorno (`API_BASE_URL`, `SUPABASE_URL`, `SUPABASE_ANON_KEY`) inyectadas vía `BuildConfig` desde `local.properties`.

## 🛠️ Stack Tecnológico

*   **Lenguaje:** [Kotlin 2.0](https://kotlinlang.org/)
*   **UI:** [Jetpack Compose](https://developer.android.com/jetpack/compose) con Material Design 3
*   **Navegación:** Compose Navigation con `collectAsStateWithLifecycle()`
*   **Arquitectura:** MVVM / UDF (Unidirectional Data Flow)
*   **Persistencia:** Room Database + Preferences DataStore
*   **Red:** Retrofit + OkHttp + KotlinX Serialization
*   **Ubicación:** Google Play Services Location (`FusedLocationProviderClient`)
*   **Cámara / Imágenes:** Photo Picker + `FileProvider` + Coil
*   **Pruebas:** JUnit, kotlinx-coroutines-test, MockWebServer, Compose UI Test

## 📂 Estructura del Proyecto

*   `data/`: Repositorios offline-first, entidades Room (`AppDatabase`, `PrestamoDao`, `EquipoDao`), DTOs y datasources.
*   `data/datastore/`: Persistencia de preferencias del usuario (`UserPreferencesRepository`).
*   `model/`: Modelos de dominio (`Equipo`, `SolicitudPrestamo`, `Usuario`, `Enums`).
*   `ui/navigation/`: NavHost (`PrestamoNavHost`), rutas y manejo de permisos runtime.
*   `ui/screens/`: Pantallas de Compose (`CatalogoScreen`, `EquipoDetalleScreen`, `SolicitudFormScreen`, `MisPrestamosScreen`, `ReportarFallaScreen`, `AdminScreen`, `LoginScreen`).
*   `ui/viewmodel/`: `PrestamoViewModel` y estados explícitos de la UI (`PrestamoUiState`, `UiStatus`, `RefreshState`).
*   `util/`: `LocationHelper` (GPS) y `NotificationHelper` (Notificaciones locales).
*   `androidTest/`: Pruebas de UI (`NavigationFlowTest`) y auditoría de accesibilidad (`AccessibilityTest`).
*   `test/`: Pruebas unitarias de ViewModels, Repositorios y TDD.

## 🧪 Pruebas Ejecutadas

Para verificar el correcto funcionamiento del proyecto, se ejecutan los comandos estándar de Gradle:

```bash
# Compilación del APK de depuración
./gradlew :app:assembleDebug

# Ejecución de la suite de pruebas unitarias y de repositorio
./gradlew :app:testDebugUnitTest
```

Resultados de Verificación:
*   **Compilación:** `:app:assembleDebug` exitosa.
*   **Pruebas Unitarias:** 18 pruebas pasadas en `PrestamoViewModelTest` y `OfflineFirstPrestamoRepositoryTest`.

---
*Proyecto desarrollado con fines académicos para la gestión eficiente de recursos de laboratorio.*
