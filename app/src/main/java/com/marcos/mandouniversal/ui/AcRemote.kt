package com.marcos.mandouniversal.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marcos.mandouniversal.RemoteController
import com.marcos.mandouniversal.data.RemoteDef
import com.marcos.mandouniversal.data.Signal

private val modeNames = linkedMapOf(
    "COOL" to "❄️ Frío", "HEAT" to "☀️ Calor", "DRY" to "💧 Seco", "AUTO" to "🅰 Auto", "FAN" to "🌀 Ventilar"
)

/**
 * Mando de aire acondicionado. Si el código trae estados completos
 * (frío/calor a cada temperatura) se maneja con modo + temperatura, como un
 * mando de verdad; si no, con las teclas que traiga (temp +/-, modo, ventilador...).
 */
@Composable
fun ColumnScope.AcRemote(controller: RemoteController, remote: RemoteDef) {
    val placed = HashSet<String>()
    fun k(key: String): Signal? = remote[key]?.also { placed += key }
    val states = remote.acStates
    remote.signals.forEach { if (Regex("^(COOL|HEAT|DRY|AUTO|FAN)_\\d{2}$").matches(it.key)) placed += it.key }

    // Encendido
    val power = k("POWER")
    val on = k("POWER_ON")
    val off = k("POWER_OFF")
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        when {
            power != null -> PowerButton(controller, power, 80)
            on != null || off != null -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    PowerButton(controller, on, 68); Text("Encender", color = TextDim, fontSize = 12.sp)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    PowerButton(controller, off, 68); Text("Apagar", color = TextDim, fontSize = 12.sp)
                }
            }
            else -> PowerButton(controller, null, 80)
        }
    }

    Spacer(Modifier.height(24.dp))

    if (states.isNotEmpty()) {
        val modes = modeNames.keys.filter { it in states }
        var mode by rememberSaveable { mutableStateOf(if ("COOL" in modes) "COOL" else modes.first()) }
        val temps = states[mode].orEmpty().keys.toList()
        var temp by rememberSaveable(mode) {
            mutableIntStateOf(temps.minByOrNull { kotlin.math.abs(it - 24) } ?: temps.first())
        }
        fun sendState() { states[mode]?.get(temp)?.let { controller.send(it) } }

        // Pantalla
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Surface1)
                .padding(vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(modeNames[mode] ?: mode, color = TextDim, fontSize = 16.sp)
            Text("$temp°", color = TextMain, fontSize = 72.sp, fontWeight = FontWeight.Light)
            Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                RemoteButton(controller = null, signal = null, modifier = Modifier.size(64.dp), shape = CircleShape,
                    color = Surface2, onClick = {
                        temps.lastOrNull { it < temp }?.let { temp = it; sendState() }
                    }) { Text("−", color = TextMain, fontSize = 30.sp) }
                RemoteButton(controller = null, signal = null, modifier = Modifier.size(64.dp), shape = CircleShape,
                    color = Surface2, onClick = {
                        temps.firstOrNull { it > temp }?.let { temp = it; sendState() }
                    }) { Text("+", color = TextMain, fontSize = 30.sp) }
            }
            Text("Temperaturas disponibles: ${temps.first()}–${temps.last()} °C", color = TextDim,
                fontSize = 12.sp, modifier = Modifier.padding(top = 12.dp))
        }

        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            modes.forEach { m ->
                val sel = m == mode
                Text(
                    modeNames[m] ?: m,
                    color = if (sel) Color.Black else TextMain,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (sel) Accent else Surface2)
                        .clickable {
                            mode = m
                            val t = states[m].orEmpty().keys
                            temp = t.minByOrNull { kotlin.math.abs(it - temp) } ?: temp
                            sendState()
                        }
                        .padding(vertical = 12.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    } else {
        // Sin estados: teclas de temperatura sueltas
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Surface1)
                .padding(vertical = 22.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            RemoteButton(controller, k("TEMP_DOWN"), Modifier.size(72.dp), CircleShape, repeatable = true) {
                Text("−", color = TextMain, fontSize = 32.sp)
            }
            Text("TEMP", color = TextDim, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            RemoteButton(controller, k("TEMP_UP"), Modifier.size(72.dp), CircleShape, repeatable = true) {
                Text("+", color = TextMain, fontSize = 32.sp)
            }
        }
    }

    // Teclas de función habituales
    val funcs = listOf("MODE" to "Modo", "FAN" to "Ventilador", "SWING" to "Oscilar", "TIMER" to "Temporizador", "SLEEP" to "Noche")
        .filter { remote.has(it.first) }
    if (funcs.isNotEmpty()) {
        Spacer(Modifier.height(20.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            funcs.take(3).forEach { (key, label) -> FuncButton(controller, k(key), label, Modifier.weight(1f)) }
        }
        if (funcs.size > 3) {
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                funcs.drop(3).forEach { (key, label) -> FuncButton(controller, k(key), label, Modifier.weight(1f)) }
            }
        }
    }

    val rest = remote.signals.filter { it.key !in placed }
    if (rest.isNotEmpty()) {
        Spacer(Modifier.height(20.dp))
        SectionTitle("Más botones")
        ButtonGrid(controller, rest)
    }
    Spacer(Modifier.height(24.dp))
}

@Composable
private fun FuncButton(controller: RemoteController, signal: Signal?, label: String, modifier: Modifier) {
    RemoteButton(controller, signal, modifier.height(52.dp)) {
        Text(label, color = TextMain, fontSize = 14.sp)
    }
}
