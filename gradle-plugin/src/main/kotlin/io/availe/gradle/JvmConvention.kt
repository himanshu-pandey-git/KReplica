package io.availe.gradle

import com.google.devtools.ksp.gradle.KspExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

fun applyJvmConvention(project: Project, projectVersion: String) {
    val kotlinExtension = project.extensions.getByType(KotlinProjectExtension::class.java)

    val genJvmKspKotlin = project.layout.buildDirectory.dir("generated/ksp/main/kotlin")
    val genJvmKspKotlinFiles = project.files(genJvmKspKotlin)

    kotlinExtension.sourceSets.named("main").configure {
        kotlin.srcDir(genJvmKspKotlinFiles)
    }
    genJvmKspKotlinFiles.builtBy("kspKotlin")

    project.dependencies.apply {
        add("implementation", "io.availe:model-ksp-annotations:$projectVersion")
        add("ksp", "io.availe:model-ksp-processor:$projectVersion")
    }

    val metadataConfig = project.configurations.getByName("kreplicaMetadata")
    val metadataFilesProvider = project.provider {
        metadataConfig.files.joinToString(java.io.File.pathSeparator)
    }
    project.extensions.configure(KspExtension::class.java) {
        arg("kreplica.metadataFiles", metadataFilesProvider)
    }

    project.tasks.withType<KotlinCompile> {
        dependsOn("kspKotlin")
    }
}