package com.kosmos.engine.network.server

import io.netty.channel.Channel
import java.util.*

/**
 * Represents a client as the server sees it.
 *
 * @param uuid The [UUID] representing the client.
 *
 * @author Boston Vanseghi
 * @since 1.0.0
 */
class DummyClient(val channel: Channel) {

}