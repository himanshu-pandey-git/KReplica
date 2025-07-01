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

    override fun process(resolver: Resolver): List<KSAnnotated> {
        if (stubsGenerated) {
            env.logger.info("--- KREPLICA-KSP: Round 2: Processing models ---")
            val modelSymbols = resolver
                .getSymbolsWithAnnotation(MODEL_ANNOTATION_NAME)
                .filterIsInstance<KSClassDeclaration>()
                .filter(::isNonHiddenModelAnnotation)
                .toList()

            if (modelSymbols.isNotEmpty()) {
                val frameworkDecls = getFrameworkDeclarations(resolver)
                val models = modelSymbols.map { decl ->
                    buildModel(decl, resolver, frameworkDecls, env)
                }
                writeModelsToFile(models, modelSymbols)
            }
            env.logger.info("--- KREPLICA-KSP: ModelProcessor FINISHED ---")
            return emptyList()
        }

        env.logger.info("--- KREPLICA-KSP: Round 1: Generating Stubs ---")
        val modelSymbols = resolver
            .getSymbolsWithAnnotation(MODEL_ANNOTATION_NAME)
            .filterIsInstance<KSClassDeclaration>()
            .filter(::isNonHiddenModelAnnotation)
            .toList()

        if (modelSymbols.isEmpty()) {
            env.logger.info("--- KREPLICA-KSP: No models found to process. ---")
            stubsGenerated = true
            return emptyList()
        }

        generateStubs(modelSymbols, env)

        stubsGenerated = true

        return modelSymbols
    }

    private fun writeModelsToFile(models: List<Model>, sourceSymbols: List<KSClassDeclaration>) {
        if (models.isEmpty()) return
        val jsonText = Json { prettyPrint = true }.encodeToString(models)
        val sourceFiles = sourceSymbols.mapNotNull { it.containingFile }.toTypedArray()
        val dependencies = Dependencies(true, *sourceFiles)
        val fileName = KReplicaPaths.MODELS_JSON_FILE
        val extension = fileName.substringAfterLast('.', "")
        val baseName = fileName.removeSuffix(".$extension")
        val file = env.codeGenerator.createNewFile(dependencies, "", baseName, extension)
        env.logger.info("--- KREPLICA-KSP: Writing ${models.size} models to models.json ---")
        OutputStreamWriter(file, "UTF-8").use { it.write(jsonText) }
    }
}