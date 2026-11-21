package com.marcos.mandouniversal.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marcos.mandouniversal.RemoteController
import com.marcos.mandouniversal.data.RemoteDef
import com.marcos.mandouniversal.data.Signal

/**
 * Mando estándar (TV, TV box, proyector, audio, ventilador...). Cada bloque solo
 * aparece si el código tiene alguna de sus teclas; el resto va a "Más botones".
 */
@Composable
fun ColumnScope.StandardRemote(controller: RemoteController, remote: RemoteDef) {
    val placed = HashSet<String>()
    fun k(key: String): Signal? = remote[key]?.also { placed += key }
    var showNumbers by rememberSaveable { mutableStateOf(false) }

    // Encendido: si no hay tecla única de encendido, se usan las de encender/apagar
    val power = k("POWER")
    val powerOn = if (power == null) k("POWER_ON") else null
    val powerOff = if (power == null) k("POWER_OFF") else null

    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (power != null || (powerOn == null && powerOff == null)) {
            PowerButton(controller, power)
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    PowerButton(controller, powerOn, 56); Text("On", color = TextDim, fontSize = 11.sp)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    PowerButton(controller, powerOff, 56); Text("Off", color = TextDim, fontSize = 11.sp)
                }
            }
        }
        val source = k("SOURCE")
        if (source != null) {
            RemoteButton(controller, source, Modifier.size(width = 112.dp, height = 52.dp)) {
                Text("Fuente", color = TextMain, fontSize = 15.sp)
            }
        }
        RemoteButton(controller, k("MUTE"), Modifier.size(64.dp), CircleShape) {
            Text("🔇", fontSize = 22.sp)
        }
    }

    // Cruceta
    if (listOf("UP", "DOWN", "LEFT", "RIGHT", "OK").any { remote.has(it) }) {
        Spacer(Modifier.height(24.dp))
        Box(
            Modifier
                .align(Alignment.CenterHorizontally)
                .size(240.dp)
                .clip(CircleShape)
                .background(Surface1),
            contentAlignment = Alignment.Center
        ) {
            val s = 70.dp
            RemoteButton(controller, k("UP"), Modifier.align(Alignment.TopCenter).size(s), CircleShape, Surface1, repeatable = true) {
                Icon(Icons.Filled.KeyboardArrowUp, "Arriba", tint = TextMain, modifier = Modifier.size(40.dp))
            }
            RemoteButton(controller, k("DOWN"), Modifier.align(Alignment.BottomCenter).size(s), CircleShape, Surface1, repeatable = true) {
                Icon(Icons.Filled.KeyboardArrowDown, "Abajo", tint = TextMain, modifier = Modifier.size(40.dp))
            }
            RemoteButton(controller, k("LEFT"), Modifier.align(Alignment.CenterStart).size(s), CircleShape, Surface1, repeatable = true) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Izquierda", tint = TextMain, modifier = Modifier.size(40.dp))
            }
            RemoteButton(controller, k("RIGHT"), Modifier.align(Alignment.CenterEnd).size(s), CircleShape, Surface1, repeatable = true) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Derecha", tint = TextMain, modifier = Modifier.size(40.dp))
            }
            RemoteButton(controller, k("OK"), Modifier.size(92.dp), CircleShape, Accent) {
                Text("OK", color = Color.Black, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    // Navegación
    val nav = listOf(
        Triple("BACK", Icons.AutoMirrored.Filled.ArrowBack, "Atrás"),
        Triple("HOME", Icons.Filled.Home, "Inicio"),
        Triple("MENU", Icons.Filled.Menu, "Menú"),
        Triple("EXIT", Icons.Filled.Close, "Salir"),
        Triple("INFO", Icons.Filled.Info, "Info"),
    ).filter { remote.has(it.first) }.take(4)
    if (nav.isNotEmpty()) {
        Spacer(Modifier.height(20.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            nav.forEach { (key, icon, label) -> NavButton(controller, k(key), icon, label) }
        }
    }

    // Volumen, números y canales
    val hasVol = remote.has("VOL_UP") || remote.has("VOL_DOWN")
    val hasCh = remote.has("CH_UP") || remote.has("CH_DOWN")
    val hasDigits = (0..9).count { remote.has("D$it") } >= 6
    if (hasVol || hasCh || hasDigits) {
        Spacer(Modifier.height(22.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (hasVol) Rocker(controller, "VOL", k("VOL_UP"), k("VOL_DOWN")) else Spacer(Modifier.width(76.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (hasDigits) {
                    RemoteButton(
                        controller = null, signal = null,
                        modifier = Modifier.size(width = 92.dp, height = 50.dp),
                        color = if (showNumbers) Accent else Surface2,
                        onClick = { showNumbers = !showNumbers }
                    ) { Text("123", color = if (showNumbers) Color.Black else TextMain, fontWeight = FontWeight.Bold) }
                }
                k("GUIDE")?.let { g ->
                    RemoteButton(controller, g, Modifier.size(width = 92.dp, height = 44.dp)) {
                        Text("Guía", color = TextMain, fontSize = 14.sp)
                    }
                }
            }
            if (hasCh) Rocker(controller, "CH", k("CH_UP"), k("CH_DOWN")) else Spacer(Modifier.width(76.dp))
        }
    }
    if (hasDigits && showNumbers) {
        Spacer(Modifier.height(16.dp))
        NumberPad(controller, remote) { placed += it }
    } else if (hasDigits) {
        (0..9).forEach { placed += "D$it" }
    }

    // Reproducción
    val media = listOf("REW" to "⏪", "PLAY" to "▶", "PLAY_PAUSE" to "⏯", "PAUSE" to "⏸", "STOP" to "⏹", "FF" to "⏩")
        .filter { remote.has(it.first) }
    if (media.isNotEmpty()) {
        Spacer(Modifier.height(20.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            media.forEach { (key, sym) ->
                RemoteButton(controller, k(key), Modifier.size(52.dp), CircleShape) {
                    if (key == "PLAY") Icon(Icons.Filled.PlayArrow, "Play", tint = TextMain)
                    else Text(sym, color = TextMain, fontSize = 18.sp)
                }
            }
        }
    }

    // Colores
    val colors = listOf("RED" to Color(0xFFE5484D), "GREEN" to Color(0xFF3DBE6B), "YELLOW" to Color(0xFFF5C542), "BLUE" to Color(0xFF3E8BFF))
        .filter { remote.has(it.first) }
    if (colors.isNotEmpty()) {
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            colors.forEach { (key, c) ->
                RemoteButton(controller, k(key), Modifier.size(width = 64.dp, height = 22.dp), RoundedCornerShape(11.dp), c) {}
            }
        }
    }

    // Resto de teclas
    val rest = remote.signals.filter { it.key !in placed }
    if (rest.isNotEmpty()) {
        Spacer(Modifier.height(20.dp))
        SectionTitle("Más botones")
        ButtonGrid(controller, rest)
    }
    Spacer(Modifier.height(24.dp))
}

@Composable
private fun NavButton(controller: RemoteController, signal: Signal?, icon: ImageVector, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        RemoteButton(controller, signal, Modifier.size(56.dp), CircleShape) {
            Icon(icon, label, tint = TextMain)
        }
        Text(label, color = TextDim, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
fun Rocker(controller: RemoteController, label: String, up: Signal?, down: Signal?) {
    Column(
        Modifier
            .width(76.dp)
            .clip(RoundedCornerShape(38.dp))
            .background(Surface2),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        RemoteButton(controller, up, Modifier.size(76.dp), CircleShape, Surface2, repeatable = true) {
            Text("+", color = TextMain, fontSize = 30.sp)
        }
        Text(label, color = TextDim, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        RemoteButton(controller, down, Modifier.size(76.dp), CircleShape, Surface2, repeatable = true) {
            Text("−", color = TextMain, fontSize = 30.sp)
        }
    }
}

@Composable
private fun NumberPad(controller: RemoteController, remote: RemoteDef, mark: (String) -> Unit) {
    val rows = listOf(listOf(1, 2, 3), listOf(4, 5, 6), listOf(7, 8, 9), listOf(null, 0, null))
    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                row.forEach { d ->
                    if (d == null) {
                        Spacer(Modifier.size(width = 84.dp, height = 54.dp))
                    } else {
                        mark("D$d")
                        RemoteButton(controller, remote["D$d"], Modifier.size(width = 84.dp, height = 54.dp)) {
                            Text("$d", color = TextMain, fontSize = 22.sp)
                        }
                    }
                }
            }
        }
    }
}
