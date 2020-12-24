package com.kosmos.engine.network.message

import io.netty.buffer.ByteBuf
import io.netty.channel.Channel

/**
 * @author Boston Vanseghi
 * @since 1.0.0
 */
abstract class Message {
    abstract fun read(buffer: ByteBuf)
    abstract fun write(buffer: ByteBuf)
    abstract fun handle(channel: Channel)
}