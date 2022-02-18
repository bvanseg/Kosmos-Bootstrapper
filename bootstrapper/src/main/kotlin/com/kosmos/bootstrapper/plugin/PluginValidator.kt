package com.kosmos.bootstrapper.plugin

import bvanseg.kotlincommons.io.logging.getLogger
import com.kosmos.bootstrapper.exception.CircularDependencyException
import java.util.*

/**
 * @author Boston Vanseghi
 */
internal object PluginValidator {

    private val logger = getLogger()

    /**
     * Checks all plugins and their dependencies to ensure that there are no missing or circular dependencies.
     *
     * @throws CircularDependencyException if a plugin has a circular dependency issue.
     */
    fun validatePluginDependencyGraph() {
        val pluginContainersByDomain = PluginLoader.pluginContainersByDomain

        val toRemove = mutableListOf<String>()
        pluginContainersByDomain.forEach { (pluginDomain, pluginContainer) ->
            val lowerPluginDomain = pluginDomain.lowercase(Locale.getDefault())
            val metadata = pluginContainer.annotationData

            metadata.dependencies.map { it.lowercase(Locale.getDefault()) }.forEach { dependencyDomain ->
                val lowerDependencyDomain = dependencyDomain.lowercase(Locale.getDefault())
                if (lowerDependencyDomain.isNotBlank() && pluginContainersByDomain[lowerDependencyDomain] == null) {
                    logger.warn("Missing plugin dependency '$lowerDependencyDomain' for plugin with domain name '$lowerPluginDomain'. Removing dependent plugin to avoid issues...")
                    toRemove.add(lowerPluginDomain)
                }
            }
        }

        toRemove.forEach(pluginContainersByDomain::remove)

        for ((_, pluginContainer) in pluginContainersByDomain) {
            val annotationData = pluginContainer.annotationData

            if (annotationData.dependencies.isNotEmpty()) {
                val result =
                    validateDependencyTree(
                        annotationData.domain.lowercase(Locale.getDefault()),
                        pluginContainer
                    )

                if (result) {
                    throw CircularDependencyException("Circular dependency detected for plugin with domain name '${annotationData.domain}'!")
                }
            }
        }
    }

    /**
     * Takes in a [PluginContainer] and recursively travels up its dependencies, verifying that this plugin isn't a dependency
     * for any of its own dependencies (circular dependency).
     */
    private fun validateDependencyTree(rootDomain: String, rootPluginContainer: PluginContainer): Boolean {
        val pluginContainersByDomain = PluginLoader.pluginContainersByDomain

        for (dependency in rootPluginContainer.annotationData.dependencies) {
            val pluginContainer = pluginContainersByDomain[dependency.lowercase(Locale.getDefault())] ?: continue

            if (rootDomain == pluginContainer.annotationData.domain.lowercase(Locale.getDefault())) {
                return true
            }

            val result = validateDependencyTree(rootDomain, pluginContainer)

            if (result) {
                return true
            }
        }

        return false
    }
}