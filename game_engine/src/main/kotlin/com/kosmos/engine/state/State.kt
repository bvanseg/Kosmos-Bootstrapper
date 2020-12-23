package com.kosmos.engine.state

/**
 * @author Boston Vanseghi
 * @since 1.0.0
 */
interface State {
    fun init()
    fun update()
    fun dispose()
}