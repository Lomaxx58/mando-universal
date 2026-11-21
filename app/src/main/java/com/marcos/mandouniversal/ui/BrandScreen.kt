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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marcos.mandouniversal.RemoteController
import com.marcos.mandouniversal.data.Brand
import com.marcos.mandouniversal.data.Brands
import com.marcos.mandouniversal.data.CodeSet
import com.marcos.mandouniversal.data.RemoteKey

/**
 * Elegir marca. Cada marca muestra sus juegos de códigos con un botón para
 * probar el encendido antes de usarlo.
 */
@Composable
fun BrandScreen(
    controller: RemoteController,
    selectedBrand: Brand?,
    selectedSet: CodeSet?,
    onSelected: (Brand, CodeSet) -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(selectedBrand?.id) }

    Column(
        Modifier
            .fillMaxSize()
            .background(Background)
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Text("Elige la marca", color = TextMain, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Text(
            "Apunta el móvil a la tele y pulsa \"Probar\". Si se enciende o apaga, pulsa \"Usar\".",
            color = TextDim, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp)
        )
        if (!controller.hasIrEmitter) NoIrWarning(Modifier.padding(top = 12.dp))
        Spacer(Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(Brands.all, key = { it.id }) { brand ->
                val isOpen = expanded == brand.id
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Surface1)
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { expanded = if (isOpen) null else brand.id }
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(brand.name, color = TextMain, fontSize = 19.sp, fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f))
                        if (brand.id == selectedBrand?.id) {
                            Text("En uso", color = Accent, fontSize = 13.sp)
                        }
                    }
                    if (isOpen) {
                        brand.codeSets.forEach { set ->
                            CodeSetRow(
                                controller = controller,
                                set = set,
                                inUse = set == selectedSet,
                                onUse = { onSelected(brand, set) }
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
            item {
                Text(
                    "¿Falta tu marca? Se pueden añadir más códigos en data/Brands.kt.",
                    color = TextDim, fontSize = 13.sp, modifier = Modifier.padding(vertical = 12.dp)
                )
            }
        }
    }
}

@Composable
private fun CodeSetRow(controller: RemoteController, set: CodeSet, inUse: Boolean, onUse: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(set.name, color = if (inUse) Accent else TextMain, fontSize = 15.sp, modifier = Modifier.weight(1f))
        RemoteButton(
            controller = controller,
            command = set.codes[RemoteKey.POWER],
            modifier = Modifier.size(width = 84.dp, height = 40.dp),
            shape = RoundedCornerShape(12.dp),
            color = Surface2
        ) {
            Text("Probar", color = TextMain, fontSize = 14.sp)
        }
        Spacer(Modifier.size(8.dp))
        Button(
            onClick = onUse,
            colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Color.Black),
            shape = RoundedCornerShape(12.dp)
        ) { Text("Usar") }
    }
}
