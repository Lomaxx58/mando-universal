package com.marcos.mandouniversal.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marcos.mandouniversal.RemoteController
import com.marcos.mandouniversal.data.AcProtocols
import com.marcos.mandouniversal.data.AcState
import com.marcos.mandouniversal.data.AcStateStore
import com.marcos.mandouniversal.data.Signal

private val modeLabel = mapOf("COOL" to "Frío", "HEAT" to "Calor", "DRY" to "Seco", "FAN" to "Ventilar", "AUTO" to "Auto")
private val modeIcon = mapOf("COOL" to "❄️", "HEAT" to "☀️", "DRY" to "💧", "FAN" to "🌀", "AUTO" to "🅰")
private val fanLabel = mapOf("AUTO" to "Auto", "LOW" to "Baja", "MEDIUM" to "Media", "HIGH" to "Alta")
private val CoolBlue = Color(0xFF4FB3FF)
private val HeatOrange = Color(0xFFFF8A3D)
private val OnGreen = Color(0xFF3DBE6B)

/**
 * Mando completo de aire: la pantalla muestra siempre el estado que se ha
 * enviado (encendido, modo, grados, ventilador, oscilación), y cada cambio
 * manda la orden entera, así que el aire queda igual que la pantalla.
 */
@Composable
fun FullAcRemote(controller: RemoteController, info: AcProtocols.Info, savedId: String) {
    val context = LocalContext.current
    val store = remember { AcStateStore(context) }
    var state by remember(savedId) { mutableStateOf(store.load(savedId, info)) }

    fun sendState(new: AcState, p: Int) {
        state = new
        store.save(savedId, new)
        if (p >= 0) {
            val i = AcProtocols.index(p, new.mode, new.temp, new.fan, new.swing)
            controller.send(Signal("Aire", "", "ACP", frequency = info.frequency, acFile = info.file, acIndex = i))
        }
    }
    // Si está apagado, los cambios solo se guardan y se aplican al encender
    fun change(new: AcState) = sendState(new, if (new.power) 0 else -1)

    val range = info.temps[state.mode]
    val accent = when (state.mode) { "COOL" -> CoolBlue; "HEAT" -> HeatOrange; else -> Accent }

    // Pantalla del aire
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(Surface1)
            .padding(vertical = 20.dp, horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            if (state.power) "ENCENDIDO" else "APAGADO",
            color = if (state.power) OnGreen else TextDim,
            fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp
        )
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.Top) {
            Text(
                if (range != null) "${state.temp}" else "--",
                color = if (state.power) TextMain else TextDim,
                fontSize = 84.sp, fontWeight = FontWeight.Light
            )
            Text("°C", color = TextDim, fontSize = 24.sp, modifier = Modifier.padding(top = 18.dp))
        }
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatusItem(modeIcon[state.mode] ?: "", "Modo", modeLabel[state.mode] ?: state.mode, accent)
            StatusItem("🌬️", "Ventilador", fanLabel[state.fan] ?: state.fan, TextMain)
            if (info.swing) StatusItem("↕️", "Oscilación", if (state.swing) "Sí" else "No", TextMain)
        }
        if (!state.power) {
            Text("Los cambios se envían al encender", color = TextDim, fontSize = 12.sp,
                modifier = Modifier.padding(top = 10.dp))
        }
    }

    Spacer(Modifier.height(20.dp))

    // Encendido y temperatura
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        RoundAction("−", enabled = range != null && state.temp > range.first) {
            change(state.copy(temp = state.temp - 1))
        }
        RemoteButton(
            controller = null, signal = null,
            modifier = Modifier.size(84.dp), shape = CircleShape,
            color = if (state.power) PowerRed else Surface2,
            onClick = {
                val on = !state.power
                sendState(state.copy(power = on), if (on) 2 else 1)
            }
        ) { Icon(PowerIcon, "Encender / apagar", tint = Color.White, modifier = Modifier.size(40.dp)) }
        RoundAction("+", enabled = range != null && state.temp < range.last) {
            change(state.copy(temp = state.temp + 1))
        }
    }

    Spacer(Modifier.height(22.dp))
    SectionTitle("Modo")
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        info.modes.forEach { m ->
            val sel = m == state.mode
            Column(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (sel) accent else Surface2)
                    .clickable {
                        val r = info.temps[m]
                        val t = r?.let { state.temp.coerceIn(it) } ?: state.temp
                        change(state.copy(mode = m, temp = t))
                    }
                    .padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(modeIcon[m] ?: "", fontSize = 18.sp)
                Text(modeLabel[m] ?: m, color = if (sel) Color.Black else TextMain, fontSize = 12.sp,
                    textAlign = TextAlign.Center)
            }
        }
    }

    Spacer(Modifier.height(16.dp))
    SectionTitle("Ventilador")
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        info.fans.forEach { f ->
            val sel = f == state.fan
            Text(
                fanLabel[f] ?: f,
                color = if (sel) Color.Black else TextMain,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (sel) Accent else Surface2)
                    .clickable { change(state.copy(fan = f)) }
                    .padding(vertical = 12.dp)
            )
        }
    }

    if (info.swing) {
        Spacer(Modifier.height(16.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Surface2)
                .clickable { change(state.copy(swing = !state.swing)) }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("↕️  Oscilación de las lamas", color = TextMain, fontSize = 15.sp, modifier = Modifier.weight(1f))
            Box(
                Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (state.swing) Accent else Surface1)
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) { Text(if (state.swing) "Sí" else "No", color = if (state.swing) Color.Black else TextMain) }
        }
    }

    Spacer(Modifier.height(16.dp))
    Text(
        "La pantalla muestra lo último que se ha enviado. Si alguien usa el mando original, " +
            "pulsa cualquier botón aquí y el aire se pondrá como indica la pantalla.",
        color = TextDim, fontSize = 12.sp
    )
    Spacer(Modifier.height(24.dp))
}

@Composable
private fun StatusItem(icon: String, title: String, value: String, valueColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(icon, fontSize = 20.sp)
        Text(title, color = TextDim, fontSize = 11.sp)
        Text(value, color = valueColor, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun RoundAction(text: String, enabled: Boolean, onClick: () -> Unit) {
    RemoteButton(
        controller = null, signal = null,
        modifier = Modifier
            .size(68.dp),
        shape = CircleShape,
        color = Surface2,
        onClick = if (enabled) onClick else null
    ) { Text(text, color = TextMain, fontSize = 32.sp) }
}
