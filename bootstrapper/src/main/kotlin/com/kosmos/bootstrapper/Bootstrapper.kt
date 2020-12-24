package com.kosmos.bootstrapper

import com.kosmos.bootstrapper.plugin.PluginLoader

object Bootstrapper {

    @JvmStatic
    fun main(vararg args: String) {
        val location = args.firstOrNull() ?: ""

        PluginLoader.processPluginsAt(location)
    }
}