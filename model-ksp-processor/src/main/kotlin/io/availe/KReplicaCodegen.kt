package io.availe

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import io.availe.generators.generateDataClasses
import io.availe.models.Model
import io.availe.validation.validateModelReplications

internal object KReplicaCodegen {
    fun execute(
        primaryModels: List<Model>,
        allModels: List<Model>,
        codeGenerator: CodeGenerator,
        dependencies: Dependencies
    ) {
        if ((primaryModels.isEmpty()) || allModels.isEmpty()) return

        validateModelReplications(allModels)
        generateDataClasses(primaryModels, allModels, codeGenerator, dependencies)
    }
}