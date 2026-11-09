package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.ui.theme.AegisVpnTheme
import com.example.vpn.engine.VpnController
import com.example.vpn.ui.navigation.AegisApp

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val controller = VpnController.getInstance(this)

        setContent {
            val settings by controller.settingsManager.settings.collectAsState()
            var currentTheme by remember(settings.themeMode) { mutableStateOf(settings.themeMode) }

            AegisVpnTheme(themePreference = currentTheme) {
                AegisApp(
                    controller = controller,
                    onThemeChanged = { newTheme ->
                        currentTheme = newTheme
                    }
                )
            }
        }
    }
}
