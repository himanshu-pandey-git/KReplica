package io.availe.ksp

import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

interface KspDependencies {
    fun ksp(dep: Any)
}

fun KotlinTarget.kspDependencies(block: KspDependencies.() -> Unit) {
    val configurationName = "ksp${targetName.replaceFirstChar { it.uppercaseChar() }}"
    project.dependencies {
        object : KspDependencies {
            override fun ksp(dep: Any) {
                add(configurationName, dep)
            }
        }.block()
    }
}

fun KotlinMultiplatformExtension.kspDependenciesForAllTargets(block: KspDependencies.() -> Unit) {
    targets.configureEach { if (targetName != "metadata") kspDependencies(block) }
}

fun KotlinMultiplatformExtension.commonMainKspDependencies(
    project: Project,
    projectVersion: String,
    block: KspDependencies.() -> Unit
) {
    project.dependencies {
        add("kspCommonMainMetadata", "io.availe:model-ksp-processor:$projectVersion")
    }

    sourceSets.named("commonMain").configure {
        kotlin.srcDir("build/generated/ksp/metadata/commonMain/kotlin")
    }

    project.tasks.withType(KotlinCompilationTask::class.java).configureEach {
        if (name != "kspCommonMainKotlinMetadata") dependsOn("kspCommonMainKotlinMetadata")
    }
}