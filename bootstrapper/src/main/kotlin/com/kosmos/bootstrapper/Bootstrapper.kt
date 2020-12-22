package com.kosmos.bootstrapper

import bvanseg.kotlincommons.any.getLogger
import com.kosmos.bootstrapper.plugin.PluginLoader
import kotlin.system.exitProcess

internal class Bootstrapper {

    companion object {

        private val logger = getLogger()

        @JvmStatic
        fun main(vararg args: String) {

            if(args.isEmpty()) {
                logger.warn("No location provided to load plugins from. Aborting...")
                exitProcess(0)
            }

            val location = args[0]

            val pluginLoader = PluginLoader(location)

            pluginLoader.loadAllPlugins()
        }
    }
}