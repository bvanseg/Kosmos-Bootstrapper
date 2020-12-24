package com.kosmos.engine.network.message.impl

import com.kosmos.engine.network.Side
import com.kosmos.engine.network.message.Message
import io.netty.buffer.ByteBuf
import io.netty.channel.Channel

/**
 * @author Boston Vanseghi
 * @since 1.0.0
 */
class PingMessage: Message() {

    var timestamp: Long = 0L

    override fun write(buffer: ByteBuf) {
        buffer.writeLong(System.currentTimeMillis())
    }

    override fun read(buffer: ByteBuf) {
        timestamp = buffer.readLong()
    }

    override fun handle(channel: Channel) {
        getSide(channel)?.let { side ->
            when (side) {
                Side.CLIENT -> {
                    logger.debug("Ping: ${System.currentTimeMillis() - timestamp}ms")
                }
                Side.SERVER -> {
                    logger.debug("Server side received ping from client ${channel.id().asLongText()}. Echoing...")
                    channel.writeAndFlush(this)
                }
            }
        }
    }
}