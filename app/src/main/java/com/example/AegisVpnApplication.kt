package com.example

import android.app.Application
import com.example.vpn.engine.VpnController

class AegisVpnApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val controller = VpnController.getInstance(this)

        // Check auto-connect on launch
        if (controller.settingsManager.settings.value.autoConnectOnLaunch) {
            val active = controller.activeProfile.value
            if (active != null) {
                controller.connect(active)
            }
        }
    }
}
