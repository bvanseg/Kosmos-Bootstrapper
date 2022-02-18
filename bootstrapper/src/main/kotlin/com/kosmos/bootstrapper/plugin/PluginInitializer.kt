package com.kosmos.bootstrapper.plugin

import bvanseg.kotlincommons.io.logging.getLogger
import bvanseg.kotlincommons.time.api.minutes
import bvanseg.kotlincommons.util.concurrent.KCountDownLatch
import com.kosmos.bootstrapper.event.PluginInitializationEvent
import kotlinx.coroutines.Job
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.concurrent.ConcurrentHashMap

/**
 * @author Boston Vanseghi
 */
internal object PluginInitializer {

    private val logger = getLogger()

    /**
     * Initializes all plugins that were instantiated and stored in the [PluginManager].
     */
    fun initializePlugins() = runBlocking {
        logger.info("Initializing all plugins...")

        val pluginContainersByDomain = PluginLoader.pluginContainersByDomain

        val start = System.currentTimeMillis()

        val event = PluginInitializationEvent()

        val jobs = hashSetOf<Job>()
        val processedPluginDomains = ConcurrentHashMap<String, KCountDownLatch>()

        // Create a KCountDownLatch for all dependencies. KCountDownLatches are specialized CountDownLatches for coroutines.
        pluginContainersByDomain.forEach { (domain, pluginContainer) ->
            processedPluginDomains.computeIfAbsent(domain) { KCountDownLatch(pluginContainer.annotationData.dependencies.size) }
        }

        // Create a job for every plugin now that each plugin is ensured to have a KCountDownLatch.
        pluginContainersByDomain.forEach { (domain, pluginContainer) ->
            val annotationData = pluginContainer.annotationData

            jobs.add(launch {
                try {
                    // If this plugin has dependencies, it must await for its latch to count down so that it may proceed.
                    if (annotationData.dependencies.isNotEmpty()) {
                        val latch = processedPluginDomains.computeIfAbsent(domain) { KCountDownLatch(annotationData.dependencies.size) }
                        // We set a timeout period of 1 minute here so that we do not stay locked forever.
                        // TODO: Calculate the timeout based on depth of the plugin in the dependency graph.
                        latch.await(1.minutes)
                    }

                    // Fires a plugin initialization event for when the plugin can proceed.
                    // NOTE: This is the actual piece of code that causes plugins to run. DO NOT REMOVE.
                    PluginManager.EVENT_BUS.fireForListener(pluginContainer.plugin, event)

                    // Count down for all latches of all dependents.
                    for (dependents in pluginContainer.dependents) {
                        processedPluginDomains[dependents]?.countDown()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            })
        }

        jobs.joinAll()
        processedPluginDomains.clear()

        logger.info("Finished initializing all plugins in ${System.currentTimeMillis() - start}ms")
    }
}