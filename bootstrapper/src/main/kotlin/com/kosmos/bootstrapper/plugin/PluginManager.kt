package com.kosmos.bootstrapper.plugin

import bvanseg.kotlincommons.util.event.EventBus
import java.util.*


/**
 * @author Boston Vanseghi
 * @since 1.0.0
 */
object PluginManager {
    val EVENT_BUS = EventBus()

    fun getPluginContainer(domain: String) = PluginLoader.pluginContainersByDomain[domain.lowercase(Locale.getDefault())]
}