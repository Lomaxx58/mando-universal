package com.marcos.mandouniversal.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marcos.mandouniversal.RemoteController
import com.marcos.mandouniversal.data.Brand
import com.marcos.mandouniversal.data.CodeSet
import com.marcos.mandouniversal.data.RemoteKey

/**
 * Guía para poner la tele en modo tienda, con un mini mando en la misma
 * pantalla para seguir los pasos sin cambiar de pantalla.
 */
@Composable
fun StoreModeScreen(
    controller: RemoteController,
    brand: Brand,
    codeSet: CodeSet,
    onBack: () -> Unit
) {
    val c = codeSet.codes
    Column(
        Modifier
            .fillMaxSize()
            .background(Background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("← Mando", color = Accent) }
        }
        Text("Modo tienda · ${brand.name}", color = TextMain, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text(
            "Ruta orientativa: el nombre exacto de los menús cambia según el año del modelo.",
            color = TextDim, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(Modifier.height(16.dp))

        brand.storeModeSteps.forEachIndexed { i, step ->
            Row(Modifier.padding(vertical = 6.dp)) {
                Box(
                    Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(Accent),
                    contentAlignment = Alignment.Center
                ) { Text("${i + 1}", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp) }
                Spacer(Modifier.size(12.dp))
                Text(step, color = TextMain, fontSize = 15.sp, modifier = Modifier.padding(top = 2.dp))
            }
        }

        if (codeSet.extras.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Text("Botones especiales", color = TextDim, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            codeSet.extras.forEach { extra ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Surface1)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(extra.description, color = TextDim, fontSize = 13.sp, modifier = Modifier.weight(1f))
                    Spacer(Modifier.size(8.dp))
                    RemoteButton(controller, extra.command, Modifier.size(width = 130.dp, height = 44.dp), color = Accent) {
                        Text(extra.label, color = Color.Black, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // Mini mando
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Surface1)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val s = 56.dp
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                RemoteButton(controller, c[RemoteKey.HOME], Modifier.size(s), CircleShape) { Icon(Icons.Filled.Home, "Inicio", tint = TextMain) }
                RemoteButton(controller, c[RemoteKey.UP], Modifier.size(s), CircleShape, repeatable = true) { Icon(Icons.Filled.KeyboardArrowUp, "Arriba", tint = TextMain) }
                RemoteButton(controller, c[RemoteKey.MENU], Modifier.size(s), CircleShape) { Icon(Icons.Filled.Menu, "Menú", tint = TextMain) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                RemoteButton(controller, c[RemoteKey.LEFT], Modifier.size(s), CircleShape, repeatable = true) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Izquierda", tint = TextMain) }
                RemoteButton(controller, c[RemoteKey.OK], Modifier.size(s), CircleShape, Accent) { Text("OK", color = Color.Black, fontWeight = FontWeight.Bold) }
                RemoteButton(controller, c[RemoteKey.RIGHT], Modifier.size(s), CircleShape, repeatable = true) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Derecha", tint = TextMain) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                RemoteButton(controller, c[RemoteKey.BACK], Modifier.size(s), CircleShape) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Atrás", tint = TextMain) }
                RemoteButton(controller, c[RemoteKey.DOWN], Modifier.size(s), CircleShape, repeatable = true) { Icon(Icons.Filled.KeyboardArrowDown, "Abajo", tint = TextMain) }
                RemoteButton(controller, c[RemoteKey.POWER], Modifier.size(s), CircleShape, PowerRed) { Text("⏻", color = Color.White, fontSize = 20.sp) }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}
