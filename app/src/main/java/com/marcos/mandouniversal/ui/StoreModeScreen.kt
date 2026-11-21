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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marcos.mandouniversal.RemoteController
import com.marcos.mandouniversal.data.RemoteDef
import com.marcos.mandouniversal.data.StoreGuides

/** Guía de modo tienda con un mini mando en la misma pantalla. */
@Composable
fun StoreModeScreen(controller: RemoteController, brandName: String, remote: RemoteDef, onBack: () -> Unit) {
    val guide = StoreGuides.forBrand(brandName)
    Column(
        Modifier
            .fillMaxSize()
            .background(Background)
            .statusBarsPadding()
    ) {
        TopBar(title = "Modo tienda", subtitle = brandName, onBack = onBack)
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Text(
                if (guide.generic) "No tengo la ruta exacta de esta marca; estos pasos suelen funcionar en casi todas."
                else "Ruta orientativa: el nombre exacto de los menús cambia según el año del modelo.",
                color = TextDim, fontSize = 13.sp, modifier = Modifier.padding(bottom = 12.dp)
            )
            guide.steps.forEachIndexed { i, step ->
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

            if (guide.extras.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                SectionTitle("Botones especiales")
                guide.extras.forEach { extra ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Surface1)
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(extra.description, color = TextDim, fontSize = 13.sp, modifier = Modifier.weight(1f))
                        Spacer(Modifier.size(8.dp))
                        RemoteButton(controller, extra.signal, Modifier.size(width = 130.dp, height = 44.dp), color = Accent) {
                            Text(extra.label, color = Color.Black, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
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
                val menu = remote["MENU"] ?: remote["HOME"]
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    RemoteButton(controller, remote["HOME"] ?: remote["MENU"], Modifier.size(s), CircleShape) { Icon(Icons.Filled.Home, "Inicio", tint = TextMain) }
                    RemoteButton(controller, remote["UP"], Modifier.size(s), CircleShape, repeatable = true) { Icon(Icons.Filled.KeyboardArrowUp, "Arriba", tint = TextMain) }
                    RemoteButton(controller, menu, Modifier.size(s), CircleShape) { Icon(Icons.Filled.Menu, "Menú", tint = TextMain) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    RemoteButton(controller, remote["LEFT"], Modifier.size(s), CircleShape, repeatable = true) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Izquierda", tint = TextMain) }
                    RemoteButton(controller, remote["OK"], Modifier.size(s), CircleShape, Accent) { Text("OK", color = Color.Black, fontWeight = FontWeight.Bold) }
                    RemoteButton(controller, remote["RIGHT"], Modifier.size(s), CircleShape, repeatable = true) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Derecha", tint = TextMain) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    RemoteButton(controller, remote["BACK"] ?: remote["EXIT"], Modifier.size(s), CircleShape) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Atrás", tint = TextMain) }
                    RemoteButton(controller, remote["DOWN"], Modifier.size(s), CircleShape, repeatable = true) { Icon(Icons.Filled.KeyboardArrowDown, "Abajo", tint = TextMain) }
                    PowerButton(controller, remote["POWER"] ?: remote["POWER_OFF"], 56)
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
