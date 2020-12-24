package com.kosmos.engine.event

import com.kosmos.engine.network.message.Message
import io.netty.channel.Channel

// CLIENT-SIDE

open class ClientEvent(val channel: Channel): KosmosEngineEvent()

open class ClientCloseEvent(channel: Channel): ClientEvent(channel) {
    class PRE(channel: Channel): ClientCloseEvent(channel)
    class POST(channel: Channel): ClientCloseEvent(channel)
}

open class ClientHandleMessageEvent(channel: Channel, val message: Message): ClientEvent(channel) {
    class PRE(channel: Channel, message: Message): ClientHandleMessageEvent(channel, message)
    class POST(channel: Channel, message: Message): ClientHandleMessageEvent(channel, message)
}


// SERVER-SIDE

open class ServerEvent(val channel: Channel): KosmosEngineEvent()

open class ServerCloseEvent(channel: Channel): ServerEvent(channel) {
    class PRE(channel: Channel): ServerCloseEvent(channel)
    class POST(channel: Channel): ServerCloseEvent(channel)
}

open class ServerHandleMessageEvent(channel: Channel, val message: Message): ServerEvent(channel) {
    class PRE(channel: Channel, message: Message): ServerHandleMessageEvent(channel, message)
    class POST(channel: Channel, message: Message): ServerHandleMessageEvent(channel, message)
}