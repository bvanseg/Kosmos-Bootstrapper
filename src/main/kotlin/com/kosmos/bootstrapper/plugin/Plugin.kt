package com.kosmos.bootstrapper.plugin

/**
 * Defines a plugin.
 *
 * @author Boston Vanseghi
 * @since 1.0.0
 */
annotation class Plugin(val version: String, val name: String, val domain: String, val dependencies: Array<String> = [])