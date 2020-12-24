package com.kosmos.sample

import bvanseg.kotlincommons.any.getLogger
import bvanseg.kotlincommons.evenir.annotation.SubscribeEvent
import com.kosmos.bootstrapper.event.PluginInitializationEvent
import com.kosmos.bootstrapper.plugin.Plugin
import com.kosmos.bootstrapper.plugin.PluginManager

/**
 * @author Boston Vanseghi
 * @since 1.0.0
 */
@Plugin("1.0.0", "Game Plugin", "game", dependencies = ["game_engine"])
class GamePlugin {

    private val logger = getLogger()

    init {
        PluginManager.EVENT_BUS.addListener(this)
    }

    @SubscribeEvent
    private fun init(e: PluginInitializationEvent) {
        logger.debug("Initializing Game plugin...")
    }
}