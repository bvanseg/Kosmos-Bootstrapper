package com.kosmos.engine

import bvanseg.kotlincommons.any.getLogger
import bvanseg.kotlincommons.evenir.bus.EventBus
import com.kosmos.bootstrapper.event.PluginInitializationEvent
import com.kosmos.bootstrapper.plugin.Plugin
import com.kosmos.bootstrapper.plugin.PluginManager
import com.kosmos.engine.network.message.Message
import com.kosmos.engine.network.message.impl.PingMessage
import com.kosmos.engine.registry.RegistryManager

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
    private val logger = getLogger()

    private val registryManager = RegistryManager()
    val messageRegistry = registryManager.addFactoryRegistry<Message>()

    fun init(event: PluginInitializationEvent) {
        logger.info("Initializing Kosmos Engine...")
        val start = System.currentTimeMillis()
        instance = this // Allows plugins dependent on the engine to grab the engine instance.

        messageRegistry.register(PingMessage::class)

        logger.info("Finished initializing Kosmos Engine in ${System.currentTimeMillis() - start}ms")
    }
}