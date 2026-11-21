# Informe de pruebas de la compilación 1.0-comp.1

## Resultados

| Prueba | Resultado |
|---|---|
| APK nativa | Ensamblada con Gradle 8.11.1, JDK 17 y SDK Android 35. Firma Android Debug v2 válida. ID `com.josxtm.dark1mirror`, API mínima 30 y objetivo 35. |
| C → SysEx → Android | Tres pruebas JVM correctas. El transmisor C formó segmentos Big Screen a partir de una imagen sintética de 57.600 píxeles. El receptor Kotlin procesó fragmentación, mensajes unidos y reloj MIDI intercalado y verificó los 57.600 píxeles ARGB. También se comprobó el rechazo de tramas malformadas y la identificación de firmware. |
| Emulador WebAssembly | Chromium arrancó con los tres recursos incluidos, pintó el lienzo RGB de 240 × 240 y respondió al toque en los selectores de género y pieza. La prueba bloqueó las peticiones externas. El test WASM original también se ejecutó correctamente. |
| Paquete auxiliar | El script de reproducción generó una segunda copia idéntica del .fwsc. El instalador original validó su identidad y esquema. Las pruebas simuladas OTA y del instalador pasaron con el candidato. El cargador compilado coincide byte por byte con el original del mismo commit y versión. |
| Suite heredada del firmware | 63 casos correctos y 22 fallos en las mismas pruebas con el commit original y con la variante. Se mantuvieron los resultados originales y no se cambiaron las pruebas de música, menús ni secuenciadores. |
| Suite específica DARK-1 | El original y el candidato obtuvieron 8 pasos correctos y 2 fallos iguales. Las 210 recetas, el render de audio y las pruebas Wander pasaron. Fallaron la comparación de `fm6_dark.h` con el generador y la expectativa estática de encontrar `DREAD BRSS` en el WASM. |
| Tablet Android 16 y FM-1 por USB-C | Pendiente. El entorno de compilación no tiene un sintetizador adjunto. La ejecución del APK en WebView Android y la reconexión USB requieren pruebas en un dispositivo Android. |

## Límites del paquete auxiliar

| Medida | DARK-1 original | Candidato | Límite |
|---|---:|---:|---:|
| Aplicación en flash | 391.788 B | 393.388 B | 581.564 B |
| RAM .data + .bss | 92.632 B | 93.944 B | 98.304 B |
| Pool | 335.320 B | 339.160 B | 344.064 B |
| Tamaño total del paquete | 610.732 B | 610.732 B | — |

Quedan 4.360 B en RAM y 4.904 B en el pool. El proceso original de compilación comprobó el acceso a registros, la ausencia de rutinas de doble precisión y los márgenes del mapa. El firmware conserva el motor original y mantiene la captura en el bucle de interfaz. El límite de envío y la prioridad al MIDI musical se verificaron en el código. La latencia total, la fidelidad de animaciones rápidas, la continuidad de audio y el funcionamiento en las dos revisiones de flash necesitan mediciones en hardware.

## Plan de prueba física

1. Registra modelo y revisión del FM-1, chip flash, firmware, tablet, versión de Android y WebView, cable y hashes de ambos artefactos. Comprueba las copias y la recuperación antes de solicitar autorización para instalar el candidato.
2. Con la tablet en horizontal, abre el modo emulador con Wi-Fi apagado. Comprueba los controles, la selección de género y pieza y el dibujo en la pantalla de 13,4 pulgadas.
3. Conecta el FM-1 en **Modo espejo real**. Observa la identidad, la llegada de segmentos y la reconstrucción de las 240 filas. Usa el escalado nítido y fotografía juntos el LCD del FM-1 y la tablet.
4. Graba una secuencia que incluya girar SELECT y un mando de parámetro, tocar una tecla, pulsar HOME, cambiar de menú, activar un efecto y reproducir un visualizador. Mide en el vídeo el tiempo entre el cambio en el LCD y el primer y último cambio de la tablet. Compara los colores y textos cuando la pantalla está estática. Comprueba el suavizado y la ocultación de los controles.
5. Desconecta y reconecta tres veces con la app abierta. Prueba también volver desde el modo emulador y desde segundo plano. Verifica que se borre la imagen antigua y el enlace se recupere solo.
6. Con una pieza exigente y varios efectos, graba audio y MIDI con y sin espectador y registra saltos de audio, retrasos MIDI y contadores de rendimiento del firmware. Repite el guardado y la recuperación de un proyecto de prueba tras un reinicio y en cada revisión disponible.
7. Conserva la grabación y los registros por modelo y revisión. El criterio de aceptación exige cambios visibles en la app ante los controles físicos, reconexión automática y continuidad del audio durante el espejo.

No se instaló el candidato en un sintetizador. Los fallos del conjunto heredado y la validación física pendiente impiden afirmar compatibilidad verificada.
