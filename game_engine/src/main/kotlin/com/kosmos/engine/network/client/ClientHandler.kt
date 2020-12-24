package com.kosmos.engine.network.client

import com.kosmos.engine.network.message.Message
import io.netty.channel.ChannelHandlerContext

import io.netty.channel.SimpleChannelInboundHandler

class ClientHandler : SimpleChannelInboundHandler<Message>() {

    override fun channelRead0(ctx: ChannelHandlerContext, msg: Message) {
        println("Message: $msg")
    }
}