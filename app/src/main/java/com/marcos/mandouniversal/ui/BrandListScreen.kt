package com.marcos.mandouniversal.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marcos.mandouniversal.data.Brand
import com.marcos.mandouniversal.data.Category
import com.marcos.mandouniversal.data.CodeDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Lista de marcas de un tipo de aparato, con buscador y las más comunes arriba. */
@Composable
fun BrandListScreen(category: Category, onBack: () -> Unit, onBrand: (Brand) -> Unit) {
    val context = LocalContext.current
    val brands by produceState<List<Brand>?>(null, category) {
        value = withContext(Dispatchers.IO) { CodeDatabase.brands(context, category) }
    }
    var query by rememberSaveable { mutableStateOf("") }

    Column(
        Modifier
            .fillMaxSize()
            .background(Background)
            .statusBarsPadding()
    ) {
        TopBar(title = category.title, subtitle = "Elige la marca", onBack = onBack)

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Buscar marca", color = TextDim) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextMain, unfocusedTextColor = TextMain,
                focusedBorderColor = Accent, unfocusedBorderColor = Surface2, cursorColor = Accent
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
        )

        val list = brands
        if (list == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Accent)
            }
            return@Column
        }

        val q = query.trim()
        val filtered = if (q.isEmpty()) list else list.filter { it.name.contains(q, ignoreCase = true) }
        val popularNames = CodeDatabase.popular[category].orEmpty()
        val popular = if (q.isEmpty()) {
            popularNames.mapNotNull { n -> list.firstOrNull { it.name.equals(n, ignoreCase = true) } }
        } else emptyList()

        LazyColumn(Modifier.padding(horizontal = 16.dp)) {
            if (popular.isNotEmpty()) {
                item { SectionTitle("Más comunes", Modifier.padding(top = 8.dp)) }
                items(popular, key = { "p_" + it.name }) { BrandRow(it, onBrand) }
                item { SectionTitle("Todas las marcas", Modifier.padding(top = 12.dp)) }
            }
            items(filtered, key = { it.name }) { BrandRow(it, onBrand) }
            if (filtered.isEmpty()) {
                item {
                    Text("No hay ninguna marca con ese nombre.", color = TextDim,
                        modifier = Modifier.padding(vertical = 24.dp))
                }
            }
        }
    }
}

@Composable
private fun BrandRow(brand: Brand, onBrand: (Brand) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Surface1)
            .clickable { onBrand(brand) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(brand.name, color = TextMain, fontSize = 16.sp, fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f))
        val n = brand.remotes.size
        Text(if (n == 1) "1 código" else "$n códigos", color = TextDim, fontSize = 13.sp)
    }
}
