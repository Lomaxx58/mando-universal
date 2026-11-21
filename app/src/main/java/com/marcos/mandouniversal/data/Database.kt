package com.marcos.mandouniversal.data

import android.content.Context
import com.marcos.mandouniversal.ir.IrCodec
import com.marcos.mandouniversal.ir.IrSignal
import org.json.JSONObject

/** Tipos de aparato del menú principal. [file] es el JSON de assets/db. */
enum class Category(val id: String, val title: String, val emoji: String, val file: String) {
    TV("tv", "Televisión", "📺", "tv"),
    AC("ac", "Aire acondicionado", "❄️", "ac"),
    TVBOX("tvbox", "TV Box / Decodificador", "📦", "tvbox"),
    PROJECTOR("projector", "Proyector", "📽️", "projector"),
    AUDIO("audio", "Barra de sonido / Audio", "🔊", "audio"),
    FAN("fan", "Ventilador", "🌀", "fan"),
    HOME("home", "Purificador / Calefactor", "🏠", "home");

    companion object {
        fun byId(id: String?) = entries.firstOrNull { it.id == id }
    }
}

/** Una tecla de un mando: etiqueta original, tecla estándar (o "") y cómo emitirla. */
class Signal(
    val label: String,
    val key: String,
    val protocol: String,
    val address: Long = 0,
    val command: Long = 0,
    val frequency: Int = 0,
    val timings: IntArray? = null
) {
    val isRaw get() = protocol == "RAW"

    fun toIr(toggle: Boolean): IrSignal =
        if (isRaw) IrSignal(frequency, timings!!)
        else IrCodec.encode(protocol, address, command, toggle)

    /** Frecuencia de portadora con la que sale esta tecla. */
    val carrier: Int
        get() = when {
            isRaw -> frequency
            protocol.startsWith("SIRC") || protocol == "Pioneer" -> 40000
            protocol.startsWith("RC") && protocol != "RCA" -> 36000
            else -> 38000
        }
}

class RemoteDef(val model: String, val signals: List<Signal>) {
    private val byKey: Map<String, Signal> = signals.filter { it.key.isNotEmpty() }.associateBy { it.key }

    operator fun get(key: String): Signal? = byKey[key]
    fun has(key: String) = byKey.containsKey(key)

    /** La tecla con la que se prueba si el código funciona. */
    val testSignal: Signal
        get() = this["POWER"] ?: this["POWER_OFF"] ?: this["POWER_ON"] ?: this["VOL_UP"]
            ?: this["MUTE"] ?: signals.first()

    /** Estados completos de aire acondicionado capturados (p. ej. COOL_23). */
    val acStates: Map<String, Map<Int, Signal>> by lazy {
        val re = Regex("^(COOL|HEAT|DRY|AUTO|FAN)_(\\d{2})$")
        signals.mapNotNull { s -> re.find(s.key)?.let { Triple(it.groupValues[1], it.groupValues[2].toInt(), s) } }
            .groupBy({ it.first }, { it.second to it.third })
            .mapValues { (_, v) -> v.toMap().toSortedMap() }
    }
}

class Brand(val name: String, val remotes: List<RemoteDef>)

/** Carga perezosa de la base de datos de códigos (Flipper-IRDB, CC0). */
object CodeDatabase {
    private val cache = HashMap<Category, List<Brand>>()

    /** Marcas que salen primero en la lista, por popularidad en España. */
    val popular: Map<Category, List<String>> = mapOf(
        Category.TV to listOf("Samsung", "LG", "Sony", "Philips", "TCL", "Hisense", "Xiaomi", "Panasonic",
            "Toshiba", "Sharp", "Grundig", "Thomson", "Telefunken", "Haier", "Nevir"),
        Category.AC to listOf("Daikin", "Mitsubishi", "Fujitsu", "LG", "Samsung", "Panasonic", "Gree",
            "Haier", "Midea", "Hisense", "Toshiba", "Carrier"),
        Category.TVBOX to listOf("Xiaomi", "Amazon", "Apple", "Google", "Roku"),
        Category.PROJECTOR to listOf("Epson", "BenQ", "Optoma", "Xiaomi", "Sony", "ViewSonic"),
        Category.AUDIO to listOf("Samsung", "LG", "Sony", "Bose", "JBL", "Yamaha", "Philips"),
    )

    @Synchronized
    fun brands(context: Context, category: Category): List<Brand> =
        cache.getOrPut(category) { load(context, category) }

    fun find(context: Context, category: Category, brand: String): Brand? =
        brands(context, category).firstOrNull { it.name.equals(brand, ignoreCase = true) }

    private fun load(context: Context, category: Category): List<Brand> {
        val text = context.assets.open("db/${category.file}.json").bufferedReader().use { it.readText() }
        return parse(text)
    }

    /** Público para poder probarlo sin Android. */
    fun parse(json: String): List<Brand> {
        val arr = JSONObject(json).getJSONArray("brands")
        return List(arr.length()) { i ->
            val b = arr.getJSONObject(i)
            val remotes = b.getJSONArray("r")
            Brand(b.getString("n"), List(remotes.length()) { j ->
                val r = remotes.getJSONObject(j)
                val sigs = r.getJSONArray("s")
                RemoteDef(r.getString("m"), List(sigs.length()) { k ->
                    val s = sigs.getJSONArray(k)
                    val proto = s.getString(2)
                    if (proto == "RAW") {
                        val t = s.getString(4).split(' ').map { it.toInt() }.toIntArray()
                        Signal(s.getString(0), s.getString(1), proto, frequency = s.getInt(3), timings = t)
                    } else {
                        Signal(s.getString(0), s.getString(1), proto, s.getLong(3), s.getLong(4))
                    }
                })
            })
        }
    }
}
