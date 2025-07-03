package io.availe.gradle

import io.availe.models.KReplicaPaths
import org.gradle.api.Project
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension

fun applyJvmConvention(project: Project, projectVersion: String) {
    project.logger.info("--- KREPLICA-PLUGIN: Applying JVM Convention to ${project.path} ---")

    val kotlinPoetOutputDir = project.layout.buildDirectory.dir(KReplicaPaths.KOTLIN_POET_GENERATED_DIR)
    project.extensions.getByType(KotlinProjectExtension::class.java).sourceSets.getByName("main").kotlin.srcDir(
        kotlinPoetOutputDir
    )

    project.dependencies.apply {
        project.logger.info("--- KREPLICA-PLUGIN: Adding KReplica dependencies for JVM project ${project.path} ---")
        add("implementation", "io.availe:model-ksp-annotations:$projectVersion")
        add("implementation", "io.availe:codegen-models:$projectVersion")
        add("ksp", "io.availe:model-ksp-processor:$projectVersion")
    }

    val primaryModelJsonProvider: Provider<RegularFile> = project.layout.buildDirectory.file(
        "${KReplicaPaths.KSP_GENERATED_DIR}/${KReplicaPaths.KSP_JVM_DIR}/${KReplicaPaths.RESOURCES_DIR}/${KReplicaPaths.MODELS_JSON_FILE}"
    )

    registerKReplicaCodegenTask(project, "kspKotlin", projectVersion, primaryModelJsonProvider)
}