package com.kosmos.sample

import bvanseg.kotlincommons.any.getLogger
import bvanseg.kotlincommons.evenir.annotation.SubscribeEvent
import com.kosmos.bootstrapper.event.PluginInitializationEvent
import com.kosmos.bootstrapper.plugin.Plugin
import com.kosmos.bootstrapper.plugin.PluginLoader

/**
 * @author Boston Vanseghi
 * @since 1.0.0
 */
@Plugin("1.0.0", "Game Engine Plugin", "game_engine")
class GameEnginePlugin {

    private val logger = getLogger()

    init {
        PluginLoader.EVENT_BUS.addListener(this)
    }

    @SubscribeEvent
    private fun init(e: PluginInitializationEvent) {
        logger.debug("Initializing Game Engine plugin...")
        Thread.sleep(2000)
    }
}