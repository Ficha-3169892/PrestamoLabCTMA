# PrestamoLabCTMA 📱🧪

**PrestamoLabCTMA** es un prototipo educativo desarrollado en Android para la gestión de préstamos de equipos en los laboratorios del CTMA. La aplicación permite a los usuarios visualizar el catálogo de equipos disponibles, ver detalles técnicos y solicitar préstamos de manera rápida y sencilla.

## 🚀 Características

*   **Catálogo de Equipos:** Visualización completa de equipos categorizados (Electrónica, Herramientas, Cómputo, etc.) con sus estados en tiempo real (Disponible, Reservado, Prestado).
*   **Detalle de Equipo:** Información detallada sobre cada dispositivo antes de realizar la solicitud.
*   **Formulario de Solicitud:** Proceso simplificado para solicitar un equipo indicando el ambiente de destino, el propósito del uso y la duración estimada (1-8 horas).
*   **Gestión de Solicitudes:** Sección de "Mis Préstamos" para realizar el seguimiento de las solicitudes y cancelarlas si es necesario.
*   **Interfaz Moderna:** Construida totalmente con **Jetpack Compose** y siguiendo las guías de **Material Design 3**.

## 🛠️ Stack Tecnológico

*   **Lenguaje:** [Kotlin 2.0](https://kotlinlang.org/)
*   **UI:** [Jetpack Compose](https://developer.android.com/jetpack/compose)
*   **Navegación:** Compose Navigation
*   **Arquitectura:** MVVM (Model-View-ViewModel) con StateFlow para la gestión de estado reactivo.
*   **Componentes de UI:** Material 3 (Scaffold, BottomBar, TopAppBar, Cards, etc.).
*   **Base de Datos:** Repositorio en memoria (`InMemoryPrestamoRepository`) para fines demostrativos.

## 📂 Estructura del Proyecto

*   `data/`: Contiene la lógica de acceso a datos y repositorios.
*   `model/`: Definición de los modelos de datos (Equipo, Solicitud, Enums).
*   `ui/navigation/`: Configuración del NavHost y las rutas de la aplicación.
*   `ui/screens/`: Pantallas individuales de la interfaz de usuario.
*   `ui/viewmodel/`: Lógica de negocio y gestión del estado de la UI.
*   `ui/theme/`: Configuración de colores, tipografía y formas de Material 3.

## ⚙️ Requisitos y Ejecución

1.  Clonar el repositorio.
2.  Abrir con **Android Studio Ladybug** o superior.
3.  Asegurarse de tener instalado el SDK de Android 35 (Android 15).
4.  Sincronizar Gradle y ejecutar en un emulador o dispositivo físico (Min SDK 24).

---
*Proyecto desarrollado con fines académicos para la gestión eficiente de recursos de laboratorio.*
