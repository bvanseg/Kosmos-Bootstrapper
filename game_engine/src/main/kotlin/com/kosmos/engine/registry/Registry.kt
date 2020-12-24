package com.kosmos.engine.registry

import java.util.concurrent.ConcurrentHashMap

/**
 * @author Boston Vanseghi
 * @since 1.0.0
 */
abstract class Registry<K, V: Any> {
    protected open val entries = ConcurrentHashMap<K, RegistryEntry<K, out V>>()

    val size: Int
        get() = entries.size
}