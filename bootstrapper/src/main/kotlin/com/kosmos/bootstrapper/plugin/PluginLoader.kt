package com.kosmos.bootstrapper.plugin

import bvanseg.kotlincommons.io.logging.getLogger
import bvanseg.kotlincommons.reflect.createInstanceFrom
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.kosmos.bootstrapper.exception.DuplicateDomainException
import com.kosmos.bootstrapper.exception.PluginInstantiationException
import io.github.classgraph.Resource
import io.github.classgraph.ScanResult
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.jvm.jvmName

/**
 * @author Boston Vanseghi
 */
internal object PluginLoader {

    private val logger = getLogger()

    val pluginContainersByDomain = ConcurrentHashMap<String, PluginContainer>()

    private val pluginClassesByDomain = ConcurrentHashMap<String, Class<*>>()
    private val pluginAnnotationsByDomain = ConcurrentHashMap<String, Plugin>()
    private val pluginInstancesByDomain = ConcurrentHashMap<String, Any>()

    private val jsonMapper = jacksonObjectMapper()

    fun loadPluginDataFromClasspathScan(scanResult: ScanResult) {
        logger.info("Loading plugin classes...")
        val start = System.currentTimeMillis()

        val pluginAnnotatedClasses = scanResult.getClassesWithAnnotation(Plugin::class.jvmName).loadClasses()

        for (pluginClass in pluginAnnotatedClasses) {
            val pluginAnnotation = pluginClass.getAnnotation(Plugin::class.java)
                ?: throw RuntimeException("Failed to load plugin of class '$pluginClass' - missing @Plugin annotation. This should not be possible!")
            val lowercasePluginDomain = pluginAnnotation.domain.lowercase(Locale.getDefault())

            if (pluginClassesByDomain.containsKey(lowercasePluginDomain)) {
                throw DuplicateDomainException("The domain name '$lowercasePluginDomain' is already being used by another plugin!")
            }

            pluginClassesByDomain[lowercasePluginDomain] = pluginClass
            pluginAnnotationsByDomain[lowercasePluginDomain] = pluginAnnotation
        }

        logger.info("Finished loading ${pluginClassesByDomain.size} plugin classes in ${System.currentTimeMillis() - start}ms")
    }

    fun instantiatePluginsFromClasses() {
        pluginClassesByDomain.forEach { (domain, pluginClass) ->
            val pluginInstance = instantiatePluginFromClass(pluginClass)
            val pluginAnnotation = pluginAnnotationsByDomain[domain] ?: return@forEach
            val pluginDomain = pluginAnnotation.domain.lowercase(Locale.getDefault())

            pluginInstancesByDomain[pluginDomain] = pluginInstance
        }
    }

    fun loadPluginContainers() {
        pluginClassesByDomain.forEach { (domain, pluginClass) ->
            val lowercaseDomain = domain.lowercase(Locale.getDefault())
            val pluginInstance = pluginInstancesByDomain[lowercaseDomain] ?: return@forEach
            val pluginAnnotation = pluginAnnotationsByDomain[lowercaseDomain] ?: return@forEach

            pluginContainersByDomain[lowercaseDomain] = PluginContainer(pluginInstance, pluginClass, pluginAnnotation)
        }
    }

    private fun instantiatePluginFromClass(pluginClass: Class<*>) = try {
        pluginClass.kotlin.objectInstance ?: createInstanceFrom(pluginClass)
    } catch(e: NoSuchMethodException) {
        throw PluginInstantiationException("Failed to construct plugin class instance for '$pluginClass'. Make sure you have a no-arg constructor!", e)
    } catch(e: UninitializedPropertyAccessException) {
        throw PluginInstantiationException("Failed to construct plugin class instance for '$pluginClass'. Make sure you have a no-arg constructor!", e)
    } ?: throw PluginInstantiationException("Failed to instantiate plugin for class '$pluginClass'")


    fun populatePluginDependents() {
        logger.info("Populating dependents for all plugins...")
        val start = System.currentTimeMillis()
        for((domain, pluginContainer) in pluginContainersByDomain) {
            for((dependentDomain, potentialDependent) in pluginContainersByDomain) {
                if(potentialDependent.annotationData.dependencies.contains(domain)) {
                    pluginContainer.dependents.add(dependentDomain)
                }
            }
        }
        logger.info("Finished populating dependents for all plugins in ${System.currentTimeMillis() - start}ms")
    }

    fun initializePluginMetadata(scanResult: ScanResult) {
        logger.info("Initializing plugin metadata...")
        val start = System.currentTimeMillis()

        scanResult.getResourcesWithExtension("json")
            .forEachByteArrayThrowingIOException { res: Resource, content: ByteArray? ->

                try {
                    if (res.pathRelativeToClasspathElement.lowercase(Locale.getDefault()) == "meta.json") {
                        val tree = jsonMapper.readTree(content)

                        val domain = tree.get("domain")?.asText("null")?.lowercase(Locale.getDefault()) ?: "null"

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

                            val pluginContainer = pluginContainersByDomain[domain]

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

        logger.info("Finished initializing all plugin metadata in ${System.currentTimeMillis() - start}ms")
    }
}