package io.availe

import io.availe.generators.generateDataClasses
import io.availe.models.Model
import io.availe.utils.validateModelReplications
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.io.File
import kotlin.system.exitProcess

private val logger = LoggerFactory.getLogger("io.availe.ApplicationKt")

fun main(args: Array<String>) {
    logger.info("--- KREPLICA-CODEGEN: Codegen Runtime launched with arguments ---")
    args.forEachIndexed { index, arg ->
        logger.info("ARG[$index]: $arg")
    }
    logger.info("--- KREPLICA-CODEGEN: End of arguments ---")

    logger.info("--- KREPLICA-CODEGEN: Codegen Runtime STARTING ---")
    try {
        var outputDir: File? = null
        val remainingArgs = mutableListOf<String>()
        var i = 0
        while (i < args.size) {
            when (args[i]) {
                "--output-dir" -> {
                    if (i + 1 < args.size) {
                        outputDir = File(args[i + 1])
                        i += 2
                    } else {
                        logger.error("--- KREPLICA-CODEGEN: --output-dir flag requires a value.")
                        exitProcess(1)
                    }
                }

                else -> {
                    remainingArgs.add(args[i])
                    i++
                }
            }
        }

        if (outputDir == null) {
            logger.error("--- KREPLICA-CODEGEN: Missing required --output-dir argument.")
            exitProcess(1)
        }

        val (flags, jsonPaths) = remainingArgs.partition { it.startsWith("--") }

        if (jsonPaths.isEmpty()) {
            logger.info("--- KREPLICA-CODEGEN: Codegen Runtime received no input paths. Exiting. ---")
            return
        }

        logger.info("--- KREPLICA-CODEGEN: Loading model definitions from: ${jsonPaths.joinToString()}")

        val allModels = jsonPaths.flatMap { path ->
            val jsonFile = File(path)
            if (jsonFile.exists()) {
                Json { ignoreUnknownKeys = true }.decodeFromString<List<Model>>(jsonFile.readText())
            } else {
                emptyList<Model>()
            }
        }.distinctBy { it.packageName + "." + it.name }

        if (allModels.isEmpty()) {
            logger.info("--- KREPLICA-CODEGEN: No models found to process. Exiting. ---")
            return
        }

        val primaryJsonFile = File(jsonPaths.first())
        val primaryModels = if (primaryJsonFile.exists()) {
            Json { ignoreUnknownKeys = true }.decodeFromString<List<Model>>(primaryJsonFile.readText())
        } else {
            emptyList()
        }

        if (primaryModels.isEmpty()) {
            logger.info("--- KREPLICA-CODEGEN: No primary models found to generate. Exiting. ---")
            return
        }

        logger.info("--- KREPLICA-CODEGEN: Loaded ${allModels.size} total model definitions. Will generate sources for ${primaryModels.size} primary models.")

        validateModelReplications(allModels)
        logger.info("--- KREPLICA-CODEGEN: Model definitions validated successfully.")

        generateDataClasses(primaryModels, allModels, outputDir)
        logger.info("--- KREPLICA-CODEGEN: Codegen Runtime FINISHED ---")

    } catch (e: Exception) {
        logger.error("--- KREPLICA-CODEGEN: An unexpected error occurred ---", e)
        exitProcess(1)
    }
}