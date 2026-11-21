package com.marcos.mandouniversal.data

import android.content.Context
import com.marcos.mandouniversal.ir.IrSignal
import org.json.JSONObject

/**
 * Aires acondicionados con "mando completo": la app guarda el estado (modo,
 * grados, ventilador, oscilación) y en cada pulsación envía la orden entera,
 * igual que el mando original. Las señales de cada estado se generaron con la
 * librería IRremoteESP8266 (tools/acgen) y están en assets/db/acp.
 */
object AcProtocols {

    val MODES = listOf("COOL", "HEAT", "DRY", "FAN", "AUTO")
    val FANS = listOf("AUTO", "LOW", "MEDIUM", "HIGH")

    class Info(
        val id: String,
        val file: String,
        val label: String,
        val modes: List<String>,
        val temps: Map<String, IntRange?>,
        val fans: List<String>,
        val swing: Boolean,
        val frequency: Int
    )

    private var appContext: Context? = null
    private var infos: Map<String, Info>? = null
    private var brandMap: Map<String, List<String>>? = null
    private val books = HashMap<String, Codebook>()

    fun init(context: Context) { appContext = context.applicationContext }

    @Synchronized
    private fun ensure() {
        if (infos != null) return
        val text = appContext!!.assets.open("db/ac_protocols.json").bufferedReader().use { it.readText() }
        val root = JSONObject(text)
        val ps = root.getJSONObject("protocols")
        infos = ps.keys().asSequence().associateWith { id ->
            val o = ps.getJSONObject(id)
            val t = o.getJSONObject("temps")
            Info(
                id = id,
                file = o.getString("file"),
                label = o.getString("label"),
                modes = o.getJSONArray("modes").let { a -> List(a.length()) { a.getString(it) } },
                temps = t.keys().asSequence().associateWith { k ->
                    if (t.isNull(k)) null else t.getJSONArray(k).let { it.getInt(0)..it.getInt(1) }
                },
                fans = o.getJSONArray("fans").let { a -> List(a.length()) { a.getString(it) } },
                swing = o.getBoolean("swing"),
                frequency = o.getInt("freq")
            )
        }
        val bs = root.getJSONObject("brands")
        brandMap = bs.keys().asSequence().associateWith { b ->
            bs.getJSONArray(b).let { a -> List(a.length()) { a.getString(it) } }
        }
    }

    fun info(id: String): Info? { ensure(); return infos!![id] }

    /** Marca (tal como se enseña) -> protocolos a probar. */
    fun brands(): Map<String, List<String>> { ensure(); return brandMap!! }

    /** Posición de un estado dentro del fichero. [p]: 0 = encendido, 1 = apagar, 2 = encender. */
    fun index(p: Int, mode: String, temp: Int, fan: String, swing: Boolean): Int {
        val m = MODES.indexOf(mode).coerceAtLeast(0)
        val t = temp.coerceIn(16, 30) - 16
        val f = FANS.indexOf(fan).coerceAtLeast(0)
        return (((p * 5 + m) * 15 + t) * 4 + f) * 2 + (if (swing) 1 else 0)
    }

    /** Tecla de prueba: encender en frío a 24 °C. */
    fun testSignal(info: Info): Signal =
        Signal("Encender (frío 24°)", "POWER", "ACP", frequency = info.frequency, acFile = info.file,
            acIndex = index(2, info.modes.first(), 24, "AUTO", false))

    @Synchronized
    fun codebook(file: String): Codebook = books.getOrPut(file) {
        val text = appContext!!.assets.open("db/acp/$file.json").bufferedReader().use { it.readText() }
        Codebook.parse(text)
    }

    /** Señales de todos los estados de un protocolo. */
    class Codebook(private val frequency: Int, private val alphabet: IntArray, private val signals: List<String>, private val index: IntArray) {
        val size get() = index.size

        fun signal(i: Int): IrSignal {
            val s = signals[index[i]]
            val pattern = IntArray(s.length) { kotlin.math.abs(alphabet[s[it] - '#']) }
            return IrSignal(frequency, pattern)
        }

        companion object {
            fun parse(json: String): Codebook {
                val o = JSONObject(json)
                val a = o.getJSONArray("a")
                val s = o.getJSONArray("s")
                val i = o.getJSONArray("i")
                return Codebook(
                    o.getInt("f"),
                    IntArray(a.length()) { a.getInt(it) },
                    List(s.length()) { s.getString(it) },
                    IntArray(i.length()) { i.getInt(it) }
                )
            }
        }
    }
}

/** Estado de un aire con mando completo, guardado por mando. */
data class AcState(val power: Boolean, val mode: String, val temp: Int, val fan: String, val swing: Boolean)

class AcStateStore(context: Context) {
    private val prefs = context.getSharedPreferences("aires", Context.MODE_PRIVATE)

    fun load(id: String, info: AcProtocols.Info): AcState {
        val mode = prefs.getString("$id.mode", null)?.takeIf { it in info.modes } ?: info.modes.first()
        val range = info.temps[mode]
        return AcState(
            power = prefs.getBoolean("$id.power", false),
            mode = mode,
            temp = prefs.getInt("$id.temp", 24).let { t -> range?.let { t.coerceIn(it) } ?: t },
            fan = prefs.getString("$id.fan", null)?.takeIf { it in info.fans } ?: info.fans.first(),
            swing = info.swing && prefs.getBoolean("$id.swing", false)
        )
    }

    fun save(id: String, s: AcState) {
        prefs.edit().putBoolean("$id.power", s.power).putString("$id.mode", s.mode).putInt("$id.temp", s.temp)
            .putString("$id.fan", s.fan).putBoolean("$id.swing", s.swing).apply()
    }
}
