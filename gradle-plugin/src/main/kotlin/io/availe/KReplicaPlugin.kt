package io.availe

import com.google.devtools.ksp.gradle.KspExtension
import io.availe.gradle.applyJvmConvention
import io.availe.gradle.applyKmpConvention
import io.availe.models.KReplicaPaths
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.JavaExec
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.named

class KReplicaPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.logger.info("--- KREPLICA-PLUGIN: Applying 'io.availe.kreplica' to project ${target.path} ---")
        val extension = target.extensions.create("kreplica", KReplicaExtension::class.java)

        val projectVersion = target.version.toString().removeSurrounding("\"")

        target.plugins.withId("com.google.devtools.ksp") {
            target.logger.info("--- KREPLICA-PLUGIN: KSP plugin found in ${target.path}. Configuring conventions. ---")
            when {
                target.plugins.hasPlugin("org.jetbrains.kotlin.multiplatform") -> {
                    applyKmpConvention(target, extension, projectVersion)
                }

                target.plugins.hasPlugin("org.jetbrains.kotlin.jvm") -> {
                    applyJvmConvention(target, extension, projectVersion)
                }

                else -> {
                    target.logger.warn("KReplica: No supported Kotlin plugin found (multiplatform or jvm). No convention applied.")
                }
            }

            target.extensions.getByType<KspExtension>().arg(
                "kreplica.output.dir",
                target.layout.buildDirectory.dir(KReplicaPaths.KOTLIN_POET_GENERATED_DIR).get().asFile.absolutePath
            )
        }

        target.afterEvaluate {
            val runCodegenTask = tasks.named<JavaExec>("runKReplicaCodegen")

            extension.contextProjects.get().forEach { contextProject ->
                evaluationDependsOn(contextProject.path)

                val kspConfigurationName: String
                val modelJsonPath: String
                val contextKspTaskName: String

                if (contextProject.plugins.hasPlugin("org.jetbrains.kotlin.multiplatform")) {
                    modelJsonPath =
                        "${KReplicaPaths.KSP_GENERATED_DIR}/${KReplicaPaths.KSP_METADATA_DIR}/${KReplicaPaths.RESOURCES_DIR}/${KReplicaPaths.MODELS_JSON_FILE}"
                    kspConfigurationName = "ksp"
                    contextKspTaskName = "kspCommonMainKotlinMetadata"
                } else if (contextProject.plugins.hasPlugin("org.jetbrains.kotlin.jvm")) {
                    modelJsonPath =
                        "${KReplicaPaths.KSP_GENERATED_DIR}/${KReplicaPaths.KSP_JVM_DIR}/${KReplicaPaths.RESOURCES_DIR}/${KReplicaPaths.MODELS_JSON_FILE}"
                    kspConfigurationName = "ksp"
                    contextKspTaskName = "kspKotlin"
                } else {
                    logger.warn("KReplica: Could not determine project type for context '${contextProject.path}'. Cannot add its model source.")
                    return@forEach
                }

                val jsonFileProvider = contextProject.layout.buildDirectory.file(modelJsonPath)
                extension.contextModelJsonsInternal.from(jsonFileProvider)
                dependencies.add(kspConfigurationName, contextProject)
                runCodegenTask.configure {
                    dependsOn(contextProject.tasks.named(contextKspTaskName))
                }
            }
        }
    }
}