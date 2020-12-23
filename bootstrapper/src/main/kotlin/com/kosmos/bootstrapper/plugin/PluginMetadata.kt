package com.kosmos.bootstrapper.plugin

/**
 * @author Boston Vanseghi
 * @since 1.0.0
 */
data class PluginMetadata(
    val name: String,
    val version: String,
    val authors: List<String>,
    val description: String
)