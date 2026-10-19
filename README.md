# VITAIROS Industrial AI — App móvil de captura en terreno

**Asignatura:** DSY1105 · Desarrollo de Aplicaciones Móviles  
**Institución:** Duoc UC  
**Evaluación:** Parcial 2 — Encargo en parejas  
**Integrantes:**
- Cindia Maldonado
- Héctor Eduardo Contreras

**N° de equipo:** [Completar]  
**Docente:** [Completar]

---

## Descripción

Aplicación Android nativa desarrollada en **Kotlin + Jetpack Compose** para la captura offline de reportes operacionales industriales. Permite registrar inspecciones, mantenciones e incidentes de equipos en terreno, guardarlos localmente y sincronizarlos al servidor cuando exista conexión.

## Funcionalidades implementadas

- **Login simulado** con perfiles Operador y Supervisor
- **Dashboard del operador** con tiles de estado en tiempo real (En cola / Error / Borrador / Sincronizados)
- **Nuevo reporte** con formulario validado: tipo, proceso, área, turno, equipo/TAG, severidad, observaciones (500 caracteres), ubicación GPS
- **Cámara** con permiso en tiempo de ejecución + selección de imagen desde galería + adjunto PDF
- **GPS/Ubicación** con permisos en tiempo de ejecución
- **Persistencia local offline-first** con Room (SQLite)
- **Arquitectura MVVM** con `AndroidViewModel` y `StateFlow`
- **6 estados** por reporte: Borrador → En cola → Enviando → Sincronizado / Error / Requiere corrección
- **Cola de sincronización** con barra de progreso y **auto-retry de 3 intentos** (5 s / 15 s / 30 s) antes de marcar ERROR
- **Reintentar** reportes en error individualmente desde la lista
- **Pantalla de Detalle** (solo lectura) y **edición** de reportes no sincronizados
- **Historial** filtrable de todos los reportes y sus estados
- **Panel del Supervisor** para revisar y marcar "Requiere corrección" con nota
- **Animaciones**: `AnimatedVisibility`, `animateColorAsState`, `LinearProgressIndicator`
- **Identificador automático** formato `VIA-YYMMDD-NNNN` (editable)

## Stack tecnológico

| Componente | Versión |
|---|---|
| Kotlin | 2.2.10 |
| Jetpack Compose BOM | 2026.02.01 |
| Room | 2.8.4 |
| AGP | 9.4.1 |
| Gradle | 9.6.0 |
| Min SDK | 26 (Android 8.0) |
| Target SDK | 37 |

## Pasos para ejecutar

1. Clonar este repositorio
2. Abrir en **Android Studio** (versión Ladybug o superior)
3. Esperar sincronización de Gradle
4. Conectar emulador o dispositivo Android 8.0+ (API 26+)
5. Ejecutar con **Run ▶**

> Los permisos de cámara y ubicación se solicitan la primera vez que se usan.

## Arquitectura

```
com.vitairos.industrial/
├── MainActivity.kt          — Navegación + todas las pantallas Compose
├── Reporte.kt               — Entidad Room (modelo de datos)
├── ReporteDao.kt            — DAO Room (consultas)
├── VitairosDatabase.kt      — Base de datos Room (migraciones v1→v5)
├── ReportesViewModel.kt     — ViewModel MVVM con StateFlow
├── GestorUbicacion.kt       — Composable GPS con permisos
└── PantallaSincronizacion.kt — Cola de envíos + auto-retry
```

## Paleta de colores VITAIROS

| Color | Hex | Uso |
|---|---|---|
| Verde noche | `#081D16` | Fondo principal |
| Verde profundo | `#102B23` | Tarjetas y barra |
| Cobre | `#DD7A52` | Acción principal / Error |
| Oro | `#D9B46C` | Acento / En cola |
| Hueso | `#FBF7F1` | Texto principal |
| Acero | `#AAB6B7` | Texto secundario |
