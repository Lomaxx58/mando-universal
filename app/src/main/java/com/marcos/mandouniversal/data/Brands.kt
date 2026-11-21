package com.marcos.mandouniversal.data

import com.marcos.mandouniversal.ir.IrCommand
import com.marcos.mandouniversal.ir.Protocol

/** Botones del mando. */
enum class RemoteKey {
    POWER, SOURCE, MUTE,
    VOL_UP, VOL_DOWN, CH_UP, CH_DOWN,
    UP, DOWN, LEFT, RIGHT, OK,
    BACK, HOME, MENU, EXIT, INFO,
    D0, D1, D2, D3, D4, D5, D6, D7, D8, D9
}

/** Botón especial que solo tienen algunas marcas (p. ej. "Modo tienda" en Sony). */
data class ExtraKey(val label: String, val description: String, val command: IrCommand)

/** Juego de códigos. Una marca puede tener varios según la época de sus modelos. */
data class CodeSet(
    val name: String,
    val codes: Map<RemoteKey, IrCommand>,
    val extras: List<ExtraKey> = emptyList()
)

data class Brand(
    val id: String,
    val name: String,
    val codeSets: List<CodeSet>,
    val storeModeSteps: List<String>
)

private fun digits(protocol: Protocol, dev: Int, sub: Int, values: List<Int>): Map<RemoteKey, IrCommand> {
    val keys = listOf(RemoteKey.D0, RemoteKey.D1, RemoteKey.D2, RemoteKey.D3, RemoteKey.D4,
        RemoteKey.D5, RemoteKey.D6, RemoteKey.D7, RemoteKey.D8, RemoteKey.D9)
    return keys.zip(values).associate { (k, v) -> k to IrCommand(protocol, dev, sub, v) }
}

private fun codes(protocol: Protocol, dev: Int, sub: Int, vararg pairs: Pair<RemoteKey, Int>) =
    pairs.associate { (k, f) -> k to IrCommand(protocol, dev, sub, f) }

/**
 * Códigos tomados de IRDB (github.com/probonopd/irdb) salvo donde se indica.
 * Los dígitos se listan en orden 0,1,2...9.
 */
object Brands {

    private val samsung = Brand(
        id = "samsung",
        name = "Samsung",
        codeSets = listOf(
            CodeSet(
                name = "Samsung (todas)",
                codes = codes(Protocol.NECX2, 7, 7,
                    RemoteKey.POWER to 2, RemoteKey.SOURCE to 1, RemoteKey.MUTE to 15,
                    RemoteKey.VOL_UP to 7, RemoteKey.VOL_DOWN to 11,
                    RemoteKey.CH_UP to 18, RemoteKey.CH_DOWN to 16,
                    RemoteKey.UP to 96, RemoteKey.DOWN to 97, RemoteKey.LEFT to 101, RemoteKey.RIGHT to 98,
                    RemoteKey.OK to 104, RemoteKey.BACK to 88, RemoteKey.HOME to 121,
                    RemoteKey.MENU to 26, RemoteKey.EXIT to 45, RemoteKey.INFO to 31
                ) + digits(Protocol.NECX2, 7, 7, listOf(17, 4, 5, 6, 8, 9, 10, 12, 13, 14))
            )
        ),
        storeModeSteps = listOf(
            "Pulsa HOME (o MENÚ en modelos antiguos) y entra en Ajustes.",
            "Ve a General (en modelos recientes, \"General y privacidad\").",
            "Entra en Administrador del sistema → Modo de uso.",
            "Elige Modo Tienda."
        )
    )

    private val lg = Brand(
        id = "lg",
        name = "LG",
        codeSets = listOf(
            CodeSet(
                name = "LG (todas)",
                codes = codes(Protocol.NEC1, 4, -1,
                    RemoteKey.POWER to 8, RemoteKey.SOURCE to 11, RemoteKey.MUTE to 9,
                    RemoteKey.VOL_UP to 2, RemoteKey.VOL_DOWN to 3,
                    RemoteKey.CH_UP to 0, RemoteKey.CH_DOWN to 1,
                    RemoteKey.UP to 64, RemoteKey.DOWN to 65, RemoteKey.LEFT to 7, RemoteKey.RIGHT to 6,
                    RemoteKey.OK to 68, RemoteKey.BACK to 40,
                    RemoteKey.HOME to 124, // botón Home de webOS (0x7C)
                    RemoteKey.MENU to 67, RemoteKey.EXIT to 91, RemoteKey.INFO to 170
                ) + digits(Protocol.NEC1, 4, -1, (16..25).toList()),
                extras = listOf(
                    ExtraKey("Demo automática", "Activa la demostración automática (no en todos los modelos).",
                        IrCommand(Protocol.NEC1, 4, -1, 128))
                )
            )
        ),
        storeModeSteps = listOf(
            "Pulsa el botón de Ajustes (MENÚ) y abre Todos los ajustes.",
            "Ve a General → Dispositivos → Ajustes de TV (en modelos antiguos está directamente en General).",
            "Entra en Modo Casa/Tienda.",
            "Elige Modo Tienda."
        )
    )

    private val sony = Brand(
        id = "sony",
        name = "Sony",
        codeSets = listOf(
            CodeSet(
                name = "Sony Bravia",
                codes = codes(Protocol.SONY12, 1, -1,
                    RemoteKey.POWER to 21, RemoteKey.SOURCE to 37, RemoteKey.MUTE to 20,
                    RemoteKey.VOL_UP to 18, RemoteKey.VOL_DOWN to 19,
                    RemoteKey.CH_UP to 16, RemoteKey.CH_DOWN to 17,
                    RemoteKey.UP to 116, RemoteKey.DOWN to 117, RemoteKey.LEFT to 52, RemoteKey.RIGHT to 51,
                    RemoteKey.OK to 101, RemoteKey.HOME to 96, RemoteKey.MENU to 96,
                    RemoteKey.EXIT to 99, RemoteKey.INFO to 58
                ) + codes(Protocol.SONY15, 151, -1,
                    RemoteKey.BACK to 35 // "Return" de Bravia (código IRCC conocido, no está en IRDB)
                ) + digits(Protocol.SONY12, 1, -1, listOf(9, 0, 1, 2, 3, 4, 5, 6, 7, 8)),
                extras = listOf(
                    ExtraKey("Modo tienda", "Botón de servicio que abre el modo tienda directamente.",
                        IrCommand(Protocol.SONY15, 151, -1, 106))
                )
            )
        ),
        storeModeSteps = listOf(
            "Prueba primero el botón \"Modo tienda\" de abajo: en muchos Bravia lo activa directamente.",
            "Si no funciona: HOME → Ajustes → Sistema (o Preferencias del dispositivo).",
            "Entra en Ajustes del modo Tienda.",
            "Activa Modo demo y/o Modo tienda."
        )
    )

    private val philips = Brand(
        id = "philips",
        name = "Philips",
        codeSets = listOf(
            CodeSet(
                name = "Philips modernas (RC6)",
                codes = codes(Protocol.RC6, 0, -1,
                    RemoteKey.POWER to 0x0C, RemoteKey.SOURCE to 0x38, RemoteKey.MUTE to 0x0D,
                    RemoteKey.VOL_UP to 0x10, RemoteKey.VOL_DOWN to 0x11,
                    RemoteKey.CH_UP to 0x4C, RemoteKey.CH_DOWN to 0x4D,
                    RemoteKey.UP to 0x58, RemoteKey.DOWN to 0x59, RemoteKey.LEFT to 0x5A, RemoteKey.RIGHT to 0x5B,
                    RemoteKey.OK to 0x5C, RemoteKey.BACK to 0x0A, RemoteKey.HOME to 0x54,
                    RemoteKey.MENU to 0x54, RemoteKey.INFO to 0x0F
                ) + digits(Protocol.RC6, 0, -1, (0..9).toList())
            ),
            CodeSet(
                name = "Philips antiguas (RC5)",
                codes = codes(Protocol.RC5, 0, -1,
                    RemoteKey.POWER to 12, RemoteKey.SOURCE to 56, RemoteKey.MUTE to 13,
                    RemoteKey.VOL_UP to 16, RemoteKey.VOL_DOWN to 17,
                    RemoteKey.CH_UP to 32, RemoteKey.CH_DOWN to 33,
                    RemoteKey.UP to 28, RemoteKey.DOWN to 29, RemoteKey.LEFT to 44, RemoteKey.RIGHT to 43,
                    RemoteKey.OK to 87, RemoteKey.MENU to 46, RemoteKey.EXIT to 15
                ) + digits(Protocol.RC5, 0, -1, (0..9).toList())
            )
        ),
        storeModeSteps = listOf(
            "Pulsa el botón de Ajustes y abre Todos los ajustes.",
            "Ve a Ajustes generales → Ubicación.",
            "Elige Tienda."
        )
    )

    private val panasonic = Brand(
        id = "panasonic",
        name = "Panasonic",
        codeSets = listOf(
            CodeSet(
                name = "Panasonic Viera",
                codes = codes(Protocol.PANASONIC, 128, 0,
                    RemoteKey.POWER to 61, RemoteKey.SOURCE to 5, RemoteKey.MUTE to 50,
                    RemoteKey.VOL_UP to 32, RemoteKey.VOL_DOWN to 33,
                    RemoteKey.CH_UP to 52, RemoteKey.CH_DOWN to 53,
                    RemoteKey.UP to 74, RemoteKey.DOWN to 75, RemoteKey.LEFT to 78, RemoteKey.RIGHT to 79,
                    RemoteKey.OK to 73, RemoteKey.BACK to 212,
                    RemoteKey.MENU to 82, RemoteKey.EXIT to 211, RemoteKey.INFO to 57
                ) + digits(Protocol.PANASONIC, 128, 0, listOf(25, 16, 17, 18, 19, 20, 21, 22, 23, 24))
            )
        ),
        storeModeSteps = listOf(
            "En la primera configuración, la TV pregunta el entorno de uso: elige Tienda.",
            "Si ya está configurada: MENÚ → Configuración y busca \"Entorno de visualización\" o \"Modo de uso\".",
            "Si no aparece, la opción suele estar en la Configuración inicial (Menú → Configuración → Configuración inicial)."
        )
    )

    private val tcl = Brand(
        id = "tcl",
        name = "TCL",
        codeSets = listOf(
            CodeSet(
                name = "TCL Roku TV",
                codes = codes(Protocol.RCA38, 15, -1,
                    RemoteKey.POWER to 42, RemoteKey.SOURCE to 163, RemoteKey.MUTE to 63,
                    RemoteKey.VOL_UP to 47, RemoteKey.VOL_DOWN to 46,
                    RemoteKey.CH_UP to 45, RemoteKey.CH_DOWN to 44,
                    RemoteKey.UP to 89, RemoteKey.DOWN to 88, RemoteKey.LEFT to 86, RemoteKey.RIGHT to 87,
                    RemoteKey.OK to 244, RemoteKey.BACK to 39, RemoteKey.MENU to 8, RemoteKey.INFO to 60
                )
            )
        ),
        storeModeSteps = listOf(
            "Pulsa HOME y entra en Ajustes.",
            "Ve a Sistema (o Preferencias del dispositivo).",
            "Busca Modo de uso / Modo tienda y elige Tienda."
        )
    )

    val all: List<Brand> = listOf(samsung, lg, sony, philips, panasonic, tcl)

    fun byId(id: String?): Brand? = all.firstOrNull { it.id == id }
}
