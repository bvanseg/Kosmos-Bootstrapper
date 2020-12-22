package com.kosmos.bootstrapper.plugin

/**
 * Contains the metadata for plugins along with a list of their dependents.
 *
 * @author Boston Vanseghi
 * @since 1.0.0
 */
data class PluginData(val metadata: Plugin, val dependents: MutableList<String>)