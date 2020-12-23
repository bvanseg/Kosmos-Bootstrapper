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

        engine.eventBus.fire(StateDisposeEvent.PRE(activeState))
        activeState.dispose()
        engine.eventBus.fire(StateDisposeEvent.POST(activeState))

        activeState = state

        engine.eventBus.fire(StateInitEvent.PRE(state))
        activeState.init()
        engine.eventBus.fire(StateInitEvent.POST(state))

    }
}