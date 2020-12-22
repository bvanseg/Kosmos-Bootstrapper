package com.kosmos.bootstrapper

import com.kosmos.bootstrapper.plugin.PluginLoader

fun main(vararg args: String) {
    val location = args[0]

    val pluginLoader = PluginLoader(location)

    pluginLoader.loadAllPlugins()
}