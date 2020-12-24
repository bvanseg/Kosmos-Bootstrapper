package com.kosmos.engine.network.server

import com.kosmos.engine.network.message.Message
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler

class MultiClientHandler: SimpleChannelInboundHandler<Message>() {

    /**
     * Fired when a client connects.
     */
    override fun channelActive(ctx: ChannelHandlerContext) {
        println("Client connected!")
    }

    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        println("Message received!")
        val future = ctx.writeAndFlush(msg)
        (msg as Message).handle()
    }

    /**
     * Fired when a message is received from the client.
     */
    override fun channelRead0(ctx: ChannelHandlerContext, msg: Message) {
        println("Message received!")
        val future = ctx.writeAndFlush(msg)
        msg.handle()
    }

    /**
     * Handles exceptions from the client.
     */
    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        ctx.close()
    }
}