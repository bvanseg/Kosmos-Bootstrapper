package com.kosmos.engine.network.message

import bvanseg.kotlincommons.any.getLogger
import com.kosmos.engine.network.Side
import io.netty.buffer.ByteBuf
import io.netty.channel.Channel
import io.netty.util.AttributeKey

/**
 * @author Boston Vanseghi
 * @since 1.0.0
 */
abstract class Message {

    lateinit var header: MessageHeader

    val logger = getLogger()

    abstract fun read(buffer: ByteBuf)
    abstract fun write(buffer: ByteBuf)
    abstract fun handle(channel: Channel)

    fun getSide(channel: Channel): Side? {
        val sideAttributeKey = AttributeKey.valueOf<Side>("side")
        return channel.attr(sideAttributeKey).get()
    }
}