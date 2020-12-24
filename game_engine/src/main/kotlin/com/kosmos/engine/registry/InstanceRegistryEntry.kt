package com.kosmos.engine.registry

/**
 * @author Boston Vanseghi
 * @since 1.0.0
 */
open class InstanceRegistryEntry<K, V: Any>(instanceRegistry: InstanceRegistry<K, V>, val value: V): RegistryEntry<K, V>(instanceRegistry)