package com.kosmos.game

import com.kosmos.bootstrapper.event.PluginInitializationEvent
import com.kosmos.bootstrapper.plugin.Plugin
import com.kosmos.bootstrapper.plugin.PluginManager

@Plugin(
    version = "1.0.0",
    name = "Zerra",
    domain = "zerra",
    dependencies = ["kosmos_engine"]
)
class ZerraPlugin {

    init {
        PluginManager.EVENT_BUS.addListener(this)
    }

    private fun init(event: PluginInitializationEvent) {
        // TODO: Create a client or server depending on bootstrapper args
    }
}