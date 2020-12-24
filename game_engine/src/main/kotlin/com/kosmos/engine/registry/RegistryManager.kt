package com.kosmos.engine.registry

import bvanseg.kotlincommons.any.getLogger
import bvanseg.kotlincommons.javaclass.createNewInstance
import kotlin.reflect.KClass

/**
 * @author Boston Vanseghi
 * @since 1.0.0
 */
open class RegistryManager {

    val logger = getLogger()

    private var hasInitialized = false

    @PublishedApi
    internal val registries = hashMapOf<KClass<*>, Registry<*, *>>()

    inline fun <reified V : Any> addClassInstanceRegistry(): InstanceRegistry<KClass<out V>, V> = addInstanceRegistry()

    inline fun <reified K, reified V : Any> addInstanceRegistry(): InstanceRegistry<K, V> {
        logger.info("Creating new registry for object of type: ${V::class}")
        val newRegistry = InstanceRegistry<K, V>()
        registries[V::class] = newRegistry
        return newRegistry
    }

    inline fun <reified T : Any> addFactoryRegistry(noinline factory: (FactoryRegistryEntry<T>) -> T? = { entry ->
        createNewInstance(entry.value.java)
    }): FactoryRegistry<T> {
        logger.info("Creating new registry for type: ${T::class}")
        val newRegistry = FactoryRegistry(factory = factory)
        registries[T::class] = newRegistry
        return newRegistry
    }

    inline fun <reified T: Any> getFactoryRegistry() = registries[T::class] as FactoryRegistry<T>? ?: throw IllegalStateException("Attempted to get a registry that does not exist: ${T::class}")
    inline fun <reified T: Any> getInstanceRegistry() = registries[T::class] as InstanceRegistry<*, T>? ?: throw IllegalStateException("Attempted to get a registry that does not exist: ${T::class}")
    inline fun <reified T> getRegistry() = registries[T::class] ?: throw IllegalStateException("Attempted to get a registry that does not exist: ${T::class}")
    fun getRegistries(): Map<KClass<*>, Registry<*, *>> = registries
}