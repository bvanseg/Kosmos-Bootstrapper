package com.kosmos.engine

import com.kosmos.bootstrapper.event.PluginInitializationEvent
import com.kosmos.engine.network.client.GameClient

fun main() {
    // Initialize engine
    KosmosEngine().init(PluginInitializationEvent())

    // Create client
    val client = GameClient()

    // Connect to server
    client.connect("127.0.0.1", 2323)
}