package com.kosmos.bootstrapper.plugin

import bvanseg.kotlincommons.any.getLogger
import bvanseg.kotlincommons.evenir.bus.EventBus
import bvanseg.kotlincommons.javaclass.createNewInstance
import com.kosmos.bootstrapper.resource.MasterResourceManager
import io.github.classgraph.ClassGraph
import java.net.URL
import java.net.URLClassLoader
import java.nio.file.Files
import java.nio.file.Path
import kotlin.reflect.jvm.jvmName


/**
 * @author Boston Vanseghi
 * @since 1.0.0
 */
internal class PluginLoader(private val location: String) {

    val logger = getLogger()

    val EVENT_BUS = EventBus()

    val plugins = mutableMapOf<String, Any>()

    private var hasLoaded = false

    internal fun loadAllPlugins() {
        injectPlugins()
        findAndLoadPlugins()
        checkPluginDependencies()
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
                    logger.warn("Failed to load plugin with name '$pluginDomain': Missing plugin dependency '$dependencyDomain'. Removing dependent plugin to avoid issues...")
                    toRemove.add(pluginDomain)
                }
            }
        }

        toRemove.forEach {
            plugins.remove(it)
        }
    }
}