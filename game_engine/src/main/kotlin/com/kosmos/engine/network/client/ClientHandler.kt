package com.kosmos.engine.network.client

import com.kosmos.engine.KosmosEngine
import com.kosmos.engine.event.ClientHandleMessageEvent
import com.kosmos.engine.network.Side
import com.kosmos.engine.network.message.Message
import io.netty.channel.ChannelHandlerContext

import io.netty.channel.SimpleChannelInboundHandler
import io.netty.util.AttributeKey

class ClientHandler : SimpleChannelInboundHandler<Message>() {

    override fun channelActive(ctx: ChannelHandlerContext) {
        val sideAttribute = AttributeKey.valueOf<Side>("side")
        ctx.channel().attr(sideAttribute).set(Side.CLIENT)
    }

    override fun channelRead0(ctx: ChannelHandlerContext, msg: Message) {
        val engine = KosmosEngine.getInstance()

        engine.eventBus.fire(ClientHandleMessageEvent.PRE(ctx.channel(), msg))
        msg.handle(ctx.channel())
        engine.eventBus.fire(ClientHandleMessageEvent.POST(ctx.channel(), msg))
    }
}