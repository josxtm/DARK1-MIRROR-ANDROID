package com.pfkellogg.fm1mirror

import org.junit.Assert.*
import org.junit.Test
import java.io.File

class ScreenStreamTest {
    @Test fun firmwareSenderToAndroidPixels() {
        val pixels = IntArray(240 * 240)
        var messages = 0
        val stream = ScreenStream {
            assertTrue(ScreenStream.validRow(it))
            assertTrue(Codec.apply(pixels, it))
            messages++
        }
        val wire = File(System.getProperty("dark1.wire")!!).readBytes()
        for (offset in wire.indices step 73) {
            stream.accept(wire, offset, minOf(73, wire.size - offset))
            stream.accept(byteArrayOf(0xf8.toByte()), 0, 1)
        }
        assertTrue(messages > 240)
        for (row in 0 until 240) {
            for (column in 0 until 240) {
                val source = when {
                    column < 16 -> 0x001f
                    column < 32 -> 0xf800
                    else -> ((column - 16) * 877 + row * 37) and 0xffff
                }
                assertEquals("RGB565 pixel ($column,$row)", Codec.argb(source), pixels[row * 240 + column])
            }
        }
        assertEquals(0xffff0000.toInt(), pixels[16])
        assertEquals(0xff0000ff.toInt(), pixels[0])
        assertEquals(0xff00ff00.toInt(), Codec.argb(0x07e0))
    }

    @Test fun malformedMessagesDoNotPaintAndParserRecovers() {
        val valid = byteArrayOf(0xf0.toByte(), 0x7d, 70, 83, 1, 111, 1, 110, 1, 2, 0, 0, 0, 0xf7.toByte())
        assertTrue(ScreenStream.validRow(valid))
        assertFalse(ScreenStream.validRow(valid.clone().also { it[9] = 3 }))
        assertFalse(ScreenStream.validRow(valid.clone().also { it[12] = 4 }))
        assertFalse(ScreenStream.validRow(valid.clone().also { it[5] = 112 }))
        assertFalse(ScreenStream.validRow(valid.copyOf(13)))
        val messages = mutableListOf<ByteArray>()
        val stream = ScreenStream { messages.add(it) }
        val overflow = byteArrayOf(0xf0.toByte()) + ByteArray(600) + byteArrayOf(0xf7.toByte())
        stream.accept(overflow, 0, overflow.size)
        stream.accept(byteArrayOf(0xf0.toByte(), 12, 0x90.toByte(), 0xf7.toByte()), 0, 4)
        stream.accept(valid, 0, valid.size)
        assertEquals(1, messages.size)
        assertArrayEquals(valid, messages.single())
    }

    @Test fun frameBatchesDirtyRowsUntilConsumed() {
        val frame = ScreenFrame()
        fun row(y: Int) = byteArrayOf(
            0xf0.toByte(), 0x7d, 70, 83, 1, (y % 128).toByte(), (y / 128).toByte(),
            0, 0, 1, 0, 112, 3, 0xf7.toByte()
        )

        assertTrue(frame.paint(row(22)))
        assertFalse(frame.paint(row(24)))
        assertEquals(2L, frame.packetCount())
        frame.consume { pixels, top, bottom ->
            assertEquals(22, top)
            assertEquals(25, bottom)
            assertEquals(0xffff0000.toInt(), pixels[22 * 240])
        }
        frame.consume { _, _, _ -> fail("Clean frame copied twice") }
        assertTrue(frame.paint(row(25)))
        frame.reset()
        assertEquals(0L, frame.packetCount())
        frame.consume { pixels, top, bottom ->
            assertEquals(0, top)
            assertEquals(240, bottom)
            assertEquals(0xff000000.toInt(), pixels[25 * 240])
        }
    }

    @Test fun recognizesFirmwareIdentity() {
        val dark = byteArrayOf(0xf0.toByte(), 0x7d, 70, 83, 3) + "DARK-1 1.0.7 BigScreen".toByteArray() + 0xf7.toByte()
        assertEquals("DARK-1 1.0.7 BigScreen", Codec.firmwareInfo(dark))
        assertNull(Codec.firmwareInfo(Codec.HELLO_ALL))
    }
}
