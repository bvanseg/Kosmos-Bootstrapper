package com.kosmos.engine.network.server

import bvanseg.kotlincommons.any.getLogger
import com.kosmos.engine.network.Side
import com.kosmos.engine.network.message.Message
import com.kosmos.engine.network.message.impl.ClientInitMessage
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

        // Set a UUID for the client.
        val uuidAttributeKey = AttributeKey.valueOf<UUID>("uuid")
        ctx.channel().attr(uuidAttributeKey).set(UUID.randomUUID())
        val clientUUID = ctx.channel().attr(uuidAttributeKey).get()

        logger.info("Client connected: $clientUUID")

        // Set the side that the channel knows.
        val sideAttributeKey = AttributeKey.valueOf<Side>("side")
        ctx.channel().attr(sideAttributeKey).set(Side.SERVER)

        // Track the client within our map.
        val dummyClient = DummyClient(ctx.channel())
        clients[clientUUID] = dummyClient

        // Initialize the client with the UUID we assign it.
        val clientInitMessage = ClientInitMessage()
        clientInitMessage.uuid = clientUUID
        ctx.writeAndFlush(clientInitMessage)
    }

    override fun channelInactive(ctx: ChannelHandlerContext) {
        val uuidAttributeKey = AttributeKey.valueOf<UUID>("uuid")
        val uuid = ctx.channel().attr(uuidAttributeKey).get()

        logger.info("Client disconnected: $uuid")

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