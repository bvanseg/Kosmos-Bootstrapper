package com.kosmos.bootstrapper.plugin

import bvanseg.kotlincommons.any.getLogger
import bvanseg.kotlincommons.evenir.bus.EventBus
import bvanseg.kotlincommons.javaclass.createNewInstance
import com.kosmos.bootstrapper.event.PluginInitializationEvent
import com.kosmos.bootstrapper.resource.MasterResourceManager
import io.github.classgraph.ClassGraph
import kotlinx.coroutines.*
import java.net.URL
import java.net.URLClassLoader
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.jvm.jvmName


/**
 * @author Boston Vanseghi
 * @since 1.0.0
 */
object PluginLoader {

    private val logger = getLogger()

    val EVENT_BUS = EventBus()

    private val plugins = ConcurrentHashMap<String, PluginContainer>()

    internal fun processPluginsAt(location: String) {
        loadJARsFrom(location)
        findPluginsOnClasspath(false)
        checkPluginDependencies()
        initializePlugins()
    }

    fun findPluginsOnClasspath(ignoreExistingPlugins: Boolean = true) {

        MasterResourceManager.resources.apply {
            logger.info("Instantiating plugins...")
            val start = System.currentTimeMillis()

            for (pluginClass in getClassesWithAnnotation(Plugin::class.jvmName).loadClasses()) {

                val metadata = pluginClass.getAnnotation(Plugin::class.java)

                if (plugins.containsKey(metadata.domain.toLowerCase()) && ignoreExistingPlugins) {
                    continue
                }

                instantiatePlugin(pluginClass)
            }

            logger.info("Finished instantiating ${plugins.size} plugins in ${System.currentTimeMillis() - start}ms")
        }
    }

    private fun instantiatePlugin(pluginClass: Class<*>) = try {
        logger.trace("Attempting to instantiate plugin class ${pluginClass.name}")

        val plugin = try {
            pluginClass.kotlin.objectInstance ?: createNewInstance(pluginClass)
        } catch(e: NoSuchMethodException) {
            throw RuntimeException("Failed to construct plugin class instance for $pluginClass. Make sure you have a no-arg constructor!", e)
        } catch(e: UninitializedPropertyAccessException) {
            throw RuntimeException("Failed to construct plugin class instance for $pluginClass. Make sure you have a no-arg constructor!", e)
        } ?: throw RuntimeException("Failed to load plugin for class $pluginClass")

        val metadata = pluginClass.getAnnotation(Plugin::class.java)
        val lowerDomain = metadata.domain.toLowerCase()

        if (plugins.containsKey(lowerDomain)) {
            throw RuntimeException("The domain name '$lowerDomain' is already being used by another plugin")
        }

        val dependents = mutableListOf<String>()

        for (plg in plugins) {
            // Don't check the current plugin
            if (plg.key.toLowerCase() == lowerDomain) {
                continue
            }

            val dependencyMetadata = plg.value.metadata

            if (dependencyMetadata.dependencies.map { it.toLowerCase() }.contains(lowerDomain)) {
                dependents.add(plg.key)
            }
        }

        plugins[lowerDomain] = PluginContainer(plugin, pluginClass, metadata, dependents)
    } catch (e: Exception) {
        logger.error("Error trying to instantiate plugin class ${pluginClass.name}", e)
    }

    /**
     * Loads all plugins onto the classpath from their respective JARs.
     */
    fun loadJARsFrom(location: String) {
        logger.info("Beginning JAR loading")
        val start = System.currentTimeMillis()
        val plugins = Files.walk(Path.of(location)).filter(Files::isRegularFile)

        val classGraph = ClassGraph().enableAllInfo()

        val urls = mutableListOf<URL>()

        plugins.filter(Files::isRegularFile).forEach {
            val file = it.toFile()
            if (file.extension.toLowerCase() == "jar") {
                logger.info("Found potential plugin JAR $it")
                urls.add(file.toURI().toURL())
            }
        }

        classGraph.addClassLoader(URLClassLoader(urls.toTypedArray(), this::class.java.classLoader))

        // Scan the resources after performing the injection.
        MasterResourceManager.scanResources(true, classGraph)
        logger.info("Finished JAR loading in ${System.currentTimeMillis() - start}ms")
    }

    private fun checkPluginDependencies() {
        val toRemove = mutableListOf<String>()
        plugins.forEach { (pluginDomain, pluginContainer) ->
            val lowerPluginDomain = pluginDomain.toLowerCase()
            val metadata = pluginContainer.metadata

            metadata.dependencies.map { it.toLowerCase() }.forEach { dependencyDomain ->
                val lowerDependencyDomain = dependencyDomain.toLowerCase()
                if(lowerDependencyDomain.isNotBlank() && plugins[lowerDependencyDomain] == null) {
                    logger.warn("Missing plugin dependency '$lowerDependencyDomain' for plugin with domain name '$lowerPluginDomain'. Removing dependent plugin to avoid issues...")
                    toRemove.add(lowerPluginDomain)
                }
            }
        }

        toRemove.forEach {
            plugins.remove(it)
        }

        for (plugin in plugins) {

            val container = plugin.value

            if (container.metadata.dependencies.isNotEmpty()) {
                val result = validateDependencyTree(container.metadata.domain.toLowerCase(), container)

                if (result) {
                    throw RuntimeException("Circular dependency detected for plugin with domain name ${container.metadata.domain}!")
                }
            }
        }
    }

    /**
     * Takes in a [PluginContainer] and recursively travels up its dependencies, verifying that this plugin isn't a dependency
     * for any of its own dependencies (circular dependency).
     */
    private fun validateDependencyTree(rootDomain: String, pluginContainer: PluginContainer): Boolean {

        for (dependency in pluginContainer.metadata.dependencies) {
            val container = plugins[dependency.toLowerCase()] ?: continue

            if (rootDomain == container.metadata.domain.toLowerCase()) {
                return true
            }

            val result = validateDependencyTree(rootDomain, container)

            if (result) {
                return true
            }
        }

        return false
    }

    private fun initializePlugins() = runBlocking {
        logger.info("Initializing all plugins...")
        val start = System.currentTimeMillis()

        val event = PluginInitializationEvent()

        val jobs = hashSetOf<Job>()
        val resolvedPlugins = hashSetOf<String>()

        plugins.forEach { pluginEntry ->
            jobs.add(GlobalScope.launch {
                try {
                    val metadata = pluginEntry.value.metadata

                    if (metadata.dependencies.isNotEmpty()) {
                        while (true) {
                            if (metadata.dependencies.all { resolvedPlugins.contains(it) }) {
                                break
                            } else {
                                delay(50)
                            }
                        }
                    }

                    EVENT_BUS.fireForListener(pluginEntry.value.plugin, event)
                    resolvedPlugins.add(pluginEntry.key)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            })
        }

        jobs.joinAll()

        logger.info("Finished initializing all plugins in ${System.currentTimeMillis() - start}ms")
    }

    fun getPlugin(domain: String) = plugins[domain.toLowerCase()]
}