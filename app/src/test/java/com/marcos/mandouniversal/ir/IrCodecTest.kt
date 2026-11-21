package com.marcos.mandouniversal.ir

import com.marcos.mandouniversal.data.Brands
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IrCodecTest {

    @Test
    fun necTieneCabeceraY32Bits() {
        val s = IrCodec.encode(IrCommand(Protocol.NEC1, 4, -1, 8)) // LG power
        assertEquals(38000, s.frequency)
        assertEquals(9000, s.pattern[0])
        assertEquals(4500, s.pattern[1])
        assertEquals(2 + 32 * 2 + 1, s.pattern.size)
    }

    @Test
    fun samsungUsaCabecera4500() {
        val s = IrCodec.encode(IrCommand(Protocol.NECX2, 7, 7, 2))
        assertEquals(4500, s.pattern[0])
        assertEquals(4500, s.pattern[1])
    }

    @Test
    fun sonyVaA40kHzYRepiteTresVeces() {
        val s = IrCodec.encode(IrCommand(Protocol.SONY12, 1, -1, 21))
        assertEquals(40000, s.frequency)
        assertEquals(3, s.pattern.count { it == 2400 })
    }

    @Test
    fun rc6EmpiezaConCabeceraLarga() {
        val s = IrCodec.encode(IrCommand(Protocol.RC6, 0, -1, 0x0C), toggle = true)
        assertEquals(36000, s.frequency)
        assertEquals(2664, s.pattern[0])
    }

    @Test
    fun todosLosCodigosGeneranPatronesValidos() {
        for (brand in Brands.all) for (set in brand.codeSets) {
            val cmds = set.codes.values + set.extras.map { it.command }
            for (cmd in cmds) {
                val p = IrCodec.encode(cmd).pattern
                assertTrue("$cmd vacío", p.isNotEmpty())
                assertTrue("$cmd debe acabar encendido", p.size % 2 == 1)
                assertTrue("$cmd duración > 2 s", p.sum() < 2_000_000)
                assertTrue("$cmd tiene tramos <= 0", p.all { it > 0 })
            }
        }
    }
}
