package com.kosmos.engine.network.message.encode

import com.kosmos.engine.KosmosEngine
import com.kosmos.engine.network.message.Message
import com.kosmos.engine.network.message.MessageHeader
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.MessageToByteEncoder
import java.util.*

class MessageEncoder: MessageToByteEncoder<Message>() {

    override fun encode(ctx: ChannelHandlerContext, msg: Message, out: ByteBuf) {

        val engine = KosmosEngine.getInstance()

        val messageID = engine.messageRegistry.getIDFor(msg)

        val sampleBuf = Unpooled.buffer()

        msg.write(sampleBuf)

        val size = sampleBuf.capacity()

        val header = MessageHeader(UUID.randomUUID(), messageID, size)
        header.write(out)
        msg.write(out)
    }
}