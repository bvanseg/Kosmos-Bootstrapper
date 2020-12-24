package com.kosmos.engine.network.server

import bvanseg.kotlincommons.any.getLogger
import com.kosmos.engine.network.Side
import com.kosmos.engine.network.message.Message
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.util.AttributeKey
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * @author Boston Vanseghi
 * @since 1.0.0
 */
class MultiClientHandler: SimpleChannelInboundHandler<Message>() {

    private val clients = ConcurrentHashMap<UUID, DummyClient>()

    private val logger = getLogger()

    /**
     * Fired when a client connects.
     */
    override fun channelActive(ctx: ChannelHandlerContext) {
        logger.info("Client connected: ${ctx.channel().id().asLongText()}")

        // Set a UUID for the client
        val uuidAttributeKey = AttributeKey.newInstance<UUID>("uuid")
        ctx.channel().attr(uuidAttributeKey).set(UUID.randomUUID())

        // Set the side that the channel knows
        val sideAttributeKey = AttributeKey.newInstance<Side>("side")
        ctx.channel().attr(sideAttributeKey).set(Side.SERVER)

        val dummyClient = DummyClient(ctx.channel())

        clients[ctx.channel().attr(uuidAttributeKey).get()] = dummyClient
    }

    override fun channelInactive(ctx: ChannelHandlerContext) {
        logger.info("Client disconnected: ${ctx.channel().id().asLongText()}")

        val uuidAttributeKey = AttributeKey.newInstance<UUID>("uuid")
        val uuid = ctx.channel().attr(uuidAttributeKey).get()

        clients.remove(uuid)
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
        cause.printStackTrace()
        ctx.close()
    }
}