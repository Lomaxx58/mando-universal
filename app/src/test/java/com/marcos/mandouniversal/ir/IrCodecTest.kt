package com.marcos.mandouniversal.ir

import com.marcos.mandouniversal.data.CodeDatabase
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.math.abs

class IrCodecTest {

    /** Normaliza tiempos con signo (negativo = silencio): une tramos iguales y quita silencios de los extremos. */
    private fun normalize(t: List<Int>): List<Int> {
        val out = ArrayList<Int>()
        for (v in t) {
            if (out.isEmpty() && v < 0) continue
            if (out.isNotEmpty() && (out.last() < 0) == (v < 0)) out[out.lastIndex] = out.last() + v else out += v
        }
        while (out.isNotEmpty() && out.last() < 0) out.removeAt(out.lastIndex)
        return out
    }

    private fun signed(pattern: IntArray) = pattern.mapIndexed { i, v -> if (i % 2 == 0) v else -v }

    /**
     * Compara con las señales que genera el propio firmware de Flipper Zero
     * (compilado aparte y guardado en flipper_reference.jsonl).
     */
    @Test
    fun coincideConFlipper() {
        val lines = javaClass.classLoader!!.getResource("flipper_reference.jsonl")!!.readText().lines().filter { it.isNotBlank() }
        assertTrue(lines.size >= 15)
        for (line in lines) {
            val o = JSONObject(line)
            val proto = o.getString("p")
            val arr = o.getJSONArray("t")
            val expected = normalize(List(arr.length()) { arr.getInt(it) })
            // Flipper empieza con el toggle a 0 en RC5/RC6
            val actual = normalize(signed(IrCodec.encode(proto, o.getLong("a"), o.getLong("c"), toggle = false).pattern))
            assertEquals("$proto ${o.getLong("a")}/${o.getLong("c")}: número de tramos", expected.size, actual.size)
            expected.zip(actual).forEachIndexed { i, (e, a) ->
                assertTrue("$proto tramo $i: esperado $e, obtenido $a", abs(e - a) <= maxOf(3, abs(e) / 50))
            }
        }
    }

    /** Todas las teclas de la base de datos se pueden emitir. */
    @Test
    fun todaLaBaseDeDatosEsValida() {
        val dir = File("src/main/assets/db")
        val files = dir.listFiles { f -> f.extension == "json" }!!
        assertTrue(files.size >= 7)
        var count = 0
        for (f in files) {
            for (brand in CodeDatabase.parse(f.readText())) {
                assertTrue(brand.remotes.isNotEmpty())
                for (remote in brand.remotes) for (s in remote.signals) {
                    val p = s.toIr(false).pattern
                    assertTrue("${f.name} ${brand.name} ${s.label} vacío", p.isNotEmpty())
                    assertTrue("${f.name} ${brand.name} ${s.label} tramos <= 0", p.all { it > 0 })
                    assertTrue("${f.name} ${brand.name} ${s.label} > 2 s", p.sum() < 2_000_000)
                    count++
                }
            }
        }
        assertTrue(count > 10000)
    }
}

class AcCodebookTest {
    /** Todos los estados de todos los aires con mando completo se pueden emitir. */
    @Test
    fun todosLosEstadosSonValidos() {
        val files = File("src/main/assets/db/acp").listFiles { f -> f.extension == "json" }!!
        assertTrue(files.size >= 40)
        for (f in files) {
            val book = com.marcos.mandouniversal.data.AcProtocols.Codebook.parse(f.readText())
            assertEquals("${f.name}: número de estados", 3 * 5 * 15 * 4 * 2, book.size)
            for (i in 0 until book.size) {
                val s = book.signal(i)
                assertTrue("${f.name} $i vacío", s.pattern.isNotEmpty() && s.pattern.size % 2 == 1)
                assertTrue("${f.name} $i tramos <= 0", s.pattern.all { it > 0 })
                assertTrue("${f.name} $i > 2 s", s.pattern.sum() < 2_000_000)
                assertTrue("${f.name} frecuencia", s.frequency in 30000..60000)
            }
        }
    }

    @Test
    fun indiceDeEstado() {
        val ac = com.marcos.mandouniversal.data.AcProtocols
        assertEquals(0, ac.index(0, "COOL", 16, "AUTO", false))
        assertEquals(1799, ac.index(2, "AUTO", 30, "HIGH", true))
    }
}
