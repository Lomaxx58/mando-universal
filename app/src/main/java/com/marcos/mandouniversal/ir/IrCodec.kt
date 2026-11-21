package com.marcos.mandouniversal.ir

/**
 * Señal lista para el emisor: frecuencia portadora (Hz) y patrón de tiempos en
 * microsegundos, alternando encendido/apagado y empezando siempre por encendido.
 */
class IrSignal(val frequency: Int, val pattern: IntArray)

/**
 * Codifica los protocolos de la base de datos Flipper-IRDB con la misma
 * interpretación de dirección/comando que el firmware de Flipper Zero, que es
 * quien capturó los códigos. Todos los bits se envían LSB primero salvo RC5/RC6.
 */
object IrCodec {

    val supportedProtocols = setOf(
        "NEC", "NECext", "NEC42", "NEC42ext", "Samsung32", "RC5", "RC5X", "RC6",
        "SIRC", "SIRC15", "SIRC20", "Kaseikyo", "RCA", "Pioneer"
    )

    /** [toggle] solo afecta a RC5/RC6: debe alternar en cada pulsación nueva. */
    fun encode(protocol: String, address: Long, command: Long, toggle: Boolean = false): IrSignal =
        when (protocol) {
            "NEC" -> {
                val a = (address and 0xFF).toInt(); val c = (command and 0xFF).toInt()
                pdwm(NEC, bitsOf(listOf(a, a.inv(), c, c.inv()), 32))
            }
            "NECext" -> {
                val a = (address and 0xFFFF).toInt(); val c = (command and 0xFFFF).toInt()
                pdwm(NEC, bitsOf(listOf(a, a shr 8, c, c shr 8), 32))
            }
            "NEC42" -> {
                val bits = BitList()
                bits.add(address, 13); bits.add(address.inv(), 13)
                bits.add(command, 8); bits.add(command.inv(), 8)
                pdwm(NEC, bits.toBooleanArray())
            }
            "NEC42ext" -> {
                val bits = BitList()
                bits.add(address, 26); bits.add(command, 16)
                pdwm(NEC, bits.toBooleanArray())
            }
            "Samsung32" -> {
                val a = (address and 0xFF).toInt(); val c = (command and 0xFF).toInt()
                pdwm(SAMSUNG, bitsOf(listOf(a, a, c, c.inv()), 32))
            }
            "RCA" -> {
                val bits = BitList()
                bits.add(address, 4); bits.add(command, 8)
                bits.add(address.inv(), 4); bits.add(command.inv(), 8)
                pdwm(RCA, bits.toBooleanArray())
            }
            "Pioneer" -> {
                val a = (address and 0xFF).toInt(); val c = (command and 0xFF).toInt()
                val bits = BitList()
                for (b in listOf(a, a.inv(), c, c.inv())) bits.add(b.toLong(), 8)
                bits.add(0, 1)
                pdwm(PIONEER, bits.toBooleanArray(), frames = 2, silence = 26000, stopMark = false)
            }
            "Kaseikyo" -> kaseikyo(address, command)
            "SIRC" -> sirc(command, address, 5)
            "SIRC15" -> sirc(command, address, 8)
            "SIRC20" -> sirc(command, address, 13)
            "RC5" -> rc5(address, command, extended = false, toggle = toggle)
            "RC5X" -> rc5(address, command, extended = true, toggle = toggle)
            "RC6" -> rc6(address, command, toggle)
            else -> throw IllegalArgumentException("Protocolo no soportado: $protocol")
        }

    // ---------- Protocolos de ancho de pulso / ancho de silencio ----------

    private class Timing(
        val frequency: Int,
        val preambleMark: Int, val preambleSpace: Int,
        val bit1Mark: Int, val bit1Space: Int,
        val bit0Mark: Int, val bit0Space: Int
    )

    private val NEC = Timing(38000, 9000, 4500, 560, 1690, 560, 560)
    private val SAMSUNG = Timing(38000, 4500, 4500, 550, 1650, 550, 550)
    private val RCA = Timing(38000, 4000, 4000, 500, 2000, 500, 1000)
    private val PIONEER = Timing(40000, 8500, 4225, 500, 1500, 500, 500)
    private const val K = 432
    private val KASEIKYO = Timing(38000, 8 * K, 4 * K, K, 3 * K, K, K)
    private val SIRC_T = Timing(40000, 2400, 600, 1200, 600, 600, 600)

    /** Cabecera, cada bit (marca + silencio) y una marca final de parada (Pioneer no la lleva). */
    private fun pdwm(t: Timing, bits: BooleanArray, frames: Int = 1, silence: Int = 0, stopMark: Boolean = true): IrSignal {
        val p = PulseBuilder()
        repeat(frames) { f ->
            if (f > 0) p.space(silence)
            p.mark(t.preambleMark); p.space(t.preambleSpace)
            for (b in bits) {
                p.mark(if (b) t.bit1Mark else t.bit0Mark)
                p.space(if (b) t.bit1Space else t.bit0Space)
            }
            if (stopMark) p.mark(t.bit0Mark)
        }
        return IrSignal(t.frequency, p.build())
    }

    private fun kaseikyo(address: Long, command: Long): IrSignal {
        val id = ((address shr 24) and 3).toInt()
        val vendor = ((address shr 8) and 0xFFFF).toInt()
        val genre1 = ((address shr 4) and 0xF).toInt()
        val genre2 = (address and 0xF).toInt()
        val cmd = (command and 0xFFFF).toInt()
        val d = IntArray(6)
        d[0] = vendor and 0xFF
        d[1] = (vendor shr 8) and 0xFF
        var parity = d[0] xor d[1]
        parity = (parity and 0xF) xor (parity shr 4)
        d[2] = (parity and 0xF) or (genre1 shl 4)
        d[3] = (genre2 and 0xF) or ((cmd and 0xF) shl 4)
        d[4] = ((id shl 6) or (cmd shr 4)) and 0xFF
        d[5] = d[2] xor d[3] xor d[4]
        return pdwm(KASEIKYO, bitsOf(d.toList(), 48))
    }

    /** Sony: el bit está en la marca (1200 = 1, 600 = 0). Se envía 3 veces cada 45 ms. */
    private fun sirc(command: Long, address: Long, addrBits: Int): IrSignal {
        val bits = BitList()
        bits.add(command, 7); bits.add(address, addrBits)
        val arr = bits.toBooleanArray()
        val t = SIRC_T
        val p = PulseBuilder()
        repeat(3) { f ->
            var sum = 0
            fun mark(us: Int) { p.mark(us); sum += us }
            fun space(us: Int) { p.space(us); sum += us }
            mark(t.preambleMark); space(t.preambleSpace)
            arr.forEachIndexed { i, b ->
                mark(if (b) t.bit1Mark else t.bit0Mark)
                if (i < arr.lastIndex) space(t.bit1Space)
            }
            if (f < 2) p.space(45000 - sum)
        }
        return IrSignal(t.frequency, p.build())
    }

    // ---------- Philips RC5 / RC6 (Manchester) ----------

    /** 36 kHz, medio bit de 889 µs. 1 = silencio→marca, 0 = marca→silencio. MSB primero. */
    private fun rc5(address: Long, command: Long, extended: Boolean, toggle: Boolean): IrSignal {
        val t = 889
        val bits = ArrayList<Int>()
        bits += 1
        bits += if (extended) 0 else 1      // RC5X: el 7º bit del comando va invertido aquí
        bits += if (toggle) 1 else 0
        for (i in 4 downTo 0) bits += ((address shr i) and 1).toInt()
        for (i in 5 downTo 0) bits += ((command shr i) and 1).toInt()
        val p = PulseBuilder()
        for (b in bits) if (b == 1) { p.space(t); p.mark(t) } else { p.mark(t); p.space(t) }
        return IrSignal(36000, p.build())
    }

    /** RC6 modo 0: cabecera 2666/889, bit de inicio, modo 000, toggle doble, 8+8 bits. 1 = marca→silencio. */
    private fun rc6(address: Long, command: Long, toggle: Boolean): IrSignal {
        val t = 444
        val p = PulseBuilder()
        p.mark(2666); p.space(889)
        fun bit(b: Int, w: Int = t) { if (b == 1) { p.mark(w); p.space(w) } else { p.space(w); p.mark(w) } }
        bit(1)
        repeat(3) { bit(0) }
        bit(if (toggle) 1 else 0, 2 * t)
        for (i in 7 downTo 0) bit(((address shr i) and 1).toInt())
        for (i in 7 downTo 0) bit(((command shr i) and 1).toInt())
        return IrSignal(36000, p.build())
    }

    // ---------- Utilidades ----------

    private fun bitsOf(bytes: List<Int>, count: Int): BooleanArray {
        val out = BooleanArray(count)
        for (i in 0 until count) out[i] = (bytes[i / 8] shr (i % 8)) and 1 == 1
        return out
    }

    private class BitList {
        private val list = ArrayList<Boolean>()
        fun add(value: Long, n: Int) { for (i in 0 until n) list += ((value shr i) and 1L) == 1L }
        fun toBooleanArray() = list.toBooleanArray()
    }
}

/**
 * Construye el patrón uniendo tramos consecutivos del mismo tipo (necesario en
 * Manchester) y descartando silencios al principio y al final.
 */
internal class PulseBuilder {
    private val list = ArrayList<Int>()
    private var lastWasMark: Boolean? = null

    fun mark(us: Int) = add(true, us)
    fun space(us: Int) = add(false, us)

    private fun add(isMark: Boolean, us: Int) {
        if (us <= 0) return
        if (list.isEmpty() && !isMark) return
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
