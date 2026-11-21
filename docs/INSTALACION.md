# Instalación y conexión

## Probar la APK

1. En la tablet, abre `DARK1-MIRROR-TABLET_v1.0-comp.1.apk` y completa el instalador de Android. Si Android lo solicita, autoriza la instalación para el gestor de archivos que abre el APK. Se trata de una compilación con firma de depuración.
2. Abre **DARK-1 Mirror** en horizontal. Toca **Modo espejo real** para cambiar a **Modo emulador**.
3. Pulsa **Switch on** dentro del emulador. Después de las pantallas de arranque, toca botones del panel virtual y cambia de género o pieza. La simulación funciona con los recursos incluidos en el APK.
4. Toca **Modo emulador** para volver a **Modo espejo real**.

## Conectar el FM-1 para una prueba física

1. Comprueba qué firmware ejecuta el FM-1. DARK-1 original carece de salida de pantalla por el protocolo Big Screen.
2. Con un FM-1 que ya ejecute la variante auxiliar, usa un cable USB-C de datos y confirma que la tablet ejerce de host USB. Cierra otras aplicaciones que ocupen el puerto MIDI.
3. En **Modo espejo real**, espera que Android abra el puerto y aparezca la identidad **DARK-1 1.0.7 BigScreen**. **MIDI** muestra el nombre reconocido y cuántos segmentos gráficos ha recibido la app.
4. Prueba **Nítido** y **Suavizado** y activa **Solo pantalla** para ocultar los controles. Toca la pantalla o pulsa Atrás para recuperarlos. Tras desenchufar y volver a enchufar, deja la app abierta para comprobar la reconexión.

## Comprobar la unidad antes de flashear

El candidato se compiló para el M-VAVE FM-1 que admite el DARK-1 original. El código de DARK-1 1.0.7 contempla la flash **Puya P25Q80, JEDEC `0x856014`**, y la **UCUN UC25HQ80, JEDEC `0xB36014`**. La aceptación en código de ambos chips no equivale a una validación de sus revisiones en hardware. [Historial de DARK-1](https://github.com/sparky480v/Dark-1/blob/6c0cc34677c4188575158c9ea5f53c95200be4a0/docs/CHANGELOG.md)

1. Anota el modelo exacto, la revisión de placa documentada y el firmware actual. En un ordenador, la consulta `python3 tools/fm1_install.py --info` del proyecto DARK-1 devuelve la identidad del firmware y si está en ejecución o en modo de actualización. Esa consulta no identifica el chip.
2. Confirma la referencia de la flash con la documentación fiable de esa revisión o con una lectura JEDEC de solo lectura por un medio ya validado para tu unidad. Si la revisión o el chip quedan sin confirmar, detén la prueba en el modo emulador.
3. Antes de autorizar una prueba de instalación, guarda una copia de las piezas, los ajustes y las voces propias. Ten a mano el paquete original y el método de recuperación de esa unidad. Verifica el SHA-256 del candidato, carga el FM-1 y conéctalo directamente al ordenador.
4. La instalación es manual y requiere autorización explícita. Si la prueba se autoriza, sigue el instalador del proyecto DARK-1 y su confirmación interactiva. Mantén la alimentación y la conexión durante la escritura. Usa las comprobaciones normales del instalador y conserva los registros. [Instalación y recuperación oficiales](https://github.com/sparky480v/Dark-1/blob/6c0cc34677c4188575158c9ea5f53c95200be4a0/BUILDING.md)

El APK solo intercambia mensajes de identificación y pantalla. El candidato incluido lleva el nombre `DARK1-BigScreen-v1.0.7.fwsc` y la identidad de paquete `FM-1_32010007`, compartida con DARK-1 1.0.7. Distingue las variantes por el nombre, el hash y el anuncio Big Screen después del arranque.
