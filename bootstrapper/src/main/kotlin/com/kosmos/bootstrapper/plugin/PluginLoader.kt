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
            logger.debug("Instantiating plugins...")
            val start = System.currentTimeMillis()

            for (pluginClass in getClassesWithAnnotation(Plugin::class.jvmName).loadClasses()) {

                val metadata = pluginClass.getAnnotation(Plugin::class.java)

                if (plugins.containsKey(metadata.domain) && ignoreExistingPlugins) {
                    continue
                }

                instantiatePlugin(pluginClass)
            }

            logger.debug("Finished instantiating ${plugins.size} plugins in ${System.currentTimeMillis() - start}ms")
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
        val domain = metadata.domain.toLowerCase()

        if (plugins.containsKey(domain)) {
            throw RuntimeException("The domain name '$domain' is already being used by another plugin")
        }

        val dependents = mutableListOf<String>()

        for (plg in plugins) {
            // Don't check the current plugin
            if (plg.key == metadata.domain) {
                continue
            }

            val dependencyMetadata = plg.value.metadata

            if (dependencyMetadata.dependencies.contains(metadata.domain)) {
                dependents.add(plg.key)
            }
        }

        plugins[domain] = PluginContainer(plugin, pluginClass, metadata, dependents)
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
            val metadata = pluginContainer.metadata

            metadata.dependencies.forEach { dependencyDomain ->
                if(dependencyDomain.isNotBlank() && plugins[dependencyDomain.toLowerCase()] == null) {
                    logger.warn("Missing plugin dependency '$dependencyDomain' for plugin with domain name '$pluginDomain'. Removing dependent plugin to avoid issues...")
                    toRemove.add(pluginDomain)
                }
            }
        }

        toRemove.forEach {
            plugins.remove(it)
        }

        // For every plugin, check for duplicates in dependency and dependents.
        plugins.forEach { (_, plugin) ->
            plugin.metadata.dependencies.forEach { dependencyDomain ->
                if (plugin.dependents.contains(dependencyDomain)) {
                    throw RuntimeException("Circular dependency detected: ${plugin.metadata.domain} <-> $dependencyDomain")
                }
            }

            plugin.dependents.forEach { dependentDomain ->
                if (plugin.metadata.dependencies.contains(dependentDomain)) {
                    throw RuntimeException("Circular dependency detected: ${plugin.metadata.domain} <-> $dependentDomain")
                }
            }
        }
    }

    private fun initializePlugins() = runBlocking {

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

                    EVENT_BUS.fireForListener(pluginEntry.value, event)
                    resolvedPlugins.add(pluginEntry.key)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            })
        }

        jobs.joinAll()
    }

    fun getPlugin(domain: String) = plugins[domain.toLowerCase()]
}