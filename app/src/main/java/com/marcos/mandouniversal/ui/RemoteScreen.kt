package com.marcos.mandouniversal.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.width
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marcos.mandouniversal.RemoteController
import com.marcos.mandouniversal.data.Brand
import com.marcos.mandouniversal.data.CodeSet
import com.marcos.mandouniversal.data.RemoteKey
import com.marcos.mandouniversal.ir.IrCommand
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun RemoteScreen(
    controller: RemoteController,
    brand: Brand,
    codeSet: CodeSet,
    onChangeBrand: () -> Unit,
    onStoreMode: () -> Unit
) {
    var showNumbers by rememberSaveable { mutableStateOf(false) }
    val c = codeSet.codes

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Cabecera
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(brand.name, color = TextMain, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text(codeSet.name, color = TextDim, fontSize = 13.sp)
            }
            TextButton(onClick = onChangeBrand) { Text("Cambiar", color = Accent) }
        }

        if (!controller.hasIrEmitter) {
            NoIrWarning(Modifier.padding(top = 8.dp))
        }

        Spacer(Modifier.height(16.dp))

        // Fila superior: encendido, fuente, silencio
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            RemoteButton(controller, c[RemoteKey.POWER], Modifier.size(64.dp), CircleShape, PowerRed) {
                Text("⏻", color = Color.White, fontSize = 26.sp)
            }
            RemoteButton(controller, c[RemoteKey.SOURCE], Modifier.size(width = 120.dp, height = 52.dp)) {
                Text("Fuente", color = TextMain, fontSize = 15.sp)
            }
            RemoteButton(controller, c[RemoteKey.MUTE], Modifier.size(64.dp), CircleShape) {
                Text("🔇", fontSize = 22.sp)
            }
        }

        Spacer(Modifier.height(28.dp))

        // Cruceta
        DPad(controller, codeSet)

        Spacer(Modifier.height(24.dp))

        // Navegación
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            NavButton(controller, c[RemoteKey.BACK], Icons.AutoMirrored.Filled.ArrowBack, "Atrás")
            NavButton(controller, c[RemoteKey.HOME], Icons.Filled.Home, "Inicio")
            NavButton(controller, c[RemoteKey.MENU], Icons.Filled.Menu, "Menú")
            NavButton(controller, c[RemoteKey.EXIT], Icons.Filled.Close, "Salir")
        }

        Spacer(Modifier.height(24.dp))

        // Volumen, números y canales
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Rocker(controller, "VOL", c[RemoteKey.VOL_UP], c[RemoteKey.VOL_DOWN])
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                RemoteButton(
                    controller = null,
                    command = null,
                    modifier = Modifier.size(width = 96.dp, height = 52.dp),
                    color = if (showNumbers) Accent else Surface2,
                    onClick = { showNumbers = !showNumbers }
                ) {
                    Text("123", color = if (showNumbers) Color.Black else TextMain, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(12.dp))
                TextButton(onClick = onStoreMode) { Text("Modo tienda", color = Accent) }
            }
            Rocker(controller, "CH", c[RemoteKey.CH_UP], c[RemoteKey.CH_DOWN])
        }

        if (showNumbers) {
            Spacer(Modifier.height(20.dp))
            NumberPad(controller, codeSet)
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun NoIrWarning(modifier: Modifier = Modifier) {
    Text(
        "Este móvil no tiene emisor de infrarrojos. La app no podrá controlar la tele " +
            "(prueba con un Xiaomi, Redmi o POCO, o un emisor IR por USB-C).",
        color = Color.Black,
        fontSize = 13.sp,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Accent)
            .padding(12.dp)
    )
}

@Composable
private fun DPad(controller: RemoteController, codeSet: CodeSet) {
    val c = codeSet.codes
    val size = 250.dp
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(Surface1),
        contentAlignment = Alignment.Center
    ) {
        val arrow = 72.dp
        RemoteButton(controller, c[RemoteKey.UP], Modifier.align(Alignment.TopCenter).size(arrow), CircleShape, Surface1, repeatable = true) {
            Icon(Icons.Filled.KeyboardArrowUp, "Arriba", tint = TextMain, modifier = Modifier.size(40.dp))
        }
        RemoteButton(controller, c[RemoteKey.DOWN], Modifier.align(Alignment.BottomCenter).size(arrow), CircleShape, Surface1, repeatable = true) {
            Icon(Icons.Filled.KeyboardArrowDown, "Abajo", tint = TextMain, modifier = Modifier.size(40.dp))
        }
        RemoteButton(controller, c[RemoteKey.LEFT], Modifier.align(Alignment.CenterStart).size(arrow), CircleShape, Surface1, repeatable = true) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Izquierda", tint = TextMain, modifier = Modifier.size(40.dp))
        }
        RemoteButton(controller, c[RemoteKey.RIGHT], Modifier.align(Alignment.CenterEnd).size(arrow), CircleShape, Surface1, repeatable = true) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Derecha", tint = TextMain, modifier = Modifier.size(40.dp))
        }
        RemoteButton(controller, c[RemoteKey.OK], Modifier.size(96.dp), CircleShape, Accent) {
            Text("OK", color = Color.Black, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun NavButton(controller: RemoteController, command: IrCommand?, icon: ImageVector, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        RemoteButton(controller, command, Modifier.size(60.dp), CircleShape) {
            Icon(icon, label, tint = TextMain)
        }
        Text(label, color = TextDim, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun Rocker(controller: RemoteController, label: String, up: IrCommand?, down: IrCommand?) {
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
private fun NumberPad(controller: RemoteController, codeSet: CodeSet) {
    val c = codeSet.codes
    val rows = listOf(
        listOf(RemoteKey.D1 to "1", RemoteKey.D2 to "2", RemoteKey.D3 to "3"),
        listOf(RemoteKey.D4 to "4", RemoteKey.D5 to "5", RemoteKey.D6 to "6"),
        listOf(RemoteKey.D7 to "7", RemoteKey.D8 to "8", RemoteKey.D9 to "9"),
        listOf(RemoteKey.INFO to "Info", RemoteKey.D0 to "0", null)
    )
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        for (row in rows) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                for (item in row) {
                    if (item == null) {
                        Spacer(Modifier.size(width = 84.dp, height = 56.dp))
                    } else {
                        RemoteButton(controller, c[item.first], Modifier.size(width = 84.dp, height = 56.dp)) {
                            if (item.first == RemoteKey.INFO) {
                                Icon(Icons.Filled.Info, "Info", tint = TextMain)
                            } else {
                                Text(item.second, color = TextMain, fontSize = 22.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Botón del mando. Envía al pulsar, vibra, y si [repeatable] repite mientras
 * se mantiene pulsado (volumen, canales, flechas). Se ve atenuado si la marca
 * no tiene ese botón.
 */
@Composable
fun RemoteButton(
    controller: RemoteController?,
    command: IrCommand?,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    color: Color = Surface2,
    repeatable: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    var pressed by remember { mutableStateOf(false) }
    val enabled = onClick != null || (command != null && controller != null)

    Box(
        modifier = modifier
            .alpha(if (enabled) 1f else 0.3f)
            .clip(shape)
            .background(if (pressed) color.copy(alpha = 0.6f) else color)
            .pointerInput(command, onClick) {
                if (!enabled) return@pointerInput
                detectTapGestures(onPress = {
                    pressed = true
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    if (onClick != null) {
                        onClick()
                    } else if (command != null && controller != null) {
                        controller.send(command)
                    }
                    val repeatJob = if (repeatable && command != null && controller != null) {
                        scope.launch {
                            delay(450)
                            while (true) {
                                controller.send(command, isRepeat = true)
                                delay(160)
                            }
                        }
                    } else null
                    tryAwaitRelease()
                    repeatJob?.cancel()
                    pressed = false
                })
            },
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
