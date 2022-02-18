package com.kosmos.bootstrapper.plugin

/**
 * Contains the metadata for plugins along with a list of their dependents.
 *
 * @author Boston Vanseghi
 * @since 1.0.0
 */
data class PluginContainer(
    val plugin: Any,
    val pluginClass: Class<*>,
    val annotationData: Plugin,
    internal val dependents: HashSet<String> = hashSetOf(),
    var metadata: PluginMetadata? = null
) {
    fun getDependentPlugins(): Set<String> = dependents
}