package io.availe

import io.availe.gradle.KReplicaModelAttribute.KREPLICA_MODEL_TYPE_ATTRIBUTE
import io.availe.gradle.KReplicaModelAttribute.MODELS_JSON_TYPE
import io.availe.gradle.applyJvmConvention
import io.availe.gradle.applyKmpConvention
import org.gradle.api.Plugin
import org.gradle.api.Project

class KReplicaPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.logger.info("--- KREPLICA-PLUGIN: Applying 'io.availe.kreplica' to project ${target.path} ---")

        val projectVersion = "6.0.0"

        val kreplicaClasspath = target.configurations.create("kreplicaClasspath") {
            isCanBeResolved = true
            isCanBeConsumed = false
            description = "Resolves models.json files from all project dependencies."
            attributes {
                attribute(KREPLICA_MODEL_TYPE_ATTRIBUTE, MODELS_JSON_TYPE)
            }
        }

        target.configurations.all {
            val configName = this.name
            if (configName.endsWith("implementation", ignoreCase = true) || configName.endsWith(
                    "api",
                    ignoreCase = true
                )
            ) {
                kreplicaClasspath.extendsFrom(this)
            }
        }

        target.plugins.withId("com.google.devtools.ksp") {
            target.logger.info("--- KREPLICA-PLUGIN: KSP plugin found in ${target.path}. Configuring conventions. ---")
            when {
                target.plugins.hasPlugin("org.jetbrains.kotlin.multiplatform") -> {
                    applyKmpConvention(target, projectVersion)
                }

                target.plugins.hasPlugin("org.jetbrains.kotlin.jvm") -> {
                    applyJvmConvention(target, projectVersion)
                }

                else -> {
                    target.logger.warn("KReplica: No supported Kotlin plugin found (multiplatform or jvm). No convention applied.")
                }
            }
        }
    }
}