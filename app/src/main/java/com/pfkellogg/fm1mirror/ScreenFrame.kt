package com.pfkellogg.fm1mirror

/** The MIDI thread writes pixels. The UI copies only the dirty rectangle on its next frame. */
internal class ScreenFrame {
    private val pixels = IntArray(Codec.SIZE * Codec.SIZE) { BLACK }
    private var top = Codec.SIZE
    private var bottom = 0
    private var packets = 0L

    @Synchronized
    fun paint(message: ByteArray): Boolean {
        val wasClean = top == Codec.SIZE
        val row = message[5].toInt() + 128 * message[6]
        Codec.apply(pixels, message)
        top = minOf(top, row)
        bottom = maxOf(bottom, row + 1)
        packets++
        return wasClean
    }

    @Synchronized
    fun consume(copy: (IntArray, Int, Int) -> Unit) {
        if (top == Codec.SIZE) return
        copy(pixels, top, bottom)
        top = Codec.SIZE
        bottom = 0
    }

    @Synchronized
    fun reset() {
        pixels.fill(BLACK)
        top = 0
        bottom = Codec.SIZE
        packets = 0
    }

    @Synchronized
    fun packetCount(): Long = packets

    private companion object {
        const val BLACK = 0xff000000.toInt()
    }
}
