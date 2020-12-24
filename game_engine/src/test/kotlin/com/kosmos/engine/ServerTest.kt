package com.kosmos.engine

import com.kosmos.bootstrapper.event.PluginInitializationEvent
import com.kosmos.engine.network.server.GameServer

/**
 * @author Boston Vanseghi
 * @since 1.0.0
 */

fun main() {
    KosmosEngine().init(PluginInitializationEvent())
    val server = GameServer()
    server.bind("127.0.0.1", 2323)
}