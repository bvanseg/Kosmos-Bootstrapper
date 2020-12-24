package com.kosmos.engine

import bvanseg.kotlincommons.any.getLogger
import bvanseg.kotlincommons.evenir.bus.EventBus
import com.kosmos.bootstrapper.event.PluginInitializationEvent
import com.kosmos.bootstrapper.plugin.Plugin
import com.kosmos.bootstrapper.plugin.PluginManager

/**
 * @author Boston Vanseghi
 * @author Ocelot5836
 * @since 1.0.0
 */
@Plugin(
    version = "1.0.0",
    name = "Kosmos Engine",
    domain = "kosmos_engine"
)
class KosmosEngine {

    init {
        PluginManager.EVENT_BUS.addListener(this)
    }

    companion object {
        private lateinit var instance: KosmosEngine

        fun getInstance() = instance
    }

    /**
     * Primary event bus for the game engine.
     */
    val eventBus = EventBus()

    /**
     * Primary logger for the game engine.
     */
    val logger = getLogger()

    private fun init(event: PluginInitializationEvent) {
        logger.info("Initializing Kosmos Engine...")
        val start = System.currentTimeMillis()
        instance = this // Allows plugins dependent on the engine to grab the engine instance.

        logger.info("Finished initializing Kosmos Engine in ${System.currentTimeMillis() - start}ms")
    }
}