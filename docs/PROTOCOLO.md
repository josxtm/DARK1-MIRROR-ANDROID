# USB-MIDI Big Screen para DARK-1

La aplicación abre el puerto de entrada y salida 0 ofrecido por Android para el dispositivo USB-MIDI. Android entrega los bytes MIDI 1.0. Cada mensaje de sistema exclusivo empieza por `F0` y termina por `F7`. Los bytes entre ambos delimitadores tienen siete bits. La app admite mensajes fragmentados entre lecturas y relojes MIDI intercalados.

| Sentido | Mensaje hexadecimal | Uso |
|---|---|---|
| App → FM-1 | `F0 7D 46 53 01 F7` | Iniciar y solicitar la pantalla completa |
| App → FM-1 | `F0 7D 46 53 00 F7` | Mantener la transmisión activa |
| App → FM-1 | `F0 7D 46 4C 01 F7` | Consulta de identidad de la familia Felucca |
| FM-1 → app | `F0 7D 46 53 01 yy YY xx XX [n c0 c1 c2]… F7` | Segmento gráfico |
| FM-1 → app | `F0 7D 46 53 03 [ASCII] F7` | Identidad de la variante DARK-1 BigScreen |

`7D` es el identificador para uso no comercial. `46 53` son los caracteres ASCII **FS**. Las coordenadas de la fila y la columna inicial son `yy + 128 × YY` y `xx + 128 × XX`. El origen está arriba a la izquierda y cada coordenada válida va de 0 a 239. Cada repetición tiene de 1 a 127 píxeles del color RGB565 `c0 + 128 × c1 + 16384 × c2`. El último componente va de 0 a 3. El límite de cada segmento es 70 bytes. La app descarta segmentos fuera de los límites y amplía los colores a ARGB opaco.

La app envía un saludo por segundo y pasa a mantener la sesión tras el primer segmento válido. La variante devuelve su identidad ASCII al recibir un saludo y vence a los tres segundos de inactividad. Al cerrarse el puerto, la app descarta la imagen anterior y busca un nuevo puerto compatible.

El transmisor obtiene los píxeles de llamadas `lcd_fill` y `lcd_blit`. El LCD utiliza RGB565 en orden de bytes de red y el transmisor convierte ese orden antes de codificar. La captura reconstruye bandas de ocho filas para ajustarse a la RAM disponible. Vuelve a recorrer la pantalla mientras se reciben saludos, renueva la sesión desde arriba al pedir pantalla completa y difiere el envío cuando hay MIDI musical u otro SysEx pendiente. La salida MIDI se limita a un segmento por milisegundo como máximo.

Los segmentos no incluyen un número de fotograma. Cada banda puede contener un instante diferente. El receptor aplica las actualizaciones según llegan. En el candidato la captura empieza cuando el firmware dibuja su interfaz principal. Las pantallas de arranque, de asistencia o de actualización quedan fuera de la sincronización inicial comprobada. [Formato Big Screen](https://github.com/pfkellogg/sloop-fm1/blob/170b844df43ba19ac001aa0cbeef77fe4b8fa6e7/BIG_SCREEN.md)
