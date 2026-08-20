# PréstamoLab CTMA - Prototipo de Gestión de Equipos

Este proyecto es un prototipo educativo desarrollado con **Jetpack Compose** y **Clean Architecture**, siguiendo un **Flujo Unidireccional de Datos (UDF)**. La aplicación permite gestionar el catálogo de equipos de un laboratorio y realizar solicitudes de préstamo.

## 🚀 Arquitectura y Tecnologías
- **UI:** Jetpack Compose con Material Design 3.
- **Navegación:** Compose Navigation (paso de IDs entre rutas).
- **Gestión de Estado:** ViewModel + StateFlow (UDF).
- **Capa de Datos:** Repository Pattern (Implementación en memoria).
- **Pruebas:** JUnit 4 con Análisis de Valores Límite.
- **Configuración:** Gradle Kotlin DSL con Version Catalog (`libs.versions.toml`).

## 🛠️ Pasos de Implementación

### 1. Dominio y Modelos
Se definieron las estructuras base en el paquete `model`:
- **Enums:** `CategoriaEquipo`, `EstadoEquipo`, `EstadoSolicitud`.
- **Data Classes:** `Equipo` (identidad y estado) y `SolicitudPrestamo` (trazabilidad).

### 2. Repositorio (Capa de Datos)
Implementación de `InMemoryPrestamoRepository`:
- Manejo de listas mutables para simular persistencia.
- **Regla RN-06:** Al crear una solicitud, el equipo cambia automáticamente a `RESERVADO`.
- **Regla RN-07:** Cancelación permitida solo en estado `SOLICITADA`, devolviendo el equipo a `DISPONIBLE`.

### 3. ViewModel y Lógica de Negocio
Gestión centralizada en `PrestamoViewModel`:
- **Validaciones:** `propositoValido` (10-180 caracteres) y `duracionValida` (1-8 horas).
- **Regla RN-01:** Validación de disponibilidad antes de procesar préstamos.
- **Regla RN-05:** Control de concurrencia en la UI mediante el estado `guardando`.

### 4. Interfaz de Usuario (UI)
Diseño basado en componentes de Material 3:
- **CatalogoScreen:** Lista con LazyColumn y feedback visual de disponibilidad.
- **EquipoDetalleScreen:** Información extendida y control de estados (RN-08).
- **FormularioSolicitudScreen:** Validación en tiempo real y mensajes de error.
- **MisPrestamosScreen:** Historial de solicitudes con capacidad de cancelación.

### 5. Navegación
Configuración de `AppNavigation`:
- Rutas seguras: `Catalogo`, `EquipoDetalle/{id}`, `Solicitar/{id}` y `MisSolicitudes`.
- Uso de `Scaffold` con `NavigationBar` para el menú principal.

### 6. Pruebas Unitarias
Localizadas en `PrestamoViewModelTest.kt`:
- Cobertura total de las reglas de validación mediante **Análisis de Valores Límite** (ej: probando duraciones de 0, 1, 8 y 9 horas).

## 📋 Reglas de Negocio Implementadas (Resumen)

| ID | Regla | Implementación |
|----|-------|----------------|
| RN-01 | Solo equipos DISPONIBLES | ViewModel y Botón deshabilitado |
| RN-02 | Propósito (10-180 chars) | Función pura y validación UI |
| RN-03 | Duración (1-8 horas) | Función pura y validación UI |
| RN-05 | Prevenir duplicidad | Estado `guardando` en ViewModel |
| RN-06 | Cambio a RESERVADO | Lógica interna del Repositorio |
| RN-07 | Cancelación segura | Validación de estado en Repositorio |

## ⚙️ Configuración del Entorno
- **Compile SDK:** 35
- **Min SDK:** 24
- **Kotlin Target:** JVM 11
- **Kotlin Version:** 2.0.21
