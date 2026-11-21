package com.marcos.mandouniversal.data

/** Botón especial que solo tienen algunas marcas. */
class ExtraKey(val label: String, val description: String, val signal: Signal)

class StoreGuide(val steps: List<String>, val extras: List<ExtraKey> = emptyList(), val generic: Boolean = false)

/** Pasos orientativos para poner cada marca de TV en modo tienda. */
object StoreGuides {

    private val guides = mapOf(
        "samsung" to StoreGuide(listOf(
            "Pulsa Inicio (o Menú en modelos antiguos) y entra en Ajustes.",
            "Ve a General (en modelos recientes, \"General y privacidad\").",
            "Entra en Administrador del sistema → Modo de uso.",
            "Elige Modo Tienda."
        )),
        "lg" to StoreGuide(listOf(
            "Pulsa el botón de Ajustes (Menú) y abre Todos los ajustes.",
            "Ve a General → Dispositivos → Ajustes de TV (en modelos antiguos está directamente en General).",
            "Entra en Modo Casa/Tienda.",
            "Elige Modo Tienda."
        ), listOf(
            ExtraKey("Demo automática", "Activa la demostración automática (no en todos los modelos).",
                Signal("Demo", "", "NEC", 0x04, 0x80))
        )),
        "sony" to StoreGuide(listOf(
            "Prueba primero el botón \"Modo tienda\" de abajo: en muchos Bravia lo activa directamente.",
            "Si no funciona: Inicio → Ajustes → Sistema (o Preferencias del dispositivo).",
            "Entra en Ajustes del modo Tienda.",
            "Activa Modo demo y/o Modo tienda."
        ), listOf(
            ExtraKey("Modo tienda", "Botón de servicio que abre el modo tienda directamente.",
                Signal("Modo tienda", "", "SIRC15", 0x97, 0x6A))
        )),
        "philips" to StoreGuide(listOf(
            "Pulsa el botón de Ajustes y abre Todos los ajustes.",
            "Ve a Ajustes generales → Ubicación.",
            "Elige Tienda."
        )),
        "panasonic" to StoreGuide(listOf(
            "En la primera configuración, la TV pregunta el entorno de uso: elige Tienda.",
            "Si ya está configurada: Menú → Configuración y busca \"Entorno de visualización\" o \"Modo de uso\".",
            "Si no aparece, suele estar en Menú → Configuración → Configuración inicial."
        )),
        "tcl" to StoreGuide(listOf(
            "Pulsa Inicio y entra en Ajustes.",
            "Ve a Sistema (o Preferencias del dispositivo).",
            "Busca Modo de uso / Modo tienda y elige Tienda."
        )),
        "hisense" to StoreGuide(listOf(
            "Pulsa Ajustes (rueda dentada) o Menú.",
            "Ve a Sistema → Configuración avanzada (o General).",
            "Busca Modo de uso / Modo tienda y elige Tienda."
        )),
    )

    private val generic = StoreGuide(listOf(
        "Pulsa Menú, Ajustes o Inicio.",
        "Busca en Sistema, General o Preferencias una opción llamada Modo de uso, Ubicación, Entorno o Modo tienda.",
        "Elige Tienda (a veces aparece como \"Demo\" o \"Shop\").",
        "Si no aparece, suele elegirse en la configuración inicial al restablecer la TV."
    ), generic = true)

    fun forBrand(brand: String): StoreGuide = guides[brand.lowercase()] ?: generic
}
