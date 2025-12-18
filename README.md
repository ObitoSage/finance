# Plata Clara - Aplicación de Gestión Financiera Personal

Una aplicación Android moderna para el control de tus finanzas personales, desarrollada en Kotlin con Material Design 3.

## Descripción

Plata Clara es una aplicación móvil que te permite:
- Registrar y visualizar tus gastos e ingresos
- Crear y seguir metas de ahorro
- Ver reportes mensuales con gráficos interactivos
- Gestionar presupuestos por categoría
- Sincronizar datos en la nube con Firebase
- Gestionar tu perfil de usuario

## Requisitos Previos

Antes de comenzar, asegúrate de tener instalado lo siguiente:

### 1. Java Development Kit (JDK)
- **Versión requerida:** JDK 11 o superior
- **Descarga:** [Oracle JDK](https://www.oracle.com/java/technologies/downloads/) o [OpenJDK](https://adoptium.net/)
- **Verificar instalación:**
  ```bash
  java -version
  ```

### 2. Android Studio
- **Versión requerida:** Android Studio Hedgehog (2023.1.1) o superior
- **Descarga:** [Android Studio](https://developer.android.com/studio)
- **Componentes necesarios:**
  - Android SDK (API Level 24 o superior)
  - Android SDK Build-Tools
  - Android Emulator (opcional, para pruebas)
  - Kotlin Plugin (viene preinstalado)

### 3. Git
- **Descarga:** [Git](https://git-scm.com/downloads)
- **Verificar instalación:**
  ```bash
  git --version
  ```

### 4. Cuenta de Firebase
- Crea una cuenta gratuita en [Firebase Console](https://console.firebase.google.com/)

## Instalación y Configuración

### Paso 1: Clonar el Repositorio

```bash
git clone https://github.com/ObitoSage/finance.git
cd finance
```

### Paso 2: Configurar Firebase

1. **Crear un proyecto en Firebase:**
   - Ve a [Firebase Console](https://console.firebase.google.com/)
   - Haz clic en "Agregar proyecto"
   - Sigue el asistente para crear tu proyecto

2. **Registrar tu aplicación Android:**
   - En la consola de Firebase, haz clic en el ícono de Android
   - **Nombre del paquete:** `com.example.finance`
   - Descarga el archivo `google-services.json`

3. **Agregar el archivo de configuración:**
   - Coloca el archivo `google-services.json` en la carpeta `app/` del proyecto
   - Ruta completa: `finance/app/google-services.json`

4. **Habilitar servicios de Firebase:**
   - **Authentication:** Ve a Authentication → Sign-in method → Habilita Email/Password
   - **Firestore Database:** Ve a Firestore Database → Crear base de datos → Modo de prueba
   - **Reglas de seguridad recomendadas:**
     ```javascript
     rules_version = '2';
     service cloud.firestore {
       match /databases/{database}/documents {
         match /users/{userId}/{document=**} {
           allow read, write: if request.auth != null && request.auth.uid == userId;
         }
       }
     }
     ```

### Paso 3: Abrir el Proyecto en Android Studio

1. Abre Android Studio
2. Selecciona "Open" o "Open an Existing Project"
3. Navega hasta la carpeta `finance` y selecciónala
4. Haz clic en "OK"
5. Espera a que Gradle sincronice el proyecto (puede tardar unos minutos)

### Paso 4: Configurar el Dispositivo

**Opción A: Usar un Emulador**
1. En Android Studio, ve a Tools → Device Manager
2. Haz clic en "Create Device"
3. Selecciona un dispositivo (recomendado: Pixel 6)
4. Descarga una imagen del sistema (recomendado: Android 11 o superior)
5. Finaliza la configuración

**Opción B: Usar un Dispositivo Físico**
1. Habilita las "Opciones de desarrollador" en tu dispositivo:
   - Ve a Configuración → Acerca del teléfono
   - Toca "Número de compilación" 7 veces
2. Habilita "Depuración USB" en Opciones de desarrollador
3. Conecta tu dispositivo con un cable USB
4. Acepta la autorización de depuración USB

### Paso 5: Ejecutar la Aplicación

1. En Android Studio, selecciona tu dispositivo/emulador en la barra de herramientas
2. Haz clic en el botón "Run" o presiona `Shift + F10`
3. La aplicación se compilará e instalará automáticamente
4. ¡Listo! La app se abrirá en tu dispositivo

## Cómo Usar la Aplicación

### Primera Vez

1. **Registro de Usuario**
   - Abre la aplicación
   - Toca "Regístrate"
   - Ingresa tu nombre, correo electrónico y contraseña
   - Toca "Crear cuenta"

2. **Inicio de Sesión**
   - Ingresa tu correo y contraseña
   - Toca "Iniciar sesión"

### Funcionalidades Principales

#### Dashboard (Pantalla Principal)
- **Vista general:** Muestra tu balance total, gastos e ingresos del mes
- **Acceso rápido:** Botones para registrar gastos e ingresos
- **Navegación:** Accede a todas las secciones desde aquí

#### Registrar Gasto
1. Toca el botón "Gastos" en el Dashboard
2. Ingresa el monto del gasto
3. Selecciona una categoría (Comida, Transporte, Café, etc.)
4. Agrega una nota opcional
5. Toca "Guardar gasto"

**Categorías disponibles:**
- Comida afuera
- Transporte
- Café
- Mercado
- Hogar
- Entretenimiento
- Servicios
- Celular

#### Registrar Ingreso
1. Toca el botón "Ingresos" en el Dashboard
2. Ingresa el monto
3. Selecciona la categoría (Salario, Freelance, Bonificación, etc.)
4. Agrega una descripción opcional
5. Toca "Guardar ingreso"

#### Mis Metas
1. Toca "Metas" en el Dashboard
2. **Crear nueva meta:**
   - Toca "+ Nueva meta"
   - Ingresa el nombre (ej: "Fondo de emergencia")
   - Define el monto objetivo
   - Selecciona un ícono y color
   - Opcionalmente, establece una fecha límite
   - Toca "Guardar meta"
3. **Agregar dinero a una meta:**
   - Toca el botón "+ Agregar" en la meta deseada
   - Ingresa el monto a agregar
   - Toca "Agregar"
4. **Eliminar una meta:** Toca el botón "✕" en la esquina de la meta

#### Reportes
1. Toca "Ver reporte" en el Dashboard
2. Visualiza:
   - Gráfico circular de gastos por categoría
   - Total gastado del mes
   - Comparación con el mes anterior
   - Lista detallada de gastos por categoría
3. Comparte el reporte tocando el botón de compartir

#### Categorías y Presupuestos
1. Toca el ícono de categorías en el Dashboard
2. Ve cuánto has gastado vs. tu presupuesto en cada categoría
3. **Configurar presupuestos:**
   - Toca "Configurar presupuesto"
   - Ajusta el monto para cada categoría
   - Toca "Guardar"

#### Historial
1. Toca "Historial" en el Dashboard
2. **Filtrar transacciones:**
   - Por tipo: Todos / Gastos / Ingresos
   - Por categoría: Usa el filtro de categorías
   - Por búsqueda: Escribe en la barra de búsqueda
3. Ve todas tus transacciones organizadas por fecha

#### Perfil de Usuario
1. Toca tu avatar en el Dashboard
2. Ve tus estadísticas de metas y balance
3. **Editar perfil:**
   - Toca "Editar perfil"
   - Actualiza nombre, correo o contraseña
   - Toca "Guardar cambios"
4. **Cerrar sesión:** Toca "Cerrar sesión" al final del perfil

## Arquitectura del Proyecto

```
finance/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/finance/
│   │   │   │   ├── dataBase/           # Room Database
│   │   │   │   │   ├── dao/            # Data Access Objects
│   │   │   │   │   ├── entities/       # Entidades de BD
│   │   │   │   │   └── repository/     # Repositorios
│   │   │   │   ├── dataClass/          # Clases de datos
│   │   │   │   ├── DashboardActivity.kt
│   │   │   │   ├── RegistrarGastoActivity.kt
│   │   │   │   ├── RegistrarIngresoActivity.kt
│   │   │   │   ├── MetasActivity.kt
│   │   │   │   ├── ReporteActivity.kt
│   │   │   │   ├── CategoriasActivity.kt
│   │   │   │   ├── HistorialActivity.kt
│   │   │   │   ├── UsuarioActivity.kt
│   │   │   │   └── ...
│   │   │   ├── res/
│   │   │   │   ├── drawable/         # Iconos y recursos gráficos
│   │   │   │   ├── layout/           # Diseños XML
│   │   │   │   ├── values/           # Colores, strings, estilos
│   │   │   │   └── ...
│   │   │   └── AndroidManifest.xml
│   ├── build.gradle.kts
│   └── google-services.json         # Configuración Firebase
├── build.gradle.kts
└── settings.gradle.kts
```

## Tecnologías Utilizadas

### Frontend
- **Kotlin** - Lenguaje de programación principal
- **ViewBinding** - Enlace de vistas
- **Material Design 3** - Componentes de UI modernos
- **RecyclerView** - Listas eficientes

### Base de Datos
- **Room Database** - Base de datos local SQLite
- **Firebase Firestore** - Base de datos en la nube
- **Firebase Authentication** - Autenticación de usuarios

### Visualización de Datos
- **MPAndroidChart** (v3.1.0) - Gráficos y reportes

### Arquitectura
- **MVVM** - Patrón de arquitectura
- **Coroutines** - Programación asíncrona
- **LiveData** - Observación de datos reactivos

### Dependencias Principales
```kotlin
// Firebase
implementation("com.google.firebase:firebase-auth:22.3.0")
implementation("com.google.firebase:firebase-firestore:24.10.0")

// Room Database
implementation("androidx.room:room-runtime:2.6.1")
implementation("androidx.room:room-ktx:2.6.1")
ksp("androidx.room:room-compiler:2.6.1")

// Charts
implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

// Material Design
implementation("com.google.android.material:material:1.11.0")
```

## Solución de Problemas

### Error: "google-services.json not found"
**Solución:** Asegúrate de haber descargado el archivo `google-services.json` de Firebase Console y colocarlo en la carpeta `app/`.

### Error: "Sync failed"
**Solución:** 
1. Verifica tu conexión a Internet
2. En Android Studio: File → Invalidate Caches → Restart
3. Limpia el proyecto: Build → Clean Project

### La app se cierra al iniciar
**Solución:**
1. Verifica que Firebase esté configurado correctamente
2. Revisa los logs en Logcat (View → Tool Windows → Logcat)
3. Asegúrate de que las reglas de Firestore permitan lectura/escritura

### No se guardan los datos
**Solución:**
1. Verifica que el usuario esté autenticado
2. Comprueba las reglas de seguridad de Firestore
3. Revisa que tengas conexión a Internet (para Firebase)

## Paleta de Colores

La aplicación utiliza una paleta de colores consistente:

- **Primary Dark:** `#212842` - Azul oscuro principal
- **Primary Light:** `#F0E7D5` - Beige claro
- **Accent Teal:** `#14B8A6` - Verde azulado para acentos
- **Red:** `#EF4444` - Rojo para gastos
- **Green:** `#10B981` - Verde para ingresos

## Notas Adicionales

### Formato de Moneda
- La aplicación usa el formato de moneda colombiano (COP)
- Los montos se muestran con separadores de miles
- Símbolo: `$`

### Sincronización
- Los datos se sincronizan automáticamente con Firebase
- La base de datos local (Room) funciona offline
- Al conectarse a Internet, los datos se sincronizan

### Privacidad
- Cada usuario solo puede acceder a sus propios datos
- Las contraseñas se almacenan de forma segura con Firebase Auth
- Los datos están encriptados en tránsito

## Contacto

- **Repositorio:** [https://github.com/ObitoSage/finance](https://github.com/ObitoSage/finance)
- **Issues:** [https://github.com/ObitoSage/finance/issues](https://github.com/ObitoSage/finance/issues)

---

**Desarrollado con amor en Kotlin**
