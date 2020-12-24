package com.kosmos.engine.network.message.impl

import com.kosmos.engine.network.Side
import com.kosmos.engine.network.message.Message
import com.kosmos.engine.network.util.readUUID
import com.kosmos.engine.network.util.writeUUID
import io.netty.buffer.ByteBuf
import io.netty.channel.Channel
import io.netty.util.AttributeKey
import java.util.*

/**
 * @author Boston Vanseghi
 * @since 1.0.0
 */
class ClientInitMessage: Message() {

    lateinit var uuid: UUID

    override fun write(buffer: ByteBuf) {
        buffer.writeUUID(uuid)
    }

    override fun read(buffer: ByteBuf) {
        uuid = buffer.readUUID()
    }

    override fun handle(channel: Channel) {
        getSide(channel)?.let { side ->
            if (side == Side.CLIENT) {
                val uuidAttributeKey = AttributeKey.valueOf<UUID>("uuid")
                channel.attr(uuidAttributeKey).set(uuid)
                logger.info("Client UUID established by server: $uuid")
            }
        }
    }
}