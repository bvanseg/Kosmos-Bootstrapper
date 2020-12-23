package com.kosmos.engine.state

import com.kosmos.engine.KosmosEngine
import com.kosmos.engine.event.StateDisposeEvent
import com.kosmos.engine.event.StateInitEvent

/**
 * @author Boston Vanseghi
 * @since 1.0.0
 */
abstract class StateManager {

    var activeState: State = Stateless
        private set

    fun setState(state: State) {

        val engine = KosmosEngine.getInstance()

        engine.EVENT_BUS.fire(StateDisposeEvent.PRE(activeState))
        activeState.dispose()
        engine.EVENT_BUS.fire(StateDisposeEvent.POST(activeState))

        activeState = state

        engine.EVENT_BUS.fire(StateInitEvent.PRE(state))
        activeState.init()
        engine.EVENT_BUS.fire(StateInitEvent.POST(state))

    }
}