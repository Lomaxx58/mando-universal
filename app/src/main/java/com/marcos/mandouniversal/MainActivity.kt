package com.marcos.mandouniversal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.marcos.mandouniversal.data.Brand
import com.marcos.mandouniversal.data.CodeSet
import com.marcos.mandouniversal.ui.BrandScreen
import com.marcos.mandouniversal.ui.MandoTheme
import com.marcos.mandouniversal.ui.RemoteScreen
import com.marcos.mandouniversal.ui.StoreModeScreen

class MainActivity : ComponentActivity() {

    private lateinit var controller: RemoteController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        controller = RemoteController(applicationContext)
        setContent {
            MandoTheme { App(controller) }
        }
    }
}

private enum class Screen { BRANDS, REMOTE, STORE }

@Composable
private fun App(controller: RemoteController) {
    val saved = remember { controller.savedBrand() }
    var brand by remember { mutableStateOf<Brand?>(saved) }
    var codeSet by remember { mutableStateOf<CodeSet?>(saved?.let { controller.savedCodeSet(it) }) }
    var screen by remember { mutableStateOf(if (saved == null) Screen.BRANDS else Screen.REMOTE) }

    val currentBrand = brand
    val currentSet = codeSet

    if (screen == Screen.BRANDS || currentBrand == null || currentSet == null) {
        BackHandler(enabled = currentBrand != null) { screen = Screen.REMOTE }
        BrandScreen(
            controller = controller,
            selectedBrand = currentBrand,
            selectedSet = currentSet,
            onSelected = { b, s ->
                brand = b; codeSet = s
                controller.save(b, s)
                screen = Screen.REMOTE
            }
        )
        return
    }

    when (screen) {
        Screen.STORE -> {
            BackHandler { screen = Screen.REMOTE }
            StoreModeScreen(
                controller = controller,
                brand = currentBrand,
                codeSet = currentSet,
                onBack = { screen = Screen.REMOTE }
            )
        }
        else -> RemoteScreen(
            controller = controller,
            brand = currentBrand,
            codeSet = currentSet,
            onChangeBrand = { screen = Screen.BRANDS },
            onStoreMode = { screen = Screen.STORE }
        )
    }
}
