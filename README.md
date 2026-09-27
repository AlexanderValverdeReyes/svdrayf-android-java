# SVDRAYF - Aplicación Móvil Android (Offline-First)

## Descripción General
Este repositorio contiene el código fuente de la aplicación móvil nativa para el Sistema Versátil de Recaudo Automatizado y Fiscalización (SVDRAYF). Está desarrollada en Java para Android y está diseñada específicamente para ser operada por los cobradores y los inspectores de ruta de la empresa de transporte "Turismo Kataleant - Maleño VIP".

La aplicación implementa una estricta arquitectura **Offline-First**, permitiendo a los cobradores emitir boletos, gestionar turnos y realizar arqueos de caja en tiempo real sin necesidad de conectividad a internet, sincronizando los datos automáticamente con el servidor central una vez que se recupera la señal de red en la carretera.

## Arquitectura y Tecnologías
- **Lenguaje:** Java (Android Nativo).
- **Patrón de Arquitectura:** MVVM (Model-View-ViewModel).
- **Persistencia Local:** Room Database (SQLite) con cifrado AES-256 (SQLCipher).
- **Sincronización en Segundo Plano:** WorkManager (Android Jetpack).
- **Conectividad de Red:** Retrofit (para comunicación HTTPS con el backend).
- **Hardware Integrado:** API de Bluetooth (para impresoras térmicas portátiles), CameraX/ZXing (para escaneo de códigos QR).
- **Despliegue:** Distribución de APK mediante Vercel.

## Módulos y Funcionalidades Principales
La aplicación está segmentada en dos flujos de trabajo principales según el rol del usuario:

### Módulo de Cobrador (Venta en Ruta)
- **Autenticación Híbrida:** Inicio de sesión en línea contra el servidor o validación local contra la caché cifrada en caso de estar offline.
- **Configuración de Hardware:** Emparejamiento y monitoreo de batería y papel de miniticketeras térmicas vía Bluetooth.
- **Gestión de Turnos:** Apertura y cierre de jornadas de viaje con sellos de tiempo inmutables.
- **Flujo de Venta:**
  - Selección de unidad de bus y tipo de ruta (Directo o con Paraderos).
  - Selección de paraderos de origen y destino basados en una matriz estática de tarifas.
  - Categorización de pasajeros (General, Universitario, Frecuente).
  - Selección de modalidad de pago (Efectivo o Billetera Digital QR).
  - Generación de hash criptográfico y emisión de boleto físico con código QR.
- **Gestión de Caja:** Visualización de cuadre operativo en tiempo real, anulación de boletos (Soft Delete) con justificación obligatoria y reimpresión de comprobantes por fallos mecánicos.

### Módulo de Inspector (Fiscalización en Vía)
- **Escaneo de QR:** Uso de la cámara del dispositivo para validar la autenticidad de los boletos emitidos, operando de forma 100% offline.
- **Feedback Visual:** Interfaz semafórica de alta visibilidad (Verde para válido, Rojo para error/fraude).
- **Registro de Incidencias:** Bitácora local para el registro de pasajeros sin boleto, boletos duplicados o irregularidades en ruta.

## Requisitos Previos
- Android Studio (versión recomendada: Flamingo o superior).
- JDK 11 o superior.
- Dispositivo Android físico o emulador (Android 8.0 / API 26 o superior).

## Instalación y Compilación
1. Clonar el repositorio:
   ```bash
   git clone https://github.com/AlexanderValverdeReyes/svdrayf-android-java.git
