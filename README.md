# BTCarControl

Proyecto Android para control Bluetooth clásico de un carrito con HC-05/HC-06.

## Comandos Bluetooth
La app envía caracteres ASCII: `F` adelante, `B` atrás, `L` izquierda, `R` derecha y `S` detener. Al soltar un botón también envía `S`.

El firmware del PIC18F4550 debe reconocer esos caracteres; si tu firmware usa otros comandos, cambia los valores en `MainActivity.java`.

## Compilar
El workflow GitHub Actions compila con `gradle assembleDebug` y publica el APK como artifact `BTCarControl-APK`.

Empareja primero el HC-05 desde Ajustes Bluetooth del teléfono. Prueba con las ruedas levantadas y confirma que el PIC interpreta estos comandos antes de usar el carro.
