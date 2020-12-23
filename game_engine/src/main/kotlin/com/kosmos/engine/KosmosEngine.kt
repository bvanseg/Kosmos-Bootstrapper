package com.kosmos.engine

import com.kosmos.bootstrapper.event.PluginInitializationEvent
import com.kosmos.bootstrapper.plugin.Plugin
import com.kosmos.bootstrapper.plugin.PluginLoader

@Plugin(
    version = "1.0.0",
    name = "Kosmos Engine",
    domain = "kosmos_engine"
)
class KosmosEngine {

    init {
        PluginLoader.EVENT_BUS.addListener(this)
    }

    private fun init(event: PluginInitializationEvent) {
        // TODO:
    }
}