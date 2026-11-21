package com.marcos.mandouniversal.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marcos.mandouniversal.RemoteController
import com.marcos.mandouniversal.data.Category
import com.marcos.mandouniversal.data.SavedRemote

/** Menú principal: mis mandos guardados y tipos de aparato para añadir uno nuevo. */
@Composable
fun HomeScreen(
    controller: RemoteController,
    saved: List<SavedRemote>,
    onOpen: (SavedRemote) -> Unit,
    onCategory: (Category) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .statusBarsPadding()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(span = { GridItemSpan(2) }) {
            Column(Modifier.padding(top = 20.dp, bottom = 4.dp)) {
                Text("Mando Universal", color = TextMain, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                if (!controller.hasIrEmitter) NoIrWarning(Modifier.padding(top = 12.dp))
            }
        }

        if (saved.isNotEmpty()) {
            item(span = { GridItemSpan(2) }) { SectionTitle("Mis mandos") }
            items(saved, key = { it.id }, span = { GridItemSpan(2) }) { r ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Surface1)
                        .clickable { onOpen(r) }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Surface2),
                        contentAlignment = Alignment.Center
                    ) { Text(r.category.emoji, fontSize = 22.sp) }
                    Spacer(Modifier.size(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(r.name, color = TextMain, fontSize = 17.sp, fontWeight = FontWeight.SemiBold,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${r.category.title} · ${r.brand}", color = TextDim, fontSize = 13.sp,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Text("›", color = TextDim, fontSize = 26.sp)
                }
            }
        }

        item(span = { GridItemSpan(2) }) {
            SectionTitle(if (saved.isEmpty()) "¿Qué quieres controlar?" else "Añadir mando")
        }
        items(Category.entries, key = { it.id }) { cat ->
            Column(
                Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Surface1)
                    .clickable { onCategory(cat) }
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(cat.emoji, fontSize = 32.sp)
                Spacer(Modifier.height(8.dp))
                Text(cat.title, color = TextMain, fontSize = 14.sp, textAlign = TextAlign.Center,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        item(span = { GridItemSpan(2) }) {
            Text(
                "Códigos de la base de datos libre Flipper-IRDB.",
                color = TextDim, fontSize = 12.sp,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        }
    }
}
