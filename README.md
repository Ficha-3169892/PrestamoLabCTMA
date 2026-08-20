# PréstamoLab CTMA - Sistema de Gestión de Préstamos

Aplicación Android moderna desarrollada con **Jetpack Compose** para la gestión y control de préstamos de equipos de laboratorio en el centro CTMA. El proyecto sigue una arquitectura limpia (Clean Architecture) y el patrón de flujo de datos unidireccional (UDF).

## 🚀 Características y Funcionalidades

- **Catálogo de Equipos:** Visualización de inventario con estados en tiempo real.
- **Gestión de Préstamos:** Formulario de solicitud con validaciones estrictas.
- **Historial Personal:** Listado de "Mis Solicitudes" para seguimiento.
- **Cancelación Segura:** Opción de cancelar solicitudes pendientes con restauración automática de stock.
- **Accesibilidad:** Uso de badges con iconos y texto explícito para estados.

## 🏗️ Arquitectura del Proyecto

La aplicación está dividida en capas para garantizar el desacoplamiento y la facilidad de pruebas:

### 1. Capa de Dominio (`domain.model`)
- Código Kotlin puro sin dependencias de Android.
- **Modelos:** `Equipo`, `SolicitudPrestamo`.
- **Enums:** Categorías de equipo, estados de equipo y estados de solicitud.

### 2. Capa de Datos (`data.repository`)
- **PrestamoRepository:** Interfaz que define el contrato de datos.
- **InMemoryPrestamoRepository:** Implementación funcional mediante un **Singleton (`object`)** que persiste los datos durante la sesión de la app, manejando la lógica de actualización de estados de los equipos (RN-06).

### 3. Capa de Presentación (`ui`)
- **ViewModel:** Gestión de estado mediante `StateFlow` y lógica de negocio.
- **UDF (Unidirectional Data Flow):** El estado fluye hacia la UI y los eventos fluyen hacia el ViewModel.
- **Navigation:** Grafo de navegación centralizado con paso seguro de argumentos (identificadores de tipo `Int`).

## 🛠️ Reglas de Negocio Implementadas (RN)

| ID | Regla de Negocio | Implementación |
|:---|:---|:---|
| **RN-01** | Disponibilidad | Solo se pueden solicitar equipos en estado `DISPONIBLE`. |
| **RN-02** | Ambiente Obligatorio | El campo ambiente de destino no puede estar vacío. |
| **RN-03** | Longitud de Propósito | El propósito debe tener entre 10 y 180 caracteres. |
| **RN-04** | Duración Máxima | El préstamo debe ser de entre 1 y 8 horas. |
| **RN-05** | Prevención de Duplicados | Bloqueo de UI (`guardando = true`) durante el proceso de envío. |
| **RN-06** | Reserva Automática | Al crear una solicitud, el equipo cambia automáticamente a `RESERVADO`. |
| **RN-07** | Cancelación Restringida | Solo se pueden cancelar solicitudes en estado `SOLICITADA`. |
| **RN-08** | Control de Errores | Manejo de IDs inexistentes en rutas mediante pantalla de error recuperable. |
| **RN-09** | Privacidad | Uso de datos sintéticos y nombres genéricos de laboratorio. |

## 💻 Stack Tecnológico

- **Lenguaje:** Kotlin 2.2.10
- **UI:** Jetpack Compose con Material Design 3
- **Navegación:** Compose Navigation
- **Arquitectura:** ViewModel, StateFlow, Clean Architecture
- **Inyección de Dependencias:** Manual mediante ViewModel Factory

---
Desarrollado para el entorno de aprendizaje CTMA utilizando prácticas modernas de desarrollo Android.
