package com.kosmos.engine.network.client

import com.kosmos.engine.network.Side
import com.kosmos.engine.network.message.Message
import io.netty.channel.ChannelHandlerContext

import io.netty.channel.SimpleChannelInboundHandler
import io.netty.util.AttributeKey

class ClientHandler : SimpleChannelInboundHandler<Message>() {

    override fun channelActive(ctx: ChannelHandlerContext) {
        val sideAttribute = AttributeKey.newInstance<Side>("side")
        ctx.channel().attr(sideAttribute).set(Side.CLIENT)
    }

    override fun channelRead0(ctx: ChannelHandlerContext, msg: Message) {
        msg.handle(ctx.channel())
    }
}