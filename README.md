# MapSalud 2.0 🏥

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-brightgreen.svg" alt="Android"/>
  <img src="https://img.shields.io/badge/Language-Kotlin-7F52FF.svg?logo=kotlin&logoColor=white" alt="Kotlin"/>
  <img src="https://img.shields.io/badge/Build-Gradle%20Kotlin%20DSL-02303A.svg?logo=gradle" alt="Gradle"/>
  <img src="https://img.shields.io/badge/UI-Material%20Design%203-6750A4.svg" alt="Material Design 3"/>
  <img src="https://img.shields.io/badge/Maps-Google%20Maps%20SDK-4285F4.svg?logo=googlemaps&logoColor=white" alt="Google Maps"/>
  <img src="https://img.shields.io/badge/Status-En%20Desarrollo-yellow.svg" alt="Status"/>
</p>

**MapSalud 2.0** es una solución móvil integral para el sector salud, diseñada para optimizar la interacción entre pacientes y profesionales médicos en entornos urbanos. La aplicación aprovecha la geolocalización en tiempo real para que los usuarios encuentren centros médicos cercanos, gestionen citas de forma digital y accedan a historiales de diagnóstico de manera segura y eficiente.

> Inspirada en la usabilidad de Google Maps y los patrones de interacción de Uber, MapSalud 2.0 busca resolver los problemas de acceso y agendamiento médico que plataformas actuales como SaludEC no han logrado resolver satisfactoriamente.

---

## 📑 Tabla de Contenidos

- [Características Principales](#-características-principales)
- [Stack Tecnológico](#-stack-tecnológico)
- [Arquitectura](#-arquitectura)
- [Diseño y UX](#-diseño-y-ux)
- [Pantallas de la Aplicación](#-pantallas-de-la-aplicación)
- [Instalación y Configuración](#-instalación-y-configuración)
- [Credenciales de Prueba](#-credenciales-de-prueba)
- [Limitaciones (v1.0)](#-limitaciones-v10)
- [Equipo](#-equipo)

---

## 🚀 Características Principales

### 👤 Para Pacientes (User Mode)

| Funcionalidad | Descripción |
|---|---|
| 📍 **Geolocalización Médica** | Visualización de centros de salud, clínicas y hospitales en un mapa interactivo (Google Maps SDK). |
| 🔍 **Búsqueda Especializada** | Filtros por tipo de centro (público/privado) y especialidad médica. |
| 📅 **Gestión de Citas** | Flujo completo para agendar, reagendar y cancelar citas con validación de disponibilidad en tiempo real. |
| 📋 **Historial de Diagnósticos** | Consulta de diagnósticos y recetas emitidas por especialistas. |
| 📍 **Centros Cercanos** | Lista ordenada por proximidad geográfica basada en la ubicación actual del usuario. |

### 👨‍⚕️ Para Médicos (Medic Mode)

| Funcionalidad | Descripción |
|---|---|
| 🗺️ **Registro de Consultorios** | Posicionamiento y gestión de consultorios en el mapa mediante marcadores. |
| ⏰ **Horarios Flexibles** | Módulo para configurar, editar y actualizar la disponibilidad horaria de atención. |
| 📊 **Panel de Control** | Gestión centralizada de citas pendientes y consultas del día. |
| 📝 **Emisión de Diagnósticos** | Interfaz para registrar detalles de consultas y tratamientos. |
| 👥 **Historial de Pacientes** | Acceso al registro de citas de pacientes individuales para seguimiento. |

---

## 🛠️ Stack Tecnológico

```
MapSalud 2.0
├── Lenguaje          → Kotlin (moderno, null-safe, conciso)
├── UI Framework
│   ├── Jetpack Compose   (estructura declarativa moderna)
│   └── XML + ViewBinding (compatibilidad y rendimiento)
├── Design System     → Material Design 3
├── Mapas             → Google Maps SDK for Android
├── Imágenes          → Picasso (carga y caché eficiente)
├── Persistencia      → SharedPreferences (sesiones y preferencias)
└── Build System      → Gradle Kotlin DSL + Version Catalog (libs.versions.toml)
```

---

## 🏗️ Arquitectura

El proyecto sigue una estructura organizada por paquetes que agrupan la funcionalidad según el rol del usuario y el tipo de dato:

```
app/src/main/java/com/example/mapsalud20/
├── ui/
│   └── theme/              # Colores y estilos (Jetpack Compose)
├── userPages/              # Actividades y lógica para el rol Paciente
│   ├── PrincipalUser.kt    # Mapa interactivo y navegación principal
│   ├── ReservarCita.kt     # Flujo de agendamiento
│   ├── Diagnosticos.kt     # Visualización de historial médico
│   └── ...                 # Gestión de citas y especialistas
├── medicPages/             # Actividades, Fragments y lógica para el rol Médico
│   ├── PrincipalMedic.kt   # Panel principal del médico
│   ├── AgregarConsultorio.kt # Gestión de ubicaciones de atención
│   ├── CitasMedicasFragment.kt # Gestión de agenda y consultas
│   └── ...                 # Diagnósticos y horarios
├── dto/                    # Data Transfer Objects (Modelos de datos)
│   ├── CitaMedica.kt       # Estructura de citas
│   ├── Medico.kt / Paciente.kt # Definición de usuarios
│   └── ...                 # Modelos para consistencia de datos
├── MainActivity.kt         # Punto de entrada de la aplicación
├── LoginActivity.kt        # Gestión de acceso y sesiones
└── RegisterActivity.kt     # Registro de nuevos usuarios (Paciente/Médico)
```

### Diagrama de Arquitectura de Información

```
Inicio: Mapa Interactivo
│
├── Marcadores de Centros Médicos
│   └── Vista de Detalles del Centro
│       └── Agendamiento
│           ├── Calendario
│           └── Selección de Hora Disponible
│               └── Gestión de Citas
│                   └── Reagendar / Cancelar
│
├── Centros Cercanos
│   ├── Lista por Proximidad
│   └── Filtros (Público / Privado)
│
└── Panel Administrativo (Médico)
    ├── Gestión de Consultorios
    ├── Configuración de Horarios
    └── Historial y Consultas
        ├── Agenda Médica
        └── Registro de Pacientes
```

---

## 🎨 Diseño y UX

El diseño de MapSalud 2.0 sigue principios de **diseño centrado en el usuario**, inspirado en Google Maps (mapas interactivos) y Uber (bottom sheets, paneles contextuales).

### Paleta de Colores

| Rol | Color | Hex | Uso |
|---|---|---|---|
| Primario | 🟢 Verde Azulado | `#0D9488` | Navegación, botones principales |
| Secundario | 💚 Verde | `#22C55E` | Confirmaciones, estado positivo |
| Fondo | ⬜ Blanco / Gris claro | `#FFFFFF / #F3F4F6` | Legibilidad y espaciado |
| Error | 🔴 Rojo | `#EF4444` | Errores, cancelaciones |
| Advertencia | 🟡 Amarillo | `#F59E0B` | Alertas y advertencias |

> Los colores siguen los lineamientos de accesibilidad **WCAG** y principios de psicología del color aplicados al sector salud.

### Tipografía

**Roboto** (Material Design) — alta legibilidad en pantallas pequeñas.

| Nivel | Peso | Uso |
|---|---|---|
| Título | Bold | Nombres de centros, headers principales |
| Subtítulo | Medium | Especialidades, horarios |
| Cuerpo | Regular | Descripciones, detalles de citas |

### Componentes UI

- **Bottom Sheets** — detalles de centros médicos sin perder el contexto del mapa.
- **Cards con elevación** — agrupación de información con jerarquía visual clara.
- **FAB (Floating Action Button)** — acción principal de agendamiento.
- **Feedback visual inmediato** — animaciones y cambios de estado en interacciones clave.

---

## 📱 Pantallas de la Aplicación

| Pantalla | Rol | Descripción |
|---|---|---|
| Mapa Principal | Paciente | Mapa interactivo con marcadores de centros médicos y acceso a detalles. |
| Centros Cercanos | Paciente | Lista por proximidad con filtros público/privado. |
| Agendamiento | Paciente | Calendario y selector de horario con validación de disponibilidad. |
| Gestión de Citas | Paciente | Listado de citas con opciones de reagendar o cancelar. |
| Panel del Médico | Médico | Mapa de gestión de consultorios y menú de herramientas. |
| Gestión de Consultorio | Médico | Registro de nuevas ubicaciones y configuración de horarios. |
| Historial y Consultas | Médico | Agenda médica del día y registro de citas por paciente. |

---

## ⚙️ Instalación y Configuración

### Prerrequisitos

- Android Studio Hedgehog (2023.1.1) o superior
- JDK 17
- Android SDK API 28+ (mínimo) / API 35 (target)
- Cuenta de Google Cloud con Maps SDK habilitado

### Pasos

1. **Clonar el repositorio:**
   ```bash
   git clone https://github.com/tu-usuario/MapSaludDispMoviles.git
   cd MapSaludDispMoviles
   ```

2. **Configurar la API Key de Google Maps:**

   Obtén una API Key en [Google Cloud Console](https://console.cloud.google.com/) y agrégala en `AndroidManifest.xml`:
   ```xml
   <meta-data
       android:name="com.google.android.geo.API_KEY"
       android:value="TU_API_KEY_AQUI" />
   ```

   > ⚠️ **Importante:** Nunca subas tu API Key al repositorio. Usa `local.properties` o variables de entorno para manejarla de forma segura.

3. **Sincronizar Gradle:**

   Abre el proyecto en Android Studio y espera a que Gradle sincronice las dependencias automáticamente.

4. **Ejecutar la aplicación:**

   Conecta un dispositivo físico o inicia un emulador con API 24+ y presiona **Run ▶**.

---

## 🔑 Credenciales de Prueba

Para propósitos de demostración y testing, utiliza las siguientes cuentas:

| Rol | Email | Contraseña |
|:---|:---|:---|
| 🧑‍💼 **Paciente** | `paciente@gmail.com` | `1234` |
| 👨‍⚕️ **Médico** | `doctor@gmail.com` | `1234` |

---

## 🚧 Limitaciones (v1.0)

Las siguientes funcionalidades **no están incluidas** en la versión inicial para reducir la complejidad del desarrollo:

- ❌ Historial clínico completo del paciente
- ❌ Integración con seguros médicos
- ❌ Telemedicina (videollamadas)
- ❌ Pagos en línea
- ❌ Recomendaciones basadas en inteligencia artificial

Estas características podrán ser consideradas en versiones futuras de la aplicación.

---

## 👥 Equipo

Desarrollado como proyecto universitario en la **Universidad Central del Ecuador** — Desarrollo de Aplicaciones para Dispositivos Móviles.

---

<p align="center">Desarrollado con ❤️ para el sector salud.</p>
