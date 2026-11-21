package com.marcos.mandouniversal.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marcos.mandouniversal.RemoteController
import com.marcos.mandouniversal.data.Brand
import com.marcos.mandouniversal.data.Category
import com.marcos.mandouniversal.data.RemoteDef
import com.marcos.mandouniversal.data.SavedRemote
import com.marcos.mandouniversal.data.Signal

/** Acciones del menú de tres puntos. */
class RemoteActions(
    val onBack: () -> Unit,
    val onStoreMode: () -> Unit,
    val onTryOther: () -> Unit,
    val onRename: (String) -> Unit,
    val onDelete: () -> Unit
)

/**
 * Marco común de todos los mandos: barra superior (flecha atrás, marca centrada,
 * tres puntos), menú con modo tienda / otro código / info / renombrar / eliminar,
 * y la vista de "Todos los botones".
 */
@Composable
fun RemoteFrame(
    controller: RemoteController,
    saved: SavedRemote,
    brand: Brand,
    remote: RemoteDef,
    actions: RemoteActions,
    content: @Composable ColumnScope.() -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    var dialog by rememberSaveable { mutableStateOf<String?>(null) }
    var showAll by rememberSaveable { mutableStateOf(false) }
    val isTv = saved.category == Category.TV

    Column(
        Modifier
            .fillMaxSize()
            .background(Background)
            .statusBarsPadding()
    ) {
        TopBar(
            title = brand.name,
            subtitle = if (saved.name != saved.brand) saved.name else saved.category.title,
            onBack = if (showAll) ({ showAll = false }) else actions.onBack,
            onMenu = { menuOpen = true }
        ) {
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                if (isTv) {
                    MenuItem("🏪  Modo tienda") { menuOpen = false; actions.onStoreMode() }
                }
                MenuItem("🔄  Probar otro código") { menuOpen = false; actions.onTryOther() }
                MenuItem(if (showAll) "🎛️  Volver al mando" else "🔢  Todos los botones (${remote.signals.size})") {
                    menuOpen = false; showAll = !showAll
                }
                MenuItem(if (isTv) "ℹ️  Información de la tele" else "ℹ️  Información del mando") {
                    menuOpen = false; dialog = "info"
                }
                MenuItem("💡  Consejos de uso") { menuOpen = false; dialog = "tips" }
                HorizontalDivider()
                MenuItem("✏️  Cambiar nombre") { menuOpen = false; dialog = "rename" }
                MenuItem("🗑️  Eliminar de mis mandos") { menuOpen = false; dialog = "delete" }
            }
        }

        if (!controller.hasIrEmitter) NoIrWarning(Modifier.padding(horizontal = 16.dp, vertical = 4.dp))

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            if (showAll) AllButtons(controller, remote.signals) else content()
        }
    }

    when (dialog) {
        "info" -> InfoDialog(controller, saved, brand, remote) { dialog = null }
        "tips" -> TipsDialog(saved.category) { dialog = null }
        "rename" -> RenameDialog(saved.name, onDismiss = { dialog = null }) { actions.onRename(it); dialog = null }
        "delete" -> AlertDialog(
            onDismissRequest = { dialog = null },
            title = { Text("¿Eliminar este mando?") },
            text = { Text("Se quitará \"${saved.name}\" de Mis mandos. Puedes volver a añadirlo cuando quieras.") },
            confirmButton = { TextButton(onClick = { dialog = null; actions.onDelete() }) { Text("Eliminar", color = PowerRed) } },
            dismissButton = { TextButton(onClick = { dialog = null }) { Text("Cancelar") } },
            containerColor = Surface1
        )
    }
}

@Composable
private fun MenuItem(text: String, onClick: () -> Unit) {
    DropdownMenuItem(text = { Text(text, fontSize = 15.sp) }, onClick = onClick)
}

/** Rejilla con todas las teclas que trae el código, con su nombre original. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ButtonGrid(controller: RemoteController, signals: List<Signal>) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        signals.forEach { s ->
            RemoteButton(
                controller, s,
                Modifier
                    .width(104.dp)
                    .height(52.dp),
                RoundedCornerShape(14.dp),
                repeatable = s.key.startsWith("VOL") || s.key.startsWith("TEMP")
            ) {
                Text(prettyLabel(s.label), color = TextMain, fontSize = 13.sp, textAlign = TextAlign.Center,
                    maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 6.dp))
            }
        }
    }
}

@Composable
private fun AllButtons(controller: RemoteController, signals: List<Signal>) {
    SectionTitle("Todos los botones")
    Text("Son todas las teclas que trae este código, con el nombre con el que se capturaron.",
        color = TextDim, fontSize = 13.sp, modifier = Modifier.padding(bottom = 12.dp))
    ButtonGrid(controller, signals)
}

/** Información del mando y, en teles, datos destacables deducidos de sus teclas. */
@Composable
private fun InfoDialog(
    controller: RemoteController,
    saved: SavedRemote,
    brand: Brand,
    remote: RemoteDef,
    onDismiss: () -> Unit
) {
    val protocols = remote.signals.map { if (it.isRaw) "Señal capturada (raw)" else it.protocol }.distinct()
    val carriers = remote.signals.map { it.carrier }.distinct().sorted().joinToString(", ") { "${it / 1000} kHz" }
    val position = brand.remotes.indexOf(remote) + 1
    val labels = remote.signals.map { it.label.lowercase() }
    fun hasAny(vararg w: String) = labels.any { l -> w.any { l.contains(it) } }

    val highlights = buildList {
        if (saved.category == Category.TV) {
            val apps = buildList {
                if (hasAny("netflix")) add("Netflix")
                if (hasAny("youtube")) add("YouTube")
                if (hasAny("prime", "amazon")) add("Prime Video")
                if (hasAny("disney")) add("Disney+")
                if (hasAny("rakuten")) add("Rakuten TV")
            }
            if (apps.isNotEmpty() || remote.has("HOME")) add("Smart TV" + if (apps.isNotEmpty()) ": botones de ${apps.joinToString(", ")}" else "")
            if (hasAny("3d")) add("Compatible con 3D")
            if (hasAny("hdmi")) add("Acceso directo a HDMI")
            if (remote.has("RED")) add("Teclas de colores (teletexto / HbbTV)")
            if (hasAny("voice", "mic", "google", "alexa")) add("Mando con asistente de voz")
            if (hasAny("bluetooth", "bt")) add("Bluetooth")
            if (hasAny("picture", "p_mode", "p.mode", "pic")) add("Botón de modo de imagen")
        }
        if (remote.acStates.isNotEmpty()) {
            val temps = remote.acStates.values.flatMap { it.keys }
            add("Temperaturas guardadas: ${temps.min()}–${temps.max()} °C")
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (saved.category == Category.TV) "Información de la tele" else "Información del mando") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                InfoRow("Marca", brand.name)
                InfoRow("Modelo de mando", remote.model)
                InfoRow("Código", "$position de ${brand.remotes.size} de la marca")
                InfoRow("Teclas", "${remote.signals.size}")
                InfoRow("Protocolo", protocols.joinToString(", "))
                InfoRow("Frecuencia", carriers)
                InfoRow("Emisor del móvil", if (controller.hasIrEmitter) controller.emitterRanges else "no tiene")
                if (highlights.isNotEmpty()) {
                    Text("Destacable", color = Accent, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                    highlights.forEach { Text("• $it", color = TextMain, fontSize = 14.sp) }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cerrar", color = Accent) } },
        containerColor = Surface1
    )
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row {
        Text(label, color = TextDim, fontSize = 14.sp, modifier = Modifier.width(128.dp))
        Text(value, color = TextMain, fontSize = 14.sp)
    }
}

@Composable
private fun TipsDialog(category: Category, onDismiss: () -> Unit) {
    val tips = buildList {
        add("Apunta la parte superior del móvil (donde está el emisor) hacia el aparato.")
        add("Funciona mejor a menos de 5 metros y sin obstáculos.")
        add("Si una tecla no responde, prueba \"Probar otro código\": a veces otro mando de la misma marca tiene esa tecla.")
        add("En \"Todos los botones\" están todas las teclas del código, también las poco comunes.")
        if (category == Category.TV) add("Volumen, canales y flechas se repiten si mantienes pulsado.")
        if (category == Category.AC) add("Si tu aire tiene temperaturas guardadas, cada pulsación envía el modo y la temperatura completos, así que el aparato se pone como indica la pantalla.")
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Consejos de uso") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { tips.forEach { Text("• $it", fontSize = 14.sp) } } },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Entendido", color = Accent) } },
        containerColor = Surface1
    )
}

@Composable
private fun RenameDialog(current: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var text by rememberSaveable { mutableStateOf(current) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nombre del mando") },
        text = {
            OutlinedTextField(
                value = text, onValueChange = { text = it }, singleLine = true,
                placeholder = { Text("Ej.: Tele escaparate 3") },
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Accent, cursorColor = Accent)
            )
        },
        confirmButton = { TextButton(onClick = { onSave(text.trim()) }) { Text("Guardar", color = Accent) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
        containerColor = Surface1
    )
}
