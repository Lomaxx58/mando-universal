package com.marcos.mandouniversal.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marcos.mandouniversal.RemoteController
import com.marcos.mandouniversal.data.Signal
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Símbolo de encendido estándar (IEC 5009), el de todos los mandos. */
val PowerIcon: ImageVector = ImageVector.Builder(
    name = "Power", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f
).addPath(
    pathData = addPathNodes(
        "M13,3h-2v10h2L13,3zM17.83,5.17l-1.42,1.42C17.99,7.86 19,9.81 19,12c0,3.87 -3.13,7 -7,7" +
            "s-7,-3.13 -7,-7c0,-2.19 1.01,-4.14 2.58,-5.42L6.17,5.17C4.23,6.82 3,9.26 3,12" +
            "c0,4.97 4.03,9 9,9s9,-4.03 9,-9c0,-2.74 -1.23,-5.18 -3.17,-6.83z"
    ),
    fill = SolidColor(Color.White)
).build()

/**
 * Barra superior: flecha atrás a la izquierda, marca centrada (con subtítulo)
 * y, si se pasa [onMenu], los tres puntos a la derecha.
 */
@Composable
fun TopBar(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    onMenu: (() -> Unit)? = null,
    menuContent: @Composable () -> Unit = {}
) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(60.dp)
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Atrás", tint = TextMain)
            }
        }
        Column(
            Modifier
                .align(Alignment.Center)
                .padding(horizontal = 56.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, color = TextMain, fontSize = 21.sp, fontWeight = FontWeight.Bold,
                maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
            if (!subtitle.isNullOrBlank()) {
                Text(subtitle, color = TextDim, fontSize = 12.sp, maxLines = 1,
                    overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
            }
        }
        if (onMenu != null) {
            Box(Modifier.align(Alignment.CenterEnd)) {
                IconButton(onClick = onMenu) {
                    Icon(Icons.Filled.MoreVert, "Más opciones", tint = TextMain)
                }
                menuContent()
            }
        }
    }
}

@Composable
fun NoIrWarning(modifier: Modifier = Modifier) {
    Text(
        "Este móvil no tiene emisor de infrarrojos, así que no podrá controlar los aparatos " +
            "(funciona en Xiaomi, Redmi, POCO, Vivo y algunos Honor y Huawei).",
        color = Color.Black,
        fontSize = 13.sp,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Accent)
            .padding(12.dp)
    )
}

/**
 * Botón del mando. Envía al pulsar, vibra, y si [repeatable] repite mientras
 * se mantiene pulsado. Se ve atenuado si el mando no tiene esa tecla.
 */
@Composable
fun RemoteButton(
    controller: RemoteController?,
    signal: Signal?,
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
    val enabled = onClick != null || (signal != null && controller != null)

    Box(
        modifier = modifier
            .alpha(if (enabled) 1f else 0.28f)
            .clip(shape)
            .background(if (pressed) color.copy(alpha = 0.55f) else color)
            .pointerInput(signal, onClick, enabled) {
                if (!enabled) return@pointerInput
                detectTapGestures(onPress = {
                    pressed = true
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    if (onClick != null) onClick()
                    else if (signal != null && controller != null) controller.send(signal)
                    val job = if (repeatable && signal != null && controller != null) {
                        scope.launch {
                            delay(450)
                            while (true) {
                                controller.send(signal, isRepeat = true)
                                delay(180)
                            }
                        }
                    } else null
                    tryAwaitRelease()
                    job?.cancel()
                    pressed = false
                })
            },
        contentAlignment = Alignment.Center
    ) { content() }
}

/** Botón de encendido redondo y rojo con el símbolo estándar. */
@Composable
fun PowerButton(controller: RemoteController, signal: Signal?, size: Int = 64) {
    RemoteButton(controller, signal, Modifier.size(size.dp), androidx.compose.foundation.shape.CircleShape, PowerRed) {
        Icon(PowerIcon, "Encender / apagar", tint = Color.White, modifier = Modifier.size((size * 0.48f).dp))
    }
}

/** Texto de una tecla sin nombre estándar: "vol_up" -> "Vol up". */
fun prettyLabel(label: String): String {
    val s = label.replace('_', ' ').trim()
    return s.replaceFirstChar { it.uppercase() }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().padding(top = 8.dp, bottom = 6.dp)) {
        Text(text.uppercase(), color = TextDim, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
    }
}
