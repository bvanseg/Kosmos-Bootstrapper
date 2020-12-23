package com.kosmos.bootstrapper

import com.kosmos.bootstrapper.plugin.PluginLoader

internal class Bootstrapper {

    companion object {

        @JvmStatic
        fun main(vararg args: String) {
            val location = args.firstOrNull() ?: ""
            PluginLoader.processPluginsAt(location)
        }
    }
}