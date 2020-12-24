package com.kosmos.engine.network.message

import io.netty.buffer.ByteBuf

class PingMessage: Message() {

    var timestamp: Long = 0L

    override fun write(buffer: ByteBuf) {
        buffer.writeLong(System.currentTimeMillis())
    }

    override fun read(buffer: ByteBuf) {
        timestamp = buffer.readLong()
    }

    override fun handle() {
        println("Ping!")
    }
}