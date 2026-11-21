package com.pfkellogg.fm1mirror

import android.content.Context
import android.media.midi.MidiDevice
import android.media.midi.MidiDeviceInfo
import android.media.midi.MidiInputPort
import android.media.midi.MidiManager
import android.media.midi.MidiOutputPort
import android.media.midi.MidiReceiver
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import java.io.IOException

/** Owns Android's USB MIDI byte-stream ports on a background looper. */
internal class MirrorMidi(
    context: Context,
    private val onState: (String) -> Unit,
    val frame: ScreenFrame,
    private val onFrame: () -> Unit,
    private val onFirmware: (String?) -> Unit
) {
    private val manager = context.getSystemService(MidiManager::class.java)
    private val thread = HandlerThread("DARK-1 USB MIDI").apply { start() }
    private val handler = Handler(thread.looper)
    private var active = false
    private var generation = 0
    private var selected: MidiDeviceInfo? = null
    private var device: MidiDevice? = null
    private var input: MidiInputPort? = null
    private var output: MidiOutputPort? = null
    private var receivedRows = false
    private var firmware: String? = null

    private val tick = object : Runnable {
        override fun run() {
            if (!active) return
            val ports = byteStreamDevices().filter {
                it.type == MidiDeviceInfo.TYPE_USB && it.inputPortCount > 0 && it.outputPortCount > 0
            }
            if (selected != null && ports.none { it.id == selected?.id }) disconnect()
            if (selected == null) {
                val preferred = ports.filter { info ->
                    val label = deviceName(info)
                    listOf("DARK-1", "FM-1", "FM1", "JieLi", "Felucca").any {
                        label.contains(it, ignoreCase = true)
                    }
                }
                val candidate = preferred.singleOrNull() ?: ports.singleOrNull()
                if (candidate != null) open(candidate)
                else onState(if (manager == null) "Servicio MIDI no disponible"
                    else if (ports.isEmpty()) "Esperando FM-1 por USB-C"
                    else "Hay varios dispositivos USB-MIDI. Deja conectado solo el FM-1.")
            } else if (input != null) {
                try {
                    input?.send(if (receivedRows) Codec.HELLO else Codec.HELLO_ALL, 0, 6)
                    input?.send(Codec.ASK_INFO, 0, Codec.ASK_INFO.size)
                } catch (_: IOException) {
                    disconnect()
                }
            }
            handler.postDelayed(this, 1000)
        }
    }

    fun setActive(enabled: Boolean) = handler.post {
        if (active == enabled) return@post
        active = enabled
        handler.removeCallbacks(tick)
        if (active) tick.run() else disconnect()
    }

    fun close() {
        handler.post {
            active = false
            handler.removeCallbacks(tick)
            disconnect()
            thread.quitSafely()
        }
    }

    private fun open(info: MidiDeviceInfo) {
        selected = info
        val attempt = ++generation
        onState("Abriendo ${deviceName(info)}")
        manager?.openDevice(info, { opened ->
            if (!active || attempt != generation) {
                opened?.close()
            } else {
                device = opened
                input = opened?.openInputPort(0)
                output = opened?.openOutputPort(0)
                if (input == null || output == null) {
                    disconnect()
                    onState("Puerto MIDI ocupado o no disponible")
                } else {
                    val stream = ScreenStream { message ->
                        if (attempt == generation) {
                            receive(message)
                        }
                    }
                    output?.connect(object : MidiReceiver() {
                        override fun onSend(data: ByteArray, offset: Int, count: Int, timestamp: Long) {
                            val bytes = data.copyOfRange(offset, offset + count)
                            handler.post { if (attempt == generation) stream.accept(bytes, 0, bytes.size) }
                        }
                    })
                    onState("USB-MIDI conectado · esperando píxeles")
                }
            }
        }, handler)
        handler.postDelayed({ if (attempt == generation && input == null) disconnect() }, 3000)
    }

    private fun receive(message: ByteArray) {
        val reported = Codec.firmwareInfo(message)?.let { Codec.firmwareName(it, null) }
        if (reported != null && reported != firmware) {
            firmware = reported
            onFirmware(reported)
            onState("${reported} · " + if (receivedRows) "recibiendo pantalla" else "esperando píxeles")
        }
        if (!ScreenStream.validRow(message)) return
        val notify = frame.paint(message)
        if (!receivedRows) {
            receivedRows = true
            onState("${firmware ?: "USB-MIDI"} · recibiendo pantalla")
        }
        if (notify) onFrame()
    }

    private fun disconnect() {
        generation++
        selected = null
        listOf(input, output, device).forEach {
            try { it?.close() } catch (_: IOException) { }
        }
        input = null
        output = null
        device = null
        receivedRows = false
        firmware = null
        frame.reset()
        onFirmware(null)
        onFrame()
        onState("Esperando FM-1 por USB-C")
    }

    @Suppress("DEPRECATION")
    private fun byteStreamDevices(): Collection<MidiDeviceInfo> {
        val midi = manager ?: return emptyList()
        return if (Build.VERSION.SDK_INT >= 33) {
            midi.getDevicesForTransport(MidiManager.TRANSPORT_MIDI_BYTE_STREAM)
        } else {
            midi.devices.asList()
        }
    }

    private fun deviceName(info: MidiDeviceInfo): String = listOfNotNull(
        info.properties.getString(MidiDeviceInfo.PROPERTY_MANUFACTURER),
        info.properties.getString(MidiDeviceInfo.PROPERTY_NAME),
        info.properties.getString(MidiDeviceInfo.PROPERTY_PRODUCT)
    ).distinct().joinToString(" ").ifBlank { "USB-MIDI" }
}
