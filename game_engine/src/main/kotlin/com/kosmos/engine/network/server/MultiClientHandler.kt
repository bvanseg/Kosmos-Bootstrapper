package com.kosmos.engine.network.server

import com.kosmos.engine.network.Side
import com.kosmos.engine.network.message.Message
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelId
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.util.AttributeKey
import java.util.*

/**
 * @author Boston Vanseghi
 * @since 1.0.0
 */
class MultiClientHandler: SimpleChannelInboundHandler<Message>() {

    val clients = hashMapOf<UUID, DummyClient>()

    /**
     * Fired when a client connects.
     */
    override fun channelActive(ctx: ChannelHandlerContext) {
        println("Client connected!")

        // Set a UUID for the client
        val uuidAttribute = AttributeKey.newInstance<UUID>("uuid")
        ctx.channel().attr(uuidAttribute).set(UUID.randomUUID())

        // Set the side that the channel knows
        val sideAttribute = AttributeKey.newInstance<Side>("side")
        ctx.channel().attr(sideAttribute).set(Side.SERVER)

        val dummyClient = DummyClient(ctx.channel())

        clients[ctx.channel().attr(uuidAttribute).get()] = dummyClient
    }

    /**
     * Fired when a message is received from the client.
     */
    override fun channelRead0(ctx: ChannelHandlerContext, msg: Message) {
        msg.handle(ctx.channel())
    }

    /**
     * Handles exceptions from the client.
     */
    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        ctx.close()
    }
}