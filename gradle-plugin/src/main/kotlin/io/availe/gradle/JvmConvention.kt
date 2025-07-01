package io.availe.gradle

import io.availe.KReplicaExtension
import io.availe.models.KReplicaPaths
import org.gradle.api.Project

fun applyJvmConvention(project: Project, extension: KReplicaExtension, projectVersion: String) {
    project.logger.info("--- KREPLICA-PLUGIN: Applying JVM Convention to ${project.path} ---")

    project.dependencies.apply {
        project.logger.info("--- KREPLICA-PLUGIN: Adding KReplica dependencies for JVM project ${project.path} ---")
        add("implementation", "io.availe:model-ksp-annotations:$projectVersion")
        add("implementation", "io.availe:codegen-models:$projectVersion")
        add("ksp", "io.availe:model-ksp-processor:$projectVersion")
    }

    extension.primaryModelJson.set(
        project.layout.buildDirectory.file(
            "${KReplicaPaths.KSP_GENERATED_DIR}/${KReplicaPaths.KSP_JVM_DIR}/${KReplicaPaths.RESOURCES_DIR}/${KReplicaPaths.MODELS_JSON_FILE}"
        )
    )
    extension.primaryModelJson.disallowChanges()

    registerKReplicaCodegenTask(project, extension, "kspKotlin", projectVersion)
}