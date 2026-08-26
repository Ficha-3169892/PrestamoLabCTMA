# PréstamoLab CTMA - Prototipo de Gestión de Equipos

Este proyecto es un prototipo educativo desarrollado con **Jetpack Compose** y **Clean Architecture**, siguiendo un **Flujo Unidireccional de Datos (UDF)**. La aplicación permite gestionar el catálogo de equipos de un laboratorio y realizar solicitudes de préstamo.

## 🚀 Arquitectura y Tecnologías
- **UI:** Jetpack Compose con Material Design 3.
- **Navegación:** Compose Navigation (paso de IDs entre rutas).
- **Gestión de Estado:** ViewModel + StateFlow (UDF).
- **Capa de Datos:** Repository Pattern (Implementación en memoria).
- **Pruebas:** JUnit 4 con Análisis de Valores Límite.
- **Configuración:** Gradle Kotlin DSL con Version Catalog (`libs.versions.toml`).

---

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

---

## 📋 Reglas de Negocio Implementadas (Resumen)

| ID | Regla | Implementación |
|----|-------|----------------|
| RN-01 | Solo equipos DISPONIBLES | ViewModel y Botón deshabilitado |
| RN-02 | Propósito (10-180 chars) | Función pura y validación UI |
| RN-03 | Duración (1-8 horas) | Función pura y validación UI |
| RN-05 | Prevenir duplicidad | Estado `guardando` en ViewModel |
| RN-06 | Cambio a RESERVADO | Lógica interna del Repositorio |
| RN-07 | Cancelación segura | Validación de estado en Repositorio |

---

## ⚙️ Configuración del Entorno
- **Compile SDK:** 35
- **Min SDK:** 24
- **Kotlin Target:** JVM 11
- **Kotlin Version:** 2.0.21

---

## 🔄 Flujo de Trabajo y DevOps (Gestión con GitHub)

Se ha configurado una infraestructura automatizada bajo la carpeta `.github/` para soportar prácticas de CI/CD, gestión de dependencias y control de calidad ágil.

### 1. Infraestructura de Repositorio (`.github/`)
- **Integración Continua (CI):** `.github/workflows/android-ci.yml` ejecuta automáticamente las pruebas unitarias y compila la versión debug en cada `push` o `pull_request` a la rama `main` en un contenedor Ubuntu con JDK 17.
- **Actualización de Dependencias:** `.github/dependabot.yml` comprueba de forma semanal las actualizaciones en dependencias Gradle y GitHub Actions, abriendo Pull Requests automáticos.
- **Plantilla de Historias de Usuario:** `.github/ISSUE_TEMPLATE/historia_usuario.yml` provee una plantilla interactiva estructurada en YAML para registrar Historias de Usuario en GitHub Issues, incluyendo campos para Como/Quiero/Para, Criterios de Aceptación (Dado/Cuando/Entonces), Notas Técnicas y Story Points.

### 2. Configuración de GitHub Projects (Tablero Kanban)
Para gestionar de forma visual el avance del proyecto, sigue estas instrucciones en la interfaz web de GitHub:
1. Ve a la sección **Projects** de tu repositorio y crea un proyecto usando la plantilla **Board (Kanban)**.
2. Configura las siguientes 5 columnas para la gestión de estados:
   - **Product Backlog:** Historias de usuario pendientes de priorizar.
   - **Sprint Backlog:** HU seleccionadas para el desarrollo inmediato.
   - **In Progress:** En desarrollo activo.
   - **In Review:** Pull Requests en revisión o validación.
   - **Done:** Tareas finalizadas y validadas con éxito.
3. Agrega un campo personalizado de tipo **Número** llamado **Story Points** para registrar el puntaje de esfuerzo de las historias utilizando la escala Fibonacci (1, 2, 3, 5, 8, 13).

### 3. Estándares de Ramificación (Git Flow Simplificado)
Para trabajar en una funcionalidad o Historia de Usuario específica, sigue la nomenclatura de ramas:
```bash
# Formato: feature/hu-<número>-<nombre-corto>
# Ejemplo para crear y cambiarte a la rama de la HU-3:
git checkout -b feature/hu-3-solicitar-prestamos
```

### 4. Flujo de Trabajo del Desarrollador (Paso a Paso)

1. **Crear e Implementar el Issue:**
   - En GitHub Issues, abre un nuevo issue utilizando la plantilla **Historia de Usuario (HU)**.
   - Asigna los Story Points, etiquetas y responsable. Muévela al *Sprint Backlog*.

2. **Trabajar Localmente:**
   - Crea tu rama usando la terminal de Android Studio (`git checkout -b feature/hu-<numero>-<descripcion>`).
   - Implementa los cambios en el código y agrega las pruebas necesarias.
   - Verifica que la CI compile exitosamente de manera local ejecutando:
     ```bash
     ./gradlew testDebugUnitTest
     ```

3. **Subir Cambios y Pull Request:**
   - Realiza el commit y sube tu rama:
     ```bash
     git add .
     git commit -m "feat: implementar validación y creación de solicitudes (HU-3)"
     git push origin HEAD
     ```
   - Abre un **Pull Request (PR)** hacia la rama `main` en GitHub.
   - En la descripción del PR, incluye la palabra clave vinculante `Closes #<número_issue>` (ej. `Closes #3`) para que el issue se cierre automáticamente al fusionar el PR.
   - El workflow de CI se ejecutará automáticamente para validar que compila sin errores. Una vez aprobado por el equipo, fusiona los cambios a `main`.
