package io.availe.gradle

import io.availe.KReplicaExtension
import io.availe.models.KReplicaPaths
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

fun applyKmpConvention(project: Project, extension: KReplicaExtension, projectVersion: String) {
    project.logger.info("--- KREPLICA-PLUGIN: Applying KMP Convention to ${project.path} ---")
    val kmpExt = project.extensions.findByType(KotlinMultiplatformExtension::class.java) ?: return

    kmpExt.sourceSets.getByName("commonMain").dependencies {
        project.logger.info("--- KREPLICA-PLUGIN: Adding KReplica dependencies for KMP commonMain in ${project.path} ---")
        implementation("io.availe:model-ksp-annotations:$projectVersion")
        implementation("io.availe:codegen-models:$projectVersion")
    }

    project.dependencies.apply {
        add("kspCommonMainMetadata", "io.availe:model-ksp-processor:$projectVersion")
    }

    extension.primaryModelJson.set(
        project.layout.buildDirectory.file(
            "${KReplicaPaths.KSP_GENERATED_DIR}/${KReplicaPaths.KSP_METADATA_DIR}/${KReplicaPaths.RESOURCES_DIR}/${KReplicaPaths.MODELS_JSON_FILE}"
        )
    )
    extension.primaryModelJson.disallowChanges()

    registerKReplicaCodegenTask(project, extension, "kspCommonMainKotlinMetadata", projectVersion)
}