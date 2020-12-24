package com.kosmos.bootstrapper

import com.kosmos.bootstrapper.plugin.PluginManager

object Bootstrapper {

    @JvmStatic
    fun main(vararg args: String) {
        val location = args.firstOrNull() ?: ""

        PluginManager.processPluginsAt(location)
    }
}