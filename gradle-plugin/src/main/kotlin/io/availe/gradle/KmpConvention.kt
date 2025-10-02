package io.availe.gradle

import com.google.devtools.ksp.gradle.KspExtension
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

fun applyKmpConvention(project: Project, projectVersion: String) {
    val kmpExt = project.extensions.findByType(KotlinMultiplatformExtension::class.java) ?: return

    val genCommonKspKotlin = project.layout.buildDirectory.dir("generated/ksp/metadata/commonMain/kotlin")

    kmpExt.sourceSets.named("commonMain").configure {
        kotlin.srcDir(genCommonKspKotlin)
        dependencies {
            implementation("io.availe:model-ksp-annotations:$projectVersion")
        }
    }

    project.dependencies.add("kspCommonMainMetadata", "io.availe:model-ksp-processor:$projectVersion")

    val metadataConfig = project.configurations.getByName("kreplicaMetadata")
    val metadataFilesProvider = project.provider {
        metadataConfig.files.joinToString(java.io.File.pathSeparator)
    }
    project.extensions.configure(KspExtension::class.java) {
        arg("kreplica.metadataFiles", metadataFilesProvider)
    }

    project.tasks.matching {
        it.name.startsWith("compile") && it.name.contains("Kotlin") && !it.name.contains("Metadata", ignoreCase = true)
    }.configureEach {
        dependsOn("kspCommonMainKotlinMetadata")
    }
}