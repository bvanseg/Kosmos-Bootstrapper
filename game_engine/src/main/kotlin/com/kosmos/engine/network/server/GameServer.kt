package com.kosmos.engine.network.server

import com.kosmos.engine.network.message.decode.MessageDecoder
import com.kosmos.engine.network.message.encode.MessageEncoder
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.ChannelInitializer
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.channel.ChannelFuture
import java.net.InetSocketAddress

/**
 * @author Boston Vanseghi
 * @since 1.0.0
 */
class GameServer {

    // Accepts connections from clients
    private val bossGroup = NioEventLoopGroup(1)

    // Worker group for actually managing clients.
    private val workerGroup = NioEventLoopGroup()

    fun bind(host: String, port: Int) {
        try {
            val bootstrap = ServerBootstrap()

            // Set groups for server bootstrap.
            bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel::class.java)
                .childHandler(object: ChannelInitializer<SocketChannel>() {
                    override fun initChannel(channel: SocketChannel) {
                        val pipeline = channel.pipeline()
                        pipeline.addLast(MessageDecoder())
                        pipeline.addLast(MessageEncoder())
                        pipeline.addLast(MultiClientHandler())
                    }

                })

            val channelFuture: ChannelFuture = bootstrap.bind(InetSocketAddress(host, port)).sync()
            println("Chat Server started. Ready to accept chat clients.")

            // Wait until the server socket is closed.
            channelFuture.channel().closeFuture().sync()
        } finally {
            bossGroup.shutdownGracefully()
            workerGroup.shutdownGracefully()
        }
    }
}