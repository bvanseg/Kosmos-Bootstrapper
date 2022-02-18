package com.kosmos.bootstrapper.resource

import com.kosmos.bootstrapper.plugin.PluginLoader
import io.github.classgraph.ClassGraph
import io.github.classgraph.ScanResult
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * @author Boston Vanseghi
 */
internal object PluginResourceLoader {

    val resourcesByDomain = ConcurrentHashMap<String, ScanResult>()

    fun loadResources() {
        val pluginContainersByDomain = PluginLoader.pluginContainersByDomain

        pluginContainersByDomain.forEach { (domain, _) ->
            val lowercaseDomain = domain.lowercase(Locale.getDefault())
            val scanResult = ClassGraph().enableAllInfo().acceptPaths(domain).scan()
            resourcesByDomain[lowercaseDomain] = scanResult
        }
    }

    fun close() {
        resourcesByDomain.entries.removeIf { (_, scanResult) ->
            scanResult.close()
            true
        }
    }
}