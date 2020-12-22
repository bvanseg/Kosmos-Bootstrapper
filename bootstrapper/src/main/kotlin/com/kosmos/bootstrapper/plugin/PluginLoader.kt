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
import kotlin.reflect.jvm.jvmName


/**
 * @author Boston Vanseghi
 * @since 1.0.0
 */
object PluginLoader {

    internal lateinit var location: String

    private val logger = getLogger()

    val EVENT_BUS = EventBus()

    private val plugins = mutableMapOf<String, Any>()

    private var hasLoaded = false

    internal fun loadAllPlugins() {
        injectPlugins()
        findAndLoadPlugins()
        checkPluginDependencies()
        initializePlugins()
    }

    private fun findAndLoadPlugins() {
        if (hasLoaded) {
            throw IllegalStateException("Attempted to load plugins when plugins have already been loaded!")
        }

        MasterResourceManager.resources.apply {
            logger.debug("Loading plugin instances...")
            val start = System.currentTimeMillis()
            getClassesWithAnnotation(Plugin::class.jvmName).loadClasses().forEach { pluginClass ->
                loadPlugin(pluginClass)
            }
            logger.debug("Finished loading ${plugins.size} plugins in ${System.currentTimeMillis() - start}ms")
        }
        hasLoaded = true
    }

    private fun loadPlugin(pluginClass: Class<*>) = try {
        logger.trace("Attempting to load plugin class ${pluginClass.name}")

        val plugin = try {
            pluginClass.kotlin.objectInstance ?: createNewInstance(pluginClass)
        } catch(e: NoSuchMethodException) {
            throw RuntimeException("Failed to construct plugin class instance for $pluginClass. Make sure you have a no-arg constructor!", e)
        } catch(e: UninitializedPropertyAccessException) {
            throw RuntimeException("Failed to construct plugin class instance for $pluginClass. Make sure you have a no-arg constructor!", e)
        } ?: throw RuntimeException("Failed to load plugin for class $pluginClass")

        val metadata = pluginClass.getAnnotation(Plugin::class.java)
        val domain = metadata.domain

        if (plugins.containsKey(domain)) {
            throw RuntimeException("The domain name '$domain' is already being used by another plugin")
        }

        plugins[domain] = plugin
    } catch (e: Exception) {
        logger.error("Error trying to load plugin class ${pluginClass.name}", e)
    }

    /**
     * Loads all plugins onto the classpath from their respective JARs.
     */
    private fun injectPlugins() {
        logger.info("Beginning plugin injection")
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
        logger.info("Finished plugin injection in ${System.currentTimeMillis() - start}ms")
    }

    private fun checkPluginDependencies() {
        val toRemove = mutableListOf<String>()
        plugins.forEach { (pluginDomain, pluginInstance) ->
            val metadata = pluginInstance::class.java.getAnnotation(Plugin::class.java)

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

        val pluginData = plugins.map { pluginEntry ->
            val metadata = pluginEntry.value::class.java.getAnnotation(Plugin::class.java)

            val dependents = mutableListOf<String>()

            for (plugin in plugins) {
                // Don't check the current plugin
                if (plugin.key == metadata.domain) {
                    continue
                }

                val dependencyMetadata = plugin.value::class.java.getAnnotation(Plugin::class.java)

                if (dependencyMetadata.dependencies.contains(metadata.domain)) {
                    dependents.add(plugin.key)
                }
            }

            PluginData(metadata, dependents)
        }

        // For every plugin, check for duplicates in dependency and dependents.
        pluginData.forEach { plugin ->
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
                val metadata = pluginEntry.value::class.java.getAnnotation(Plugin::class.java)

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
            })
        }

        jobs.joinAll()
    }
}