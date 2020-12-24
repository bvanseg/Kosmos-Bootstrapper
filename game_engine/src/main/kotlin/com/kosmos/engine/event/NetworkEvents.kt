package com.kosmos.engine.event

import com.kosmos.engine.network.message.Message
import io.netty.channel.Channel

// CLIENT-SIDE

open class ClientEvent: KosmosEngineEvent()

open class ClientConnectEvent: ClientEvent() {
    class PRE: ClientConnectEvent()
    class POST(val channel: Channel): ClientConnectEvent()
}

open class ClientCloseEvent(val channel: Channel): ClientEvent() {
    class PRE(channel: Channel): ClientCloseEvent(channel)
    class POST(channel: Channel): ClientCloseEvent(channel)
}

open class ClientHandleMessageEvent(val channel: Channel, val message: Message): ClientEvent() {
    class PRE(channel: Channel, message: Message): ClientHandleMessageEvent(channel, message)
    class POST(channel: Channel, message: Message): ClientHandleMessageEvent(channel, message)
}


// SERVER-SIDE

open class ServerEvent: KosmosEngineEvent()

open class ServerBindEvent: ServerEvent() {
    class PRE: ServerBindEvent()
    class POST(val channel: Channel): ServerBindEvent()
}

open class ServerCloseEvent(val channel: Channel): ServerEvent() {
    class PRE(channel: Channel): ServerCloseEvent(channel)
    class POST(channel: Channel): ServerCloseEvent(channel)
}

open class ServerHandleMessageEvent(val channel: Channel, val message: Message): ServerEvent() {
    class PRE(channel: Channel, message: Message): ServerHandleMessageEvent(channel, message)
    class POST(channel: Channel, message: Message): ServerHandleMessageEvent(channel, message)
}