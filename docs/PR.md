## Entrega

- APK Kotlin en horizontal, con receptor USB-MIDI Big Screen, controles para ampliar y suavizar, reconexión y panel MIDI.
- Emulador oficial DARK-1 1.0.7 en WebAssembly con recursos locales.
- Candidato auxiliar sobre DARK-1 1.0.7 y workflow para compilar la APK.

## Verificación

APK compilada y firma verificada. Las pruebas de SysEx cubren un barrido de 240 × 240 píxeles generado por el transmisor C. El emulador arranca y responde a controles táctiles en Chromium.

Queda pendiente probar Android 16 con un FM-1 físico, incluida la latencia, la reconexión y el audio durante la transmisión. La variante usa bandas de ocho filas para ajustarse a la memoria disponible. Véase el informe de pruebas.
