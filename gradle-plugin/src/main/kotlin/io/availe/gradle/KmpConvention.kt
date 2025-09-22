package io.availe.gradle

import com.google.devtools.ksp.gradle.KspExtension
import io.availe.gradle.KReplicaModelAttribute.KREPLICA_MODEL_TYPE_ATTRIBUTE
import io.availe.gradle.KReplicaModelAttribute.MODELS_JSON_TYPE
import io.availe.models.KReplicaPaths
import org.gradle.api.Project
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import java.io.File

fun applyKmpConvention(project: Project, projectVersion: String) {
    val kmpExt = project.extensions.findByType(KotlinMultiplatformExtension::class.java) ?: return

    kmpExt.sourceSets.named("commonMain").configure {
        kotlin.srcDir(project.layout.buildDirectory.dir("generated/ksp/metadata/commonMain/kotlin"))
    }

    kmpExt.sourceSets.getByName("commonMain").dependencies {
        implementation("io.availe:model-ksp-annotations:$projectVersion")
    }

    project.dependencies.apply {
        add("kspCommonMainMetadata", "io.availe:model-ksp-processor:$projectVersion")
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
        "${KReplicaPaths.KSP_GENERATED_DIR}/${KReplicaPaths.KSP_METADATA_DIR}/${KReplicaPaths.RESOURCES_DIR}/${KReplicaPaths.MODELS_JSON_FILE}"
    )

    project.artifacts {
        add("kreplicaModelsElements", primaryModelJsonProvider) {
            builtBy("kspCommonMainKotlinMetadata")
        }
    }

    val metadataConfig = project.configurations.getByName("kreplicaMetadata")
    val metadataFilesProvider = project.provider {
        metadataConfig.files.joinToString(File.pathSeparator)
    }
    project.extensions.configure(KspExtension::class.java) {
        arg("kreplica.metadataFiles", metadataFilesProvider)
    }

    kmpExt.targets.configureEach {
        compilations.configureEach {
            if (name == KotlinCompilation.MAIN_COMPILATION_NAME) {
                compileTaskProvider.get().dependsOn("kspCommonMainKotlinMetadata")
            }
        }
    }
}