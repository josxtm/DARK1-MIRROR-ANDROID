# Actualización más rápida del espejo

El usuario confirmó que la revisión original de BigScreen funcionaba con su FM-1. En la revisión Fast se priorizan los cambios visibles de mandos y menús. El objetivo es reducir el retraso y los datos repetidos.

- **Firmware 1.0.7 Fast.** Captura bandas de hasta doce filas y evita los envíos de regiones de 60 × 4 píxeles que siguen iguales. Une cambios contiguos para conservar el coste original de un barrido completo. Tras detectar movimiento prioriza las etiquetas o los valores, y sigue recorriendo el resto de la pantalla. Mantiene como máximo un mensaje gráfico en el anillo USB y comprueba primero si hay MIDI musical u otro SysEx pendiente.
- **APK comp.2.** Decodifica en el hilo MIDI y solicita un redibujado cuando aparecen píxeles pendientes. Copia al bitmap las filas actualizadas en el siguiente dibujo. El paquete de aplicación y el protocolo de pantalla son compatibles con la revisión original.

## Verificación

- El barrido sintético de 240 × 240 se verificó píxel por píxel con la app. En el segundo barrido idéntico el firmware Fast transmitió **0 bytes**. Un cambio dentro de una región de un mando produjo **12 mensajes y 600 bytes** en el patrón sintético de alto contraste.
- El mínimo teórico de captura del barrido ordinario, sin esperar por el USB ni contar el tiempo de dibujo, pasa de alrededor de 1,9 s a 0,6 s en menús estáticos. Los visualizadores conservan su propio ritmo y suelen cambiar más regiones por barrido. Esta aproximación describe la programación de la captura.
- La compilación del firmware pasó los límites de RAM, pool y tamaño de flash. El paquete auxiliar se comprobó con la prueba simulada del proceso de actualización. En Android pasaron las pruebas JVM, incluida la agrupación de filas.
- El flujo de instalación web se probó con Web MIDI y un FM-1 simulados. Quedan pendientes la latencia de extremo a extremo, la fluidez de los visualizadores y el audio simultáneo con **Fast** en el FM-1 físico.

Para medir el cambio, usa la misma página y pieza en cada versión. Graba a la vez el LCD y la tablet mientras giras un mando y cambias de página. Compara el primer cambio visible y el tiempo hasta que se completa el dibujo, y comprueba el audio con una pieza exigente.
