package com.kosmos.engine.network.client

import com.kosmos.engine.network.message.PingMessage
import com.kosmos.engine.network.message.decode.MessageDecoder
import com.kosmos.engine.network.message.encode.MessageEncoder
import io.netty.bootstrap.Bootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelInitializer
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import java.net.InetSocketAddress
import java.util.*

/**
 * @author Boston Vanseghi
 * @since 1.0.0
 */
class GameClient {

    private val group = NioEventLoopGroup()

    fun connect(host: String, port: Int) {

        val scanner = Scanner(System.`in`)

        try {
            val bootstrap = Bootstrap()

            // Set groups for server bootstrap.
            bootstrap.group(group)
                .channel(NioSocketChannel::class.java)
                .handler(object: ChannelInitializer<SocketChannel>() {
                    override fun initChannel(channel: SocketChannel) {
                        val pipeline = channel.pipeline()
                        pipeline.addLast(MessageDecoder())
                        pipeline.addLast(MessageEncoder())
                        pipeline.addLast(ClientHandler())
                    }

                })

            val channelFuture: ChannelFuture = bootstrap.connect(InetSocketAddress(host, port)).sync()
            println("Chat Server started. Ready to accept chat clients.")

            while(true) {
                if (System.currentTimeMillis() % 20 == 0L) {
                    val channel: Channel = channelFuture.sync().channel()
                    channel.writeAndFlush(PingMessage())
                }
            }

            // Wait until the server socket is closed.
            channelFuture.channel().closeFuture().sync()
        } finally {
            group.shutdownGracefully()
        }
    }
}