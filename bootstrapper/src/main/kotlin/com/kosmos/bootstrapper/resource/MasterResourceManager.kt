package com.kosmos.bootstrapper.resource

import java.util.*
import java.util.function.Predicate

/**
 * @author Boston Vanseghi
 * @since 1.0.0
 */
object MasterResourceManager : ResourceManager("", "master") {

    private val resourceManagers = hashMapOf<String, ResourceManager>()

    init {
        resourceManagers[domain] = this
    }

    fun createResourceManager(root: String, domain: String): ResourceManager {
        val lowerDomain = domain.lowercase(Locale.getDefault())

        if (resourceManagers[lowerDomain] != null) {
            throw IllegalStateException("Attempted to register a domain that already exists: $domain")
        }

        val manager = ResourceManager(root, domain)

        resourceManagers[domain] = manager

        return manager
    }

    fun getAllResourceLocations(predicate: Predicate<String> = Predicate { true }): Collection<ResourceLocation> {
        return resourceManagers.values.flatMap { it.getResourceLocations(predicate) }
    }

    fun getResourceManager(domain: String) = resourceManagers[domain.lowercase(Locale.getDefault())]
    fun getAllResourceManagers(): Collection<ResourceManager> = resourceManagers.values

    fun createResourceLocation(domain: String, location: String) = ResourceLocation(this, domain, location)
}