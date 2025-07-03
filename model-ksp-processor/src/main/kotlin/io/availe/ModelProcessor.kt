package io.availe

import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import io.availe.builders.buildModel
import io.availe.builders.generateStubs
import io.availe.helpers.MODEL_ANNOTATION_NAME
import io.availe.helpers.getFrameworkDeclarations
import io.availe.helpers.isNonHiddenModelAnnotation
import io.availe.models.KReplicaPaths
import io.availe.models.Model
import kotlinx.serialization.json.Json
import java.io.OutputStreamWriter

class ModelProcessor(private val env: SymbolProcessorEnvironment) : SymbolProcessor {
    private var stubsGenerated = false
    private val models = mutableListOf<Model>()
    private val sourceSymbols = mutableListOf<KSClassDeclaration>()

    override fun process(resolver: Resolver): List<KSAnnotated> {
        if (stubsGenerated) {
            env.logger.info("--- KREPLICA-KSP: Round 2: Processing models ---")
            val modelSymbols = resolver
                .getSymbolsWithAnnotation(MODEL_ANNOTATION_NAME)
                .filterIsInstance<KSClassDeclaration>()
                .filter(::isNonHiddenModelAnnotation)
                .toList()

            val frameworkDecls = getFrameworkDeclarations(resolver)
            val builtModels = modelSymbols.map { decl ->
                buildModel(decl, resolver, frameworkDecls, env)
            }
            this.models.addAll(builtModels)
            this.sourceSymbols.addAll(modelSymbols)
            return emptyList()
        }

        env.logger.info("--- KREPLICA-KSP: Round 1: Generating Stubs ---")
        val modelSymbols = resolver
            .getSymbolsWithAnnotation(MODEL_ANNOTATION_NAME)
            .filterIsInstance<KSClassDeclaration>()
            .filter(::isNonHiddenModelAnnotation)
            .toList()

        if (modelSymbols.isNotEmpty()) {
            generateStubs(modelSymbols, env)
        } else {
            env.logger.info("--- KREPLICA-KSP: No models found to process. ---")
        }

        stubsGenerated = true
        return modelSymbols
    }

    override fun finish() {
        env.logger.info("--- KREPLICA-KSP: Finish hook called. Writing models.json. ---")
        writeModelsToFile(this.models, this.sourceSymbols)
    }

    private fun writeModelsToFile(models: List<Model>, sourceSymbols: List<KSClassDeclaration>) {
        val jsonText = Json { prettyPrint = true }.encodeToString(models)
        val sourceFiles = sourceSymbols.mapNotNull { it.containingFile }.distinct().toTypedArray()
        val dependencies = Dependencies(true, *sourceFiles)
        val fileName = KReplicaPaths.MODELS_JSON_FILE
        val extension = fileName.substringAfterLast('.', "")
        val baseName = fileName.removeSuffix(".$extension")
        val file = env.codeGenerator.createNewFile(dependencies, "", baseName, extension)
        env.logger.info("--- KREPLICA-KSP: Writing ${models.size} models to models.json ---")
        OutputStreamWriter(file, "UTF-8").use { it.write(jsonText) }
    }
}