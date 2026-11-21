package com.marcos.mandouniversal.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marcos.mandouniversal.RemoteController
import com.marcos.mandouniversal.data.Brand
import com.marcos.mandouniversal.data.Category
import com.marcos.mandouniversal.data.RemoteDef

/**
 * Búsqueda del código que funciona, como en los mandos de Vivo o Xiaomi:
 * se prueba la tecla de encendido de cada mando de la marca hasta que el
 * aparato responde.
 */
@Composable
fun MatchScreen(
    controller: RemoteController,
    category: Category,
    brand: Brand,
    startIndex: Int,
    onBack: () -> Unit,
    onFound: (RemoteDef) -> Unit
) {
    val total = brand.remotes.size
    var index by rememberSaveable(brand.name) { mutableIntStateOf(startIndex.coerceIn(0, total - 1)) }
    var tried by rememberSaveable(brand.name) { mutableStateOf(false) }
    var showList by rememberSaveable { mutableStateOf(false) }
    val remote = brand.remotes[index]
    val test = remote.testSignal

    Column(
        Modifier
            .fillMaxSize()
            .background(Background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TopBar(title = brand.name, subtitle = "Buscando el código de tu ${category.title.lowercase()}", onBack = onBack)

        if (!controller.hasIrEmitter) NoIrWarning(Modifier.padding(16.dp))

        Text("Código ${index + 1} de $total", color = TextDim, fontSize = 14.sp,
            modifier = Modifier.padding(top = 12.dp))
        LinearProgressIndicator(
            progress = { (index + 1f) / total },
            color = Accent, trackColor = Surface2,
            modifier = Modifier
                .padding(horizontal = 40.dp, vertical = 10.dp)
                .fillMaxWidth()
        )

        Spacer(Modifier.height(24.dp))
        Text(
            "Apunta el móvil al aparato y pulsa el botón",
            color = TextMain, fontSize = 18.sp, textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
        Spacer(Modifier.height(28.dp))

        val isPower = test.key.startsWith("POWER")
        RemoteButton(
            controller = controller,
            signal = test,
            modifier = Modifier.size(120.dp),
            shape = androidx.compose.foundation.shape.CircleShape,
            color = if (isPower) PowerRed else Accent,
            onClick = { controller.send(test); tried = true }
        ) {
            if (isPower) {
                androidx.compose.material3.Icon(PowerIcon, "Probar", tint = Color.White, modifier = Modifier.size(56.dp))
            } else {
                Text(prettyLabel(test.label), color = Color.Black, fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center)
            }
        }
        Text(
            if (isPower) "Tecla de encendido" else "Tecla: ${prettyLabel(test.label)}",
            color = TextDim, fontSize = 13.sp, modifier = Modifier.padding(top = 10.dp)
        )

        Spacer(Modifier.height(32.dp))
        Text(
            if (tried) "¿Ha respondido el aparato?" else "Pulsa el botón de arriba para probar",
            color = TextMain, fontSize = 17.sp, fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(16.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = {
                    index = (index + 1) % total
                    tried = false
                },
                enabled = total > 1,
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp),
                shape = RoundedCornerShape(14.dp)
            ) { Text(if (total > 1) "No, siguiente" else "Solo hay uno", color = TextMain) }
            Button(
                onClick = { onFound(remote) },
                enabled = tried,
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Color.Black)
            ) { Text("Sí, funciona", fontWeight = FontWeight.Bold) }
        }

        TextButton(onClick = { showList = !showList }, modifier = Modifier.padding(top = 16.dp)) {
            Text(if (showList) "Ocultar lista de códigos" else "Elegir el código de la lista", color = Accent)
        }
        if (showList) {
            Column(Modifier.padding(horizontal = 16.dp)) {
                brand.remotes.forEachIndexed { i, r ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (i == index) Surface2 else Surface1)
                            .clickable { index = i; tried = false }
                            .padding(14.dp)
                    ) {
                        Text("${i + 1}. ${r.model}", color = TextMain, modifier = Modifier.weight(1f))
                        Text("${r.signals.size} teclas", color = TextDim, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}
