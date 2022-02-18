package com.kosmos.bootstrapper.resource

import bvanseg.kotlincommons.io.logging.getLogger
import io.github.classgraph.ResourceList
import io.github.classgraph.ScanResult
import java.util.function.Predicate

/**
 * Used to manage files within a specified domain-space.
 *
 * @author Boston Vanseghi
 * @since 1.0.0
 */
open class ResourceManager(val root: String, val domain: String) {

    val logger = getLogger()

    val domainResources: ScanResult?
        get() = PluginResourceLoader.resourcesByDomain[domain]

    fun getResourcesWithPath(path: String): ResourceList? {
        return domainResources?.getResourcesWithPath(path)
    }

    fun createResourceLocation(location: String): ResourceLocation = ResourceLocation(this, domain, location)

    fun resourceExists(location: String): Boolean = !getResourcesWithPath("$root$domain/$location").isNullOrEmpty()

    fun getResourceLocations(): Collection<ResourceLocation> {
        return domainResources?.allResources?.filter {
            it.path.startsWith("$root$domain/")
        }?.map {
            ResourceLocation(this, domain, it.path.substring(root.length + domain.length + 1))
        } ?: emptyList()
    }

    fun getResourceLocations(predicate: Predicate<String>): Collection<ResourceLocation> {
        return domainResources?.allResources?.filter {
            it.path.startsWith("$root$domain/") && predicate.test(it.path.substring(root.length + domain.length + 1))
        }?.map {
            ResourceLocation(this, domain, it.path.substring(root.length + domain.length + 1))
        } ?: emptyList()
    }
}