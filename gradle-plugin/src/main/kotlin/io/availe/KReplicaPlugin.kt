package io.availe

import io.availe.gradle.applyJvmConvention
import io.availe.gradle.applyKmpConvention
import org.gradle.api.Plugin
import org.gradle.api.Project

class KReplicaPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.logger.info("--- KREPLICA-PLUGIN: Applying 'io.availe.kreplica' to project ${target.path} ---")

        val projectVersion = "1.0.0"

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