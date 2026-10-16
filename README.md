# BT Car Control (Android)

App Android nativa en Java para controlar un carrito por Bluetooth clásico SPP (HC-05).

## Funciones
- Interfaz horizontal: joystick a la izquierda y botones a la derecha.
- Emparejamiento/selección de módulos Bluetooth desde los dispositivos ya vinculados en Android.
- Al mantener pulsado un botón envía el comando; al soltarlo envía `S` (stop).
- El joystick envía el comando de la dirección dominante y `S` al soltarlo.
- Comandos seriales por defecto, un carácter ASCII sin salto de línea:
  - `F` = adelante
  - `B` = atrás
  - `L` = izquierda
  - `R` = derecha
  - `S` = detener

## Requisitos
- Android Studio reciente y Android SDK Platform 35.
- Bluetooth clásico en el teléfono.
- HC-05 encendido y emparejado desde Ajustes de Android antes de conectar en la app.

## Compilar APK
1. Abre la carpeta `BTCarControl` en Android Studio.
2. Espera la sincronización de Gradle.
3. Usa **Build > Build APK(s)**.
4. Android Studio deja el APK de depuración en `app/build/outputs/apk/debug/app-debug.apk`.

## Importante: firmware PIC
La app envía `F`, `B`, `L`, `R`, `S` en ASCII. Tu firmware del PIC18F4550 debe leer esos caracteres por UART y activar el puente H. Si tu programa usa otras letras, modifica los comandos en `MainActivity.java`. No conectes directamente el TX de un PIC a un HC-05 sin comprobar los niveles lógicos: el RX del HC-05 normalmente trabaja a 3.3 V; usa adaptación de nivel cuando corresponda. Cruza TX/RX (TX PIC → RX HC-05, TX HC-05 → RX PIC) y comparte GND.

## Generar el APK automáticamente con GitHub
1. Crea un repositorio nuevo en GitHub.
2. Sube **el contenido de esta carpeta** (incluida la carpeta `.github`) a la raíz del repositorio, donde se encuentra `settings.gradle`.
3. Abre la pestaña **Actions** y selecciona **Build Android APK**.
4. Pulsa **Run workflow** y espera a que el trabajo termine en verde.
5. En el resumen de esa ejecución, baja hasta **Artifacts** y descarga `BT-Car-Control-APK`.
6. Descomprime ese archivo ZIP: dentro estará `app-debug.apk`. Pásalo a tu Android e instálalo.

El APK de depuración no está firmado para publicación en Play Store; sirve para instalación de prueba. En Android, permite instalar desde esa fuente si el sistema te lo solicita.
