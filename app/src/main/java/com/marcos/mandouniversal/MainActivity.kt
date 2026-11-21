package com.marcos.mandouniversal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.marcos.mandouniversal.data.Brand
import com.marcos.mandouniversal.data.Category
import com.marcos.mandouniversal.data.CodeDatabase
import com.marcos.mandouniversal.data.SavedRemote
import com.marcos.mandouniversal.ui.AcRemote
import com.marcos.mandouniversal.ui.Accent
import com.marcos.mandouniversal.ui.Background
import com.marcos.mandouniversal.ui.BrandListScreen
import com.marcos.mandouniversal.ui.HomeScreen
import com.marcos.mandouniversal.ui.MandoTheme
import com.marcos.mandouniversal.ui.MatchScreen
import com.marcos.mandouniversal.ui.RemoteActions
import com.marcos.mandouniversal.ui.RemoteFrame
import com.marcos.mandouniversal.ui.StandardRemote
import com.marcos.mandouniversal.ui.StoreModeScreen
import com.marcos.mandouniversal.ui.TextMain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val controller = RemoteController(applicationContext)
        setContent { MandoTheme { App(controller) } }
    }
}

/** Pantallas de la app. */
private sealed interface Screen {
    data object Home : Screen
    data class Brands(val category: Category) : Screen
    data class Match(val category: Category, val brand: String, val start: Int = 0, val replaceId: String? = null) : Screen
    data class Remote(val id: String) : Screen
    data class Store(val id: String) : Screen
}

@Composable
private fun App(controller: RemoteController) {
    val store = controller.saved
    var savedList by remember { mutableStateOf(store.all()) }
    val first = savedList.firstOrNull()
    // Al abrir la app se vuelve al último mando usado, como en Vivo
    var screen by remember { mutableStateOf<Screen>(if (first != null) Screen.Remote(first.id) else Screen.Home) }
    fun refresh() { savedList = store.all() }

    when (val s = screen) {
        Screen.Home -> HomeScreen(
            controller = controller,
            saved = savedList,
            onOpen = { screen = Screen.Remote(it.id) },
            onCategory = { screen = Screen.Brands(it) }
        )

        is Screen.Brands -> {
            BackHandler { screen = Screen.Home }
            BrandListScreen(
                category = s.category,
                onBack = { screen = Screen.Home },
                onBrand = { screen = Screen.Match(s.category, it.name) }
            )
        }

        is Screen.Match -> {
            val back = { screen = if (s.replaceId != null) Screen.Remote(s.replaceId) else Screen.Brands(s.category) }
            BackHandler(onBack = back)
            WithBrand(s.category, s.brand, onMissing = back) { brand ->
                MatchScreen(
                    controller = controller,
                    category = s.category,
                    brand = brand,
                    startIndex = s.start,
                    onBack = back,
                    onFound = { remote ->
                        val saved = store.save(s.category, brand.name, remote.model, s.replaceId)
                        refresh()
                        screen = Screen.Remote(saved.id)
                    }
                )
            }
        }

        is Screen.Remote -> {
            val saved = savedList.firstOrNull { it.id == s.id }
            if (saved == null) {
                screen = Screen.Home
                return
            }
            BackHandler { screen = Screen.Brands(saved.category) }
            LaunchedEffect(saved.id) { store.touch(saved.id) }
            RemoteScreen(controller, saved, onNavigate = { screen = it }, onChanged = { refresh() })
        }

        is Screen.Store -> {
            val saved = savedList.firstOrNull { it.id == s.id }
            if (saved == null) {
                screen = Screen.Home
                return
            }
            BackHandler { screen = Screen.Remote(saved.id) }
            WithBrand(saved.category, saved.brand, onMissing = { screen = Screen.Home }) { brand ->
                val remote = brand.remotes.firstOrNull { it.model == saved.model } ?: brand.remotes.first()
                StoreModeScreen(controller, brand.name, remote, onBack = { screen = Screen.Remote(saved.id) })
            }
        }
    }
}

@Composable
private fun RemoteScreen(
    controller: RemoteController,
    saved: SavedRemote,
    onNavigate: (Screen) -> Unit,
    onChanged: () -> Unit
) {
    val store = controller.saved
    WithBrand(saved.category, saved.brand, onMissing = { onNavigate(Screen.Brands(saved.category)) }) { brand ->
        val remote = brand.remotes.firstOrNull { it.model == saved.model }
        if (remote == null) {
            Missing("No encuentro el código guardado de este mando.", "Buscar de nuevo") {
                onNavigate(Screen.Match(saved.category, brand.name, 0, saved.id))
            }
            return@WithBrand
        }
        val actions = RemoteActions(
            onBack = { onNavigate(Screen.Brands(saved.category)) },
            onStoreMode = { onNavigate(Screen.Store(saved.id)) },
            onTryOther = {
                val next = (brand.remotes.indexOf(remote) + 1) % brand.remotes.size
                onNavigate(Screen.Match(saved.category, brand.name, next, saved.id))
            },
            onRename = { store.rename(saved.id, it); onChanged() },
            onDelete = { store.delete(saved.id); onChanged(); onNavigate(Screen.Home) }
        )
        RemoteFrame(controller, saved, brand, remote, actions) {
            if (saved.category == Category.AC) AcRemote(controller, remote, saved.id, brand.remotes.any { it.acProtocol != null })
            else StandardRemote(controller, remote)
        }
    }
}

/** Carga la marca en segundo plano y muestra [content] cuando está lista. */
@Composable
private fun WithBrand(category: Category, brandName: String, onMissing: () -> Unit, content: @Composable (Brand) -> Unit) {
    val context = LocalContext.current
    val state by produceState<Result<Brand?>?>(null, category, brandName) {
        value = withContext(Dispatchers.IO) { runCatching { CodeDatabase.find(context, category, brandName) } }
    }
    val result = state
    when {
        result == null -> Box(Modifier.fillMaxSize().background(Background), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Accent)
        }
        result.getOrNull() == null -> Missing("No se ha podido cargar la marca $brandName.", "Volver", onMissing)
        else -> content(result.getOrNull()!!)
    }
}

@Composable
private fun Missing(text: String, button: String, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Background)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
    ) {
        Text(text, color = TextMain)
        TextButton(onClick = onClick) { Text(button, color = Accent) }
    }
}
