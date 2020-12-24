package com.kosmos.engine

import bvanseg.kotlincommons.evenir.annotation.SubscribeEvent
import com.kosmos.engine.event.ServerBindEvent
import kotlin.system.exitProcess

object ServerListener {

    @SubscribeEvent
    fun onServerBind(serverBindEvent: ServerBindEvent.POST) {
        println("Received server bind event! Shutting down...")
        exitProcess(0)
    }
}