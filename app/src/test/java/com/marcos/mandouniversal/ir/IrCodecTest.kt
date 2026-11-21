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
