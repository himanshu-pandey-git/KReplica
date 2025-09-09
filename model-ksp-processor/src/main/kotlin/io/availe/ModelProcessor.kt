package io.availe

import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import io.availe.builders.buildModel
import io.availe.builders.generateStubs
import io.availe.extensions.KReplicaAnnotationContext
import io.availe.extensions.MODEL_ANNOTATION_NAME
import io.availe.extensions.getFrameworkDeclarations
import io.availe.extensions.isNonHiddenModelAnnotation
import io.availe.models.KReplicaPaths
import io.availe.models.Model
import kotlinx.serialization.json.Json
import java.io.File
import java.io.OutputStreamWriter
import kotlin.system.exitProcess

private val jsonParser = Json { ignoreUnknownKeys = true }
private val jsonPrettyPrinter = Json { prettyPrint = true }

internal class ModelProcessor(private val env: SymbolProcessorEnvironment) : SymbolProcessor {
    private val builtModels = mutableListOf<Model>()
    private val sourceSymbols = mutableSetOf<KSClassDeclaration>()
    private var round = ProcessingRound.FIRST
    private val upstreamModels = mutableListOf<Model>()
    private var initialized = false

    private enum class ProcessingRound {
        FIRST, SECOND
    }

    private fun initialize() {
        val metadataPaths = env.options["kreplica.metadataFiles"]?.split(File.pathSeparator)
            ?.filter { it.isNotBlank() } ?: emptyList()

        if (metadataPaths.isNotEmpty()) {
            env.logger.info("--- KREPLICA-KSP: Loading metadata from ${metadataPaths.size} upstream files. ---")
        }

        val loadedModels = metadataPaths
            .map { path -> File(path) }
            .filter { file -> file.name == KReplicaPaths.MODELS_JSON_FILE }
            .flatMap { jsonFile ->
                if (jsonFile.exists() && jsonFile.length() > 0) {
                    try {
                        jsonParser.decodeFromString<List<Model>>(jsonFile.readText())
                    } catch (e: Exception) {
                        env.logger.error("--- KREPLICA-KSP: Failed to parse metadata file: ${jsonFile.absolutePath} ---\n${e.stackTraceToString()}")
                        emptyList<Model>()
                    }
                } else {
                    emptyList<Model>()
                }
            }
        upstreamModels.addAll(loadedModels)
        initialized = true
    }

    override fun process(resolver: Resolver): List<KSAnnotated> {
        if (!initialized) {
            initialize()
        }

        return when (round) {
            ProcessingRound.FIRST -> processStubs(resolver)
            ProcessingRound.SECOND -> processModels(resolver)
        }
    }

    private fun processStubs(resolver: Resolver): List<KSAnnotated> {
        val modelSymbols = resolver
            .getSymbolsWithAnnotation(MODEL_ANNOTATION_NAME)
            .filterIsInstance<KSClassDeclaration>()
            .filter { it.isNonHiddenModelAnnotation() }
            .toList()

        if (modelSymbols.isNotEmpty()) {
            generateStubs(modelSymbols, env)
        }

        round = ProcessingRound.SECOND
        return modelSymbols
    }

    private fun processModels(resolver: Resolver): List<KSAnnotated> {
        val modelSymbols = resolver
            .getSymbolsWithAnnotation(MODEL_ANNOTATION_NAME)
            .filterIsInstance<KSClassDeclaration>()
            .filter { it.isNonHiddenModelAnnotation() }
            .toList()

        val modelAnnotationDeclaration =
            resolver.getClassDeclarationByName(resolver.getKSNameFromString(MODEL_ANNOTATION_NAME))
                ?: error("Could not resolve @Replicate.Model annotation declaration.")
        val annotationContext = KReplicaAnnotationContext(modelAnnotation = modelAnnotationDeclaration)

        val frameworkDecls = getFrameworkDeclarations(resolver)
        val builtModels = modelSymbols.map { decl ->
            buildModel(decl, resolver, frameworkDecls, annotationContext, env)
        }
        this.builtModels.addAll(builtModels)
        this.sourceSymbols.addAll(modelSymbols)
        return emptyList()
    }

    override fun finish() {
        if (builtModels.isEmpty()) {
            return
        }

        try {
            val allKnownModels = (this.upstreamModels + this.builtModels).distinctBy {
                "${it.packageName}:${it.isVersionOf}:${it.name}"
            }
            val dependencies = Dependencies(true, *sourceSymbols.mapNotNull { it.containingFile }.toTypedArray())
            KReplicaCodegen.execute(
                primaryModels = this.builtModels,
                allModels = allKnownModels,
                codeGenerator = env.codeGenerator,
                dependencies = dependencies
            )
        } catch (e: Exception) {
            env.logger.error("--- KREPLICA-KSP: Code generation failed with an exception ---\n${e.stackTraceToString()}")
            exitProcess(1)
        }

        writeModelsToFile(this.builtModels, this.sourceSymbols.toList())
    }

    private fun writeModelsToFile(models: List<Model>, sourceSymbols: List<KSClassDeclaration>) {
        val jsonText = jsonPrettyPrinter.encodeToString(models)
        val sourceFiles = sourceSymbols.mapNotNull { it.containingFile }.distinct().toTypedArray()
        val dependencies = Dependencies(true, *sourceFiles)
        val fileName = KReplicaPaths.MODELS_JSON_FILE
        val file = env.codeGenerator.createNewFile(dependencies, "", fileName, "")
        env.logger.info("--- KREPLICA-KSP: Writing ${models.size} models to models.json for downstream consumers. ---")
        OutputStreamWriter(file, "UTF-8").use { it.write(jsonText) }
    }
}