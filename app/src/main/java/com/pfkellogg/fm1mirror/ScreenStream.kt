package com.pfkellogg.fm1mirror

/** Big Screen records on the MIDI byte stream, which can split a SysEx at any byte. */
internal class ScreenStream(private val onMessage: (ByteArray) -> Unit) {
    private val buffer = ByteArray(256)
    private var length = 0

    fun accept(bytes: ByteArray, offset: Int, count: Int) {
        for (index in offset until offset + count) {
            val byte = bytes[index]
            when (byte.toInt() and 255) {
                0xf0 -> {
                    buffer[0] = byte
                    length = 1
                }
                0xf7 -> {
                    if (length in 1 until buffer.size) {
                        buffer[length] = byte
                        onMessage(buffer.copyOf(length + 1))
                    }
                    length = 0
                }
                in 0xf8..0xff -> Unit
                in 0x80..0xf6 -> length = 0
                else -> if (length in 1 until buffer.size) buffer[length++] = byte
            }
        }
    }

    companion object {
        fun validRow(message: ByteArray): Boolean {
            if (message.size !in 14..70 || (message.size - 10) % 4 != 0 ||
                message[0] != 0xf0.toByte() || message[1] != 0x7d.toByte() ||
                message[2] != 'F'.code.toByte() || message[3] != 'S'.code.toByte() ||
                message[4] != 1.toByte() || message.last() != 0xf7.toByte()) return false
            if ((1 until message.lastIndex).any { message[it] < 0 }) return false
            val row = message[5].toInt() + 128 * message[6]
            var column = message[7].toInt() + 128 * message[8]
            if (row >= Codec.SIZE || column >= Codec.SIZE) return false
            for (index in 9 until message.lastIndex step 4) {
                if (message[index] <= 0 || message[index + 3] > 3) return false
                column += message[index]
            }
            return column <= Codec.SIZE
        }
    }
}
