package com.kosmos.engine.network.message

import io.netty.buffer.ByteBuf
import java.util.*

/**
 * @author Boston Vanseghi
 * @since 1.0.0
 */
data class MessageHeader(
    val uuid: UUID,
    val messageID: Int,
    val messageSize: Int
) {
    companion object {
        const val HEADER_SIZE = Long.SIZE_BYTES * 2 + Int.SIZE_BYTES * 2
    }

    fun write(buffer: ByteBuf) {
        buffer.writeLong(uuid.mostSignificantBits)
        buffer.writeLong(uuid.leastSignificantBits)
        buffer.writeInt(messageID)
        buffer.writeInt(messageSize)
    }
}