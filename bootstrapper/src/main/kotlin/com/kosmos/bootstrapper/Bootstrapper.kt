package com.kosmos.bootstrapper

import com.kosmos.bootstrapper.jar.JARLoader
import com.kosmos.bootstrapper.plugin.PluginInitializer
import com.kosmos.bootstrapper.plugin.PluginLoader
import com.kosmos.bootstrapper.plugin.PluginValidator
import com.kosmos.bootstrapper.resource.PluginResourceLoader

/**
 * @author Boston Vanseghi
 */
internal object Bootstrapper {

    @JvmStatic
    fun main(vararg args: String) {
        val location = args.firstOrNull() ?: "plugins"

        // JAR Loading
        val classGraph = JARLoader.loadJARsAtLocationOntoClasspath(location)
        val result = JARLoader.scanClasspathResources(classGraph)

        // Plugin Loading
        PluginLoader.loadPluginDataFromClasspathScan(result)
        PluginLoader.instantiatePluginsFromClasses()
        PluginLoader.loadPluginContainers()

        // Plugin Metadata Loading
        PluginLoader.populatePluginDependents()
        PluginLoader.initializePluginMetadata(result)

        // Finish using classpath scan
        result.close()

        // Plugin Validation
        PluginValidator.validatePluginDependencyGraph()

        // Plugin Resource Loading
        PluginResourceLoader.loadResources()

        // Plugin Initialization
        PluginInitializer.initializePlugins()
    }
}