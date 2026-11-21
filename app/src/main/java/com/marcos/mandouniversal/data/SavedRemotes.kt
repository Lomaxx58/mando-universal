package com.marcos.mandouniversal.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/** Un mando guardado en "Mis mandos". */
data class SavedRemote(
    val id: String,
    val category: Category,
    val brand: String,
    val model: String,
    val name: String,
    val lastUsed: Long
)

/** Guarda la lista de mandos del usuario en el móvil. */
class SavedRemotesStore(context: Context) {
    private val prefs = context.getSharedPreferences("mis_mandos", Context.MODE_PRIVATE)

    fun all(): List<SavedRemote> {
        val raw = prefs.getString("list", null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.getJSONObject(i)
                val cat = Category.byId(o.getString("c")) ?: return@mapNotNull null
                SavedRemote(o.getString("id"), cat, o.getString("b"), o.getString("m"),
                    o.optString("n", o.getString("b")), o.optLong("t", 0))
            }.sortedByDescending { it.lastUsed }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun get(id: String) = all().firstOrNull { it.id == id }

    /** Crea o sustituye un mando. Si [replaceId] existe, conserva su nombre personalizado. */
    fun save(category: Category, brand: String, model: String, replaceId: String? = null): SavedRemote {
        val list = all().toMutableList()
        val old = replaceId?.let { id -> list.firstOrNull { it.id == id } }
        val keepName = old != null && old.name != old.brand
        val remote = SavedRemote(
            id = old?.id ?: UUID.randomUUID().toString(),
            category = category,
            brand = brand,
            model = model,
            name = if (keepName) old!!.name else brand,
            lastUsed = System.currentTimeMillis()
        )
        list.removeAll { it.id == remote.id }
        list.add(0, remote)
        write(list)
        return remote
    }

    fun rename(id: String, name: String) = update(id) { it.copy(name = name.ifBlank { it.brand }) }

    fun touch(id: String) = update(id) { it.copy(lastUsed = System.currentTimeMillis()) }

    fun delete(id: String) = write(all().filterNot { it.id == id })

    private fun update(id: String, f: (SavedRemote) -> SavedRemote) =
        write(all().map { if (it.id == id) f(it) else it })

    private fun write(list: List<SavedRemote>) {
        val arr = JSONArray()
        list.forEach {
            arr.put(JSONObject().put("id", it.id).put("c", it.category.id).put("b", it.brand)
                .put("m", it.model).put("n", it.name).put("t", it.lastUsed))
        }
        prefs.edit().putString("list", arr.toString()).apply()
    }
}
