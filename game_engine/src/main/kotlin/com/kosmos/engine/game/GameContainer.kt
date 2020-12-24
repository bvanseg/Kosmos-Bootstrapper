package com.kosmos.engine.game

import com.kosmos.engine.network.Side

/**
 * @author Boston Vanseghi
 * @since 1.0.0
 */
abstract class GameContainer {

    /**
     * The [Side] of the [GameContainer], whether it is Client or Server.
     */
    protected val localSide = InheritableThreadLocal<Side>()

    fun getSide(): Side = localSide.get()

    abstract fun init()
    abstract fun update()
    abstract fun dispose()
}