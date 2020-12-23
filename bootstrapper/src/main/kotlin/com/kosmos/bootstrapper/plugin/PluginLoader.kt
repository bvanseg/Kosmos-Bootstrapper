package com.kosmos.bootstrapper.plugin

import bvanseg.kotlincommons.any.getLogger
import bvanseg.kotlincommons.evenir.bus.EventBus
import bvanseg.kotlincommons.javaclass.createNewInstance
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.kosmos.bootstrapper.event.PluginInitializationEvent
import com.kosmos.bootstrapper.exception.CircularDependencyException
import com.kosmos.bootstrapper.exception.DuplicateDomainException
import com.kosmos.bootstrapper.exception.PluginInstantiationException
import com.kosmos.bootstrapper.resource.MasterResourceManager
import io.github.classgraph.ClassGraph
import io.github.classgraph.Resource
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

    private val jsonMapper = jacksonObjectMapper()

    internal fun processPluginsAt(location: String) {
        loadJARsFrom(location)
        findPluginsOnClasspath(false)
        checkPluginDependencies()
        fetchPluginMetadata()
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

    /**
     * Instantiates a plugin of the given class.
     *
     * @param pluginClass The class of the Plugin to instantiate. It is presumed this class has an annotation of type [Plugin].
     */
    private fun instantiatePlugin(pluginClass: Class<*>) = try {
        logger.trace("Attempting to instantiate plugin class '${pluginClass.name}'")

        val plugin = try {
            pluginClass.kotlin.objectInstance ?: createNewInstance(pluginClass)
        } catch(e: NoSuchMethodException) {
            throw PluginInstantiationException("Failed to construct plugin class instance for '$pluginClass'. Make sure you have a no-arg constructor!", e)
        } catch(e: UninitializedPropertyAccessException) {
            throw PluginInstantiationException("Failed to construct plugin class instance for '$pluginClass'. Make sure you have a no-arg constructor!", e)
        } ?: throw PluginInstantiationException("Failed to instantiate plugin for class '$pluginClass'")

        val metadata = pluginClass.getAnnotation(Plugin::class.java) ?: throw RuntimeException("Failed to load plugin of class '$pluginClass' - missing @Plugin annotation. This should not be possible!")
        val lowerDomain = metadata.domain.toLowerCase()

        if (plugins.containsKey(lowerDomain)) {
            throw DuplicateDomainException("The domain name '$lowerDomain' is already being used by another plugin!")
        }

        val dependents = mutableListOf<String>()

        for (plg in plugins) {
            // Don't check the current plugin
            if (plg.key.toLowerCase() == lowerDomain) {
                continue
            }

            val dependencyMetadata = plg.value.annotationData

            if (dependencyMetadata.dependencies.map { it.toLowerCase() }.contains(lowerDomain)) {
                dependents.add(plg.key)
            }
        }

        plugins[lowerDomain] = PluginContainer(plugin, pluginClass, metadata, dependents)
    } catch (e: Exception) {
        logger.error("Error trying to instantiate plugin class '${pluginClass.name}'", e)
    }

    /**
     * Loads all plugins onto the classpath from their respective JARs.
     *
     * @param location The location as a path to load JAR files from.
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
                logger.info("Found potential plugin JAR '$it'")
                urls.add(file.toURI().toURL())
            }
        }

        classGraph.addClassLoader(URLClassLoader(urls.toTypedArray(), this::class.java.classLoader))

        // Scan the resources after performing the injection.
        MasterResourceManager.scanResources(true, classGraph)
        logger.info("Finished JAR loading in ${System.currentTimeMillis() - start}ms")
    }

    /**
     * Checks all plugins and their dependencies to ensure that there are no missing or circular dependencies.
     *
     * @throws CircularDependencyException if a plugin has a circular dependency issue.
     */
    private fun checkPluginDependencies() {
        val toRemove = mutableListOf<String>()
        plugins.forEach { (pluginDomain, pluginContainer) ->
            val lowerPluginDomain = pluginDomain.toLowerCase()
            val metadata = pluginContainer.annotationData

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

            if (container.annotationData.dependencies.isNotEmpty()) {
                val result = validateDependencyTree(container.annotationData.domain.toLowerCase(), container)

                if (result) {
                    throw CircularDependencyException("Circular dependency detected for plugin with domain name '${container.annotationData.domain}'!")
                }
            }
        }
    }

    /**
     * Takes in a [PluginContainer] and recursively travels up its dependencies, verifying that this plugin isn't a dependency
     * for any of its own dependencies (circular dependency).
     */
    private fun validateDependencyTree(rootDomain: String, pluginContainer: PluginContainer): Boolean {

        for (dependency in pluginContainer.annotationData.dependencies) {
            val container = plugins[dependency.toLowerCase()] ?: continue

            if (rootDomain == container.annotationData.domain.toLowerCase()) {
                return true
            }

            val result = validateDependencyTree(rootDomain, container)

            if (result) {
                return true
            }
        }

        return false
    }

    private fun fetchPluginMetadata() {
        logger.info("Fetching plugin metadata...")
        val start = System.currentTimeMillis()

        MasterResourceManager.resources.getResourcesWithExtension("json")
        .forEachByteArray { res: Resource, content: ByteArray? ->

            try {
                if (res.pathRelativeToClasspathElement.toLowerCase() == "meta.json") {
                    val tree = jsonMapper.readTree(content)

                    val domain = tree.get("domain")?.asText("null")?.toLowerCase() ?: "null"

                    if (domain != "null") {
                        logger.info("Found plugin metadata for domain name '$domain'")
                        val name = tree.get("name")?.asText("null") ?: "null"
                        val version = tree.get("version")?.asText("null") ?: "null"
                        val authors = tree.get("authors")?.map { it.asText() } ?: listOf()
                        val description = tree.get("description")?.asText("null") ?: "null"
                        val websiteURL = tree.get("websiteURL")?.asText("null") ?: "null"
                        val logoURL = tree.get("logoURL")?.asText("null") ?: "null"
                        val credits = tree.get("credits")?.asText("null") ?: "null"
                        val dependencies = tree.get("dependencies")?.map { it.asText() } ?: listOf()

                        val pluginContainer = plugins[domain]

                        if (pluginContainer != null) {
                            pluginContainer.metadata = PluginMetadata(
                                name = name,
                                version = version,
                                authors = authors,
                                description = description,
                                websiteURL = websiteURL,
                                logoURL = logoURL,
                                credits = credits,
                                dependencies = dependencies
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        logger.info("Finished fetching all plugin metadata in ${System.currentTimeMillis() - start}ms")
    }

    /**
     * Initializes all plugins that were instantiated and stored in the [PluginLoader].
     */
    private fun initializePlugins() = runBlocking {
        logger.info("Initializing all plugins...")
        val start = System.currentTimeMillis()

        val event = PluginInitializationEvent()

        val jobs = hashSetOf<Job>()
        val resolvedPlugins = hashSetOf<String>()

        plugins.forEach { pluginEntry ->
            jobs.add(GlobalScope.launch {
                try {
                    val metadata = pluginEntry.value.annotationData

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