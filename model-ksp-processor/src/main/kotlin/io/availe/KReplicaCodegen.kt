package io.availe

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import io.availe.generators.generateDataClasses
import io.availe.models.Model
import io.availe.utils.validateModelReplications
import org.slf4j.LoggerFactory

internal object KReplicaCodegen {
    private val logger = LoggerFactory.getLogger("io.availe.KReplicaCodegen")

    fun execute(
        primaryModels: List<Model>,
        allModels: List<Model>,
        codeGenerator: CodeGenerator,
        dependencies: Dependencies
    ) {
        logger.info("--- KREPLICA-CODEGEN: Codegen starting. ---")

        if (primaryModels.isEmpty()) {
            logger.info("--- KREPLICA-CODEGEN: No primary models to generate. Exiting. ---")
            return
        }

        if (allModels.isEmpty()) {
            logger.info("--- KREPLICA-CODEGEN: No models found at all. Exiting. ---")
            return
        }

        logger.info("--- KREPLICA-CODEGEN: Loaded ${allModels.size} total model definitions. Will generate sources for ${primaryModels.size} primary models.")

        validateModelReplications(allModels)
        logger.info("--- KREPLICA-CODEGEN: Model definitions validated successfully.")

        generateDataClasses(primaryModels, allModels, codeGenerator, dependencies)
        logger.info("--- KREPLICA-CODEGEN: Codegen finished. ---")
    }
}