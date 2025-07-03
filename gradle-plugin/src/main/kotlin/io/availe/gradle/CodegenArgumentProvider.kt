package io.availe.gradle

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.*
import org.gradle.process.CommandLineArgumentProvider

internal abstract class CodegenArgumentProvider : CommandLineArgumentProvider {
    @get:Internal
    abstract val outputDir: DirectoryProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.ABSOLUTE)
    abstract val primaryModelJson: RegularFileProperty

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.ABSOLUTE)
    abstract val allModelFiles: ConfigurableFileCollection

    override fun asArguments(): Iterable<String> {
        val jsonFiles = allModelFiles.files.filter { it.name.endsWith(".json") }
        val primaryPath = primaryModelJson.get().asFile.absolutePath

        val allPaths = jsonFiles.map { it.absolutePath }.toMutableSet()
        allPaths.remove(primaryPath)
        val orderedPaths = listOf(primaryPath) + allPaths.sorted()

        return listOf("--output-dir", outputDir.get().asFile.absolutePath) + orderedPaths
    }
}