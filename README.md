# DARK-1 Mirror Tablet

APK nativa en Kotlin para ampliar la pantalla de un M-VAVE FM-1 por USB-MIDI. Incluye el emulador oficial DARK-1 1.0.7, compilado a WebAssembly y empaquetado para uso local.

El modo espejo requiere un firmware DARK-1 BigScreen. La revisión original de BigScreen funcionó en la unidad del usuario. La versión Fast mejora el transporte y conserva los mismos mensajes Big Screen. Las mediciones de latencia y audio de Fast en el FM-1 físico siguen pendientes.

- [Instalación, conexión y revisión del hardware](docs/INSTALACION.md)
- [Protocolo USB-MIDI](docs/PROTOCOLO.md)
- [Rendimiento de la versión rápida](docs/RENDIMIENTO.md) · [Informe inicial y plan de pruebas físicas](docs/PRUEBAS.md)

## Compilar

En Linux, con JDK 17, SDK Android 35, Build Tools 35.0.0 y un compilador C:

```sh
./gradlew :app:assembleDebug :app:testDebugUnitTest
```

El APK se genera en `app/build/outputs/apk/debug/app-debug.apk`. GitHub Actions lo empaqueta como `DARK1-MIRROR-TABLET_v1.0-comp.2.apk`. La firma de depuración sirve para instalación directa de esta compilación.

`scripts/build-upstream.sh emulator` reconstruye los recursos locales. Requiere Python 3, Pillow y Clang con destino wasm32. Se usó WASI SDK 25. Las pruebas del emulador se ejecutan con `node tests/emulator.cjs`, Playwright y Chromium.

`scripts/build-upstream.sh firmware` reconstruye el paquete auxiliar. Usa el toolchain JieLi `20260730.1` y el SDK AC79NN V1.2.1, commit `d179b4484759423312073f5fbb232501aa491047`. Configura `JIELI_TOOLCHAIN` y `AC79_SDK` antes de ejecutarlo. El resultado queda en `build/DARK1-BigScreen-v1.0.7-fast.fwsc`. Consulta las instrucciones del proyecto para obtener las dependencias. [Compilación de DARK-1](https://github.com/sparky480v/Dark-1/blob/6c0cc34677c4188575158c9ea5f53c95200be4a0/BUILDING.md)

## Código y licencias

- **DARK-1**, commit `6c0cc34677c4188575158c9ea5f53c95200be4a0`. El emulador y la variante mantienen GPL-3.0-only. El paquete de entrega incluye el código fuente original de ese commit. [Licencias y atribuciones de DARK-1](https://github.com/sparky480v/Dark-1/blob/6c0cc34677c4188575158c9ea5f53c95200be4a0/LICENSING.md)
- **FM-1 Mirror/Spyglass**, commit `793364bf4ac38317ebd13443c59957637b00d8d2`. El receptor reutiliza la decodificación de pantalla de Patrick F. Kellogg bajo GPL-3.0. [Repositorio original](https://github.com/pfkellogg/fm1-mirror-spyglass-android/tree/793364bf4ac38317ebd13443c59957637b00d8d2)
- **Big Screen**, commit `170b844df43ba19ac001aa0cbeef77fe4b8fa6e7`. El transmisor reutiliza `screen_mirror.c` y `screen_mirror.h` de Patrick F. Kellogg bajo MIT. [Código original](https://github.com/pfkellogg/sloop-fm1/tree/170b844df43ba19ac001aa0cbeef77fe4b8fa6e7) · [Aviso MIT](firmware/LICENSE-MIT.txt)
