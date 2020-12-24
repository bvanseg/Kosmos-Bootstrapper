package com.kosmos.engine.network.util

import io.netty.buffer.ByteBuf
import java.util.*

fun ByteBuf.writeUUID(uuid: UUID) {
    this.writeLong(uuid.mostSignificantBits)
    this.writeLong(uuid.leastSignificantBits)
}

fun ByteBuf.readUUID(): UUID = UUID(this.readLong(), this.readLong())