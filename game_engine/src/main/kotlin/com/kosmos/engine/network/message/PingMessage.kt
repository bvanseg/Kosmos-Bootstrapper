package com.kosmos.engine.network.message

import com.kosmos.engine.network.Side
import io.netty.buffer.ByteBuf
import io.netty.channel.Channel
import io.netty.util.AttributeKey

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
        val sideAttributeKey = AttributeKey.valueOf<Side>("side")
        if (channel.hasAttr(sideAttributeKey)) {
            when (channel.attr(sideAttributeKey).get()!!) {
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