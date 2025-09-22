package io.availe.gradle

import com.google.devtools.ksp.gradle.KspExtension
import io.availe.gradle.KReplicaModelAttribute.KREPLICA_MODEL_TYPE_ATTRIBUTE
import io.availe.gradle.KReplicaModelAttribute.MODELS_JSON_TYPE
import io.availe.models.KReplicaPaths
import org.gradle.api.Project
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.File

fun applyJvmConvention(project: Project, projectVersion: String) {
    val kotlinExtension = project.extensions.getByType(KotlinProjectExtension::class.java)
    kotlinExtension.sourceSets.named("main").configure {
        kotlin.srcDir(project.layout.buildDirectory.dir("generated/ksp/main/kotlin"))
    }

    project.dependencies.apply {
        add("implementation", "io.availe:model-ksp-annotations:$projectVersion")
        add("ksp", "io.availe:model-ksp-processor:$projectVersion")
    }

    project.configurations.create("kreplicaModelsElements") {
        isCanBeConsumed = true
        isCanBeResolved = false
        description = "Exposes the models.json file for other KReplica projects to consume."
        attributes {
            attribute(KREPLICA_MODEL_TYPE_ATTRIBUTE, MODELS_JSON_TYPE)
        }
    }

    val primaryModelJsonProvider: Provider<RegularFile> = project.layout.buildDirectory.file(
        "${KReplicaPaths.KSP_GENERATED_DIR}/${KReplicaPaths.KSP_JVM_DIR}/${KReplicaPaths.RESOURCES_DIR}/${KReplicaPaths.MODELS_JSON_FILE}"
    )

    project.artifacts {
        add("kreplicaModelsElements", primaryModelJsonProvider) {
            builtBy("kspKotlin")
        }
    }

    val metadataConfig = project.configurations.getByName("kreplicaMetadata")
    val metadataFilesProvider = project.provider {
        metadataConfig.files.joinToString(File.pathSeparator)
    }
    project.extensions.configure(KspExtension::class.java) {
        arg("kreplica.metadataFiles", metadataFilesProvider)
    }

    project.tasks.withType<KotlinCompile> {
        dependsOn("kspKotlin")
    }
}