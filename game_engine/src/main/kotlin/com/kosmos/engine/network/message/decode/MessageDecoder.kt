package com.kosmos.engine.network.message.decode

import bvanseg.kotlincommons.any.getLogger
import com.kosmos.engine.KosmosEngine
import com.kosmos.engine.network.message.Message
import com.kosmos.engine.network.message.MessageHeader
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.ReplayingDecoder
import java.util.*

class MessageDecoder: ReplayingDecoder<Message>() {

    val logger = getLogger()

    override fun decode(ctx: ChannelHandlerContext, input: ByteBuf, out: MutableList<Any>) {
        val engine = KosmosEngine.getInstance()

        val messageHeader = MessageHeader(UUID(input.readLong(), input.readLong()), input.readInt(), input.readInt())

        val factoryEntry = engine.messageRegistry.getEntryByID(messageHeader.messageID)

        if (factoryEntry == null) {
            logger.warn("Failed to find factory entry for message with id ${messageHeader.messageID}")
            return
        }

        val message = factoryEntry.createInstance()

        if (message == null) {
            logger.warn("Failed to create message instance from factory entry for message with id ${messageHeader.messageID}")
            return
        }

        message.read(input)

        out.add(message)
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        cause.printStackTrace()
    }
}