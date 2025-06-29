package io.availe.gradle

import io.availe.KReplicaExtension
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

fun applyJvmConvention(project: Project, extension: KReplicaExtension, projectVersion: String) {
    project.logger.info("--- KREPLICA-PLUGIN: Applying JVM Convention to ${project.path} ---")

    project.dependencies.apply {
        project.logger.info("--- KREPLICA-PLUGIN: Adding KReplica dependencies for JVM project ${project.path} ---")
        add("implementation", "io.availe:model-ksp-annotations:$projectVersion")
        add("implementation", "io.availe:codegen-models:$projectVersion")
        add("ksp", "io.availe:model-ksp-processor:$projectVersion")
    }

    extension.primaryModelJson.set(
        project.layout.buildDirectory.file("generated/ksp/main/resources/models.json")
    )
    extension.primaryModelJson.disallowChanges()

    registerKReplicaCodegenTask(project, extension, "kspKotlin", projectVersion)

    project.extensions.getByType(KotlinJvmProjectExtension::class.java).sourceSets.getByName("main").kotlin.srcDir(
        project.layout.buildDirectory.dir("generated-src/kotlin-poet")
    )
}