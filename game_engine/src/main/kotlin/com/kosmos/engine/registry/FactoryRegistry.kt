package com.kosmos.engine.registry

import bvanseg.kotlincommons.any.getLogger
import kotlin.reflect.KClass

/**
 * Represents the base of a registry.
 *
 * @author Boston Vanseghi
 * @since 1.0.0
 */
class FactoryRegistry<T : Any>(val factory: (FactoryRegistryEntry<T>) -> T?): Registry<KClass<out T>, T>() {

    private val logger = getLogger()

    private val idMap = hashMapOf<Int, KClass<out T>>()

    fun register(value: KClass<out T>) {
        val entry = FactoryRegistryEntry(this, value)

        if(entries.contains(entry)) {
            throw IllegalStateException("Attempted to register an entry that already exists: $value")
        }


        entries[value] = entry
        idMap[value.simpleName.hashCode()] = value

        logger.info("Successfully registered entry $value")
    }

    fun unregister(value: KClass<T>) {
        val entry = entries.remove(value)
        logger.info("Successfully unregistered entry $value")
    }

    fun getEntry(klass: KClass<out T>) = entries[klass] as FactoryRegistryEntry<T>?
    fun getEntry(obj: T) = getEntry(obj::class)
    fun getEntryByID(id: Int) = idMap[id]?.let { getEntry(it) }

    fun getIDFor(klass: KClass<out T>): Int = klass.simpleName.hashCode()
    fun getIDFor(obj: T) = getIDFor(obj::class)
}