package com.kosmos.bootstrapper.jar

import bvanseg.kotlincommons.io.logging.getLogger
import io.github.classgraph.ClassGraph
import io.github.classgraph.ScanResult
import java.net.URL
import java.net.URLClassLoader
import java.nio.file.Files
import java.nio.file.Path
import java.util.*

/**
 * @author Boston Vanseghi
 * @since 1.0.0
 */
internal object JARLoader {

    private val logger = getLogger()

    private fun findJARFilesAt(location: String): List<URL> {
        val plugins = Files.walk(Path.of(location)).filter(Files::isRegularFile)

        val urls = mutableListOf<URL>()

        plugins.filter(Files::isRegularFile).forEach {
            val file = it.toFile()
            if (file.extension.lowercase(Locale.getDefault()) == "jar") {
                logger.info("Found potential plugin JAR '$it'")
                urls.add(file.toURI().toURL())
            }
        }

        return urls
    }

    /**
     * Loads all plugins onto the classpath from their respective JARs.
     *
     * @param location The location as a path to load JAR files from.
     */
    fun loadJARsAtLocationOntoClasspath(location: String): ClassGraph {
        logger.info("Beginning JAR loading")
        val start = System.currentTimeMillis()

        val jarFileURLs = findJARFilesAt(location)

        val classGraph = ClassGraph().enableAnnotationInfo()
        classGraph.acceptJars(*jarFileURLs.map { it.file.substringAfterLast("/") }.toTypedArray())
        classGraph.addClassLoader(URLClassLoader(jarFileURLs.toTypedArray(), this::class.java.classLoader))

        logger.info("Finished JAR loading in ${System.currentTimeMillis() - start}ms")

        return classGraph
    }

    fun scanClasspathResources(classGraph: ClassGraph? = null): ScanResult {
        logger.info("Scanning all resources...")
        val start = System.currentTimeMillis()
        val scanResult = (classGraph ?: ClassGraph().enableAllInfo()).scan()
        logger.info("Resource scan finished in ${System.currentTimeMillis() - start}ms")

        return scanResult
    }
}