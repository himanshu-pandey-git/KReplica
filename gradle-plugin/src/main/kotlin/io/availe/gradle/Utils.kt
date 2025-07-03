package io.availe.gradle

import io.availe.models.KReplicaPaths
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

fun registerKReplicaCodegenTask(
    project: Project,
    kspTaskName: String,
    projectVersion: String,
    primaryModelJsonProvider: Provider<RegularFile>
): TaskProvider<JavaExec> {
    val cleanKReplicaOutput = project.tasks.register("cleanKReplicaOutput", Delete::class.java) {
        delete(
            project.layout.buildDirectory.dir(
                "${KReplicaPaths.KSP_GENERATED_DIR}/${KReplicaPaths.KSP_JVM_DIR}"
            ),
            project.layout.buildDirectory.dir(
                "${KReplicaPaths.KSP_GENERATED_DIR}/${KReplicaPaths.KSP_METADATA_DIR}"
            ),
            project.layout.buildDirectory.dir(
                KReplicaPaths.KOTLIN_POET_GENERATED_DIR
            )
        )
    }

    project.tasks.matching { it.name == kspTaskName }.configureEach {
        dependsOn(cleanKReplicaOutput)
    }

    val codegenConfiguration = project.configurations.create("kreplicaCodegen") {
        isCanBeConsumed = false
        isCanBeResolved = true
    }
    project.dependencies.add(codegenConfiguration.name, "io.availe:codegen-runtime:$projectVersion")

    val kotlinPoetOutputDirProvider: Provider<Directory> = project.layout.buildDirectory.dir(
        KReplicaPaths.KOTLIN_POET_GENERATED_DIR
    )

    val kspKotlinDirProvider: Provider<Directory> = project.layout.buildDirectory.dir(
        if (kspTaskName == "kspCommonMainKotlinMetadata") {
            "${KReplicaPaths.KSP_GENERATED_DIR}/${KReplicaPaths.KSP_METADATA_DIR}/${KReplicaPaths.KOTLIN_DIR}"
        } else {
            "${KReplicaPaths.KSP_GENERATED_DIR}/${KReplicaPaths.KSP_JVM_DIR}/${KReplicaPaths.KOTLIN_DIR}"
        }
    )

    val cleanKReplicaStubs = project.tasks.register("cleanKReplicaStubs", Delete::class.java) {
        group = "kreplica"
        description = "Deletes temporary KReplica stub files after final code is generated."
        delete(kspKotlinDirProvider)
    }

    val runCodegen = project.tasks.register("runKReplicaCodegen", JavaExec::class.java) {
        group = "kreplica"
        description = "Runs the KReplica code generator"
        classpath = codegenConfiguration
        mainClass.set("io.availe.ApplicationKt")
        dependsOn(kspTaskName)
        finalizedBy(cleanKReplicaStubs)
        outputs.dir(kotlinPoetOutputDirProvider)
            .withPropertyName("outputDir")

        val kreplicaClasspath = project.configurations.getByName("kreplicaClasspath")

        val provider = project.objects.newInstance(CodegenArgumentProvider::class.java).apply {
            outputDir.set(kotlinPoetOutputDirProvider)
            primaryModelJson.set(primaryModelJsonProvider)
            allModelFiles.from(kreplicaClasspath)
        }
        argumentProviders.add(provider)
    }

    project.extensions.findByType(KotlinMultiplatformExtension::class.java)?.let { kmpExt ->
        kmpExt.targets.configureEach {
            compilations.configureEach {
                if (compilationName == KotlinCompilation.MAIN_COMPILATION_NAME) {
                    compileTaskProvider.configure {
                        if (name != kspTaskName) {
                            dependsOn(runCodegen)
                            mustRunAfter(cleanKReplicaStubs)
                        }
                    }
                }
            }
        }
    } ?: project.tasks.withType<KotlinCompile>().configureEach {
        if (name == "compileKotlin" && name != kspTaskName) {
            dependsOn(runCodegen)
            mustRunAfter(cleanKReplicaStubs)
        }
    }

    return runCodegen
}