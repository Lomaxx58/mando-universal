package com.marcos.mandouniversal.ir

/**
 * Señal lista para el emisor: frecuencia portadora (Hz) y patrón de tiempos en
 * microsegundos, alternando encendido/apagado y empezando siempre por encendido.
 */
class IrSignal(val frequency: Int, val pattern: IntArray)

/** Protocolos infrarrojos que usan las marcas de TV incluidas. */
enum class Protocol {
    NEC1,       // LG
    NECX2,      // Samsung
    SONY12,     // Sony (12 bits)
    SONY15,     // Sony (15 bits)
    RC5,        // Philips antiguas
    RC6,        // Philips modernas
    PANASONIC,  // Panasonic (Kaseikyo)
    RCA38       // TCL (Roku TV)
}

/**
 * Un comando en la notación de la base de datos IRDB:
 * protocolo, dispositivo, subdispositivo (-1 si no tiene) y función.
 */
data class IrCommand(
    val protocol: Protocol,
    val device: Int,
    val subdevice: Int,
    val function: Int
)

/** Convierte comandos IRDB en patrones de pulsos para ConsumerIrManager. */
object IrCodec {

    /** [toggle] solo afecta a RC5/RC6: debe alternar en cada pulsación nueva. */
    fun encode(cmd: IrCommand, toggle: Boolean = false): IrSignal = when (cmd.protocol) {
        Protocol.NEC1 -> nec(cmd.device, cmd.subdevice, cmd.function, 9000, 4500)
        Protocol.NECX2 -> nec(cmd.device, cmd.subdevice, cmd.function, 4500, 4500)
        Protocol.SONY12 -> sony(12, cmd.device, cmd.function)
        Protocol.SONY15 -> sony(15, cmd.device, cmd.function)
        Protocol.RC5 -> rc5(cmd.device, cmd.function, toggle)
        Protocol.RC6 -> rc6(cmd.device, cmd.function, toggle)
        Protocol.PANASONIC -> panasonic(cmd.device, cmd.subdevice, cmd.function)
        Protocol.RCA38 -> rca38(cmd.device, cmd.function)
    }

    // ---------- NEC (LG) y NECx2 (Samsung) ----------
    // Bits LSB primero. 0 = 564 on / 564 off, 1 = 564 on / 1692 off.
    private fun nec(device: Int, subdevice: Int, function: Int, leadOn: Int, leadOff: Int): IrSignal {
        val p = PulseBuilder()
        p.mark(leadOn); p.space(leadOff)
        val sub = if (subdevice < 0) device.inv() else subdevice
        val bytes = intArrayOf(device, sub, function, function.inv())
        for (byte in bytes) {
            for (i in 0 until 8) {
                p.mark(564)
                p.space(if ((byte shr i) and 1 == 1) 1692 else 564)
            }
        }
        p.mark(564)
        return IrSignal(38000, p.build())
    }

    // ---------- Sony SIRC ----------
    // 40 kHz. Cabecera 2400 on / 600 off. Bit 1 = 1200 on, bit 0 = 600 on, separados por 600 off.
    // 7 bits de comando + (bits-7) de dispositivo, LSB primero. Se repite 3 veces cada 45 ms.
    private fun sony(bits: Int, device: Int, function: Int): IrSignal {
        val devBits = bits - 7
        val data = (function and 0x7F) or ((device and ((1 shl devBits) - 1)) shl 7)
        val p = PulseBuilder()
        repeat(3) {
            var frame = 0
            p.mark(2400); p.space(600); frame += 3000
            for (i in 0 until bits) {
                val on = if ((data shr i) and 1 == 1) 1200 else 600
                p.mark(on); frame += on
                if (i < bits - 1) { p.space(600); frame += 600 }
            }
            p.space(45000 - frame)
        }
        return IrSignal(40000, p.build())
    }

    // ---------- Philips RC5 ----------
    // 36 kHz, codificación Manchester de 889 µs. 1 = off/on, 0 = on/off. MSB primero.
    private fun rc5(device: Int, function: Int, toggle: Boolean): IrSignal {
        val t = 889
        val p = PulseBuilder()
        val bits = mutableListOf<Int>()
        bits += 1                                   // bit de inicio
        bits += if (function < 64) 1 else 0         // segundo bit (RC5X: 7º bit del comando invertido)
        bits += if (toggle) 1 else 0
        for (i in 4 downTo 0) bits += (device shr i) and 1
        for (i in 5 downTo 0) bits += (function shr i) and 1
        for (b in bits) {
            if (b == 1) { p.space(t); p.mark(t) } else { p.mark(t); p.space(t) }
        }
        return IrSignal(36000, p.build())
    }

    // ---------- Philips RC6 modo 0 ----------
    // 36 kHz, unidad 444 µs. Cabecera 6t on / 2t off, bit de inicio, 3 bits de modo,
    // bit toggle de doble anchura, 8 bits de dirección y 8 de comando (MSB primero).
    // En RC6: 1 = on/off, 0 = off/on.
    private fun rc6(device: Int, function: Int, toggle: Boolean): IrSignal {
        val t = 444
        val p = PulseBuilder()
        p.mark(6 * t); p.space(2 * t)
        fun bit(b: Int, w: Int = t) { if (b == 1) { p.mark(w); p.space(w) } else { p.space(w); p.mark(w) } }
        bit(1)                       // inicio
        repeat(3) { bit(0) }         // modo 000
        bit(if (toggle) 1 else 0, 2 * t)
        for (i in 7 downTo 0) bit((device shr i) and 1)
        for (i in 7 downTo 0) bit((function shr i) and 1)
        return IrSignal(36000, p.build())
    }

    // ---------- Panasonic (Kaseikyo) ----------
    // 37 kHz, unidad 432 µs. Cabecera 8t on / 4t off. 0 = t/t, 1 = t/3t. 48 bits LSB primero:
    // 0x02, 0x20 (fabricante), dispositivo, subdispositivo, función, XOR de los tres anteriores.
    private fun panasonic(device: Int, subdevice: Int, function: Int): IrSignal {
        val t = 432
        val sub = if (subdevice < 0) 0 else subdevice
        val bytes = intArrayOf(0x02, 0x20, device, sub, function, (device xor sub xor function))
        val p = PulseBuilder()
        p.mark(8 * t); p.space(4 * t)
        for (byte in bytes) {
            for (i in 0 until 8) {
                p.mark(t)
                p.space(if ((byte shr i) and 1 == 1) 3 * t else t)
            }
        }
        p.mark(t)
        return IrSignal(37000, p.build())
    }

    // ---------- RCA a 38 kHz (TCL Roku TV) ----------
    // Unidad 460 µs. Cabecera 8t/8t. 0 = t/2t, 1 = t/4t. MSB primero:
    // dirección (4 bits), comando (8), dirección invertida (4), comando invertido (8). Se envía 2 veces.
    private fun rca38(device: Int, function: Int): IrSignal {
        val t = 460
        val p = PulseBuilder()
        fun field(value: Int, n: Int) {
            for (i in n - 1 downTo 0) {
                p.mark(t)
                p.space(if ((value shr i) and 1 == 1) 4 * t else 2 * t)
            }
        }
        repeat(2) {
            p.mark(8 * t); p.space(8 * t)
            field(device, 4); field(function, 8)
            field(device.inv(), 4); field(function.inv(), 8)
            p.mark(t); p.space(16 * t)
        }
        return IrSignal(38000, p.build())
    }
}

/**
 * Construye el patrón uniendo tramos consecutivos del mismo tipo (necesario en
 * Manchester) y descartando silencios al principio y al final.
 */
private class PulseBuilder {
    private val list = ArrayList<Int>()
    private var lastWasMark: Boolean? = null

    fun mark(us: Int) = add(true, us)
    fun space(us: Int) = add(false, us)

    private fun add(isMark: Boolean, us: Int) {
        if (us <= 0) return
        if (list.isEmpty() && !isMark) return          // el patrón debe empezar encendido
        if (lastWasMark == isMark) {
            list[list.lastIndex] = list.last() + us
        } else {
            list.add(us)
            lastWasMark = isMark
        }
    }

    fun build(): IntArray {
        if (lastWasMark == false) list.removeAt(list.lastIndex)
        return list.toIntArray()
    }
}
