@file:OptIn(ExperimentalWasmDsl::class)

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    `maven-publish`
}

group = "io.availe"
version = "1.0.0"

kotlin {
    jvm()
    iosX64()
    iosArm64()
    iosSimulatorArm64()
    linuxX64()
    macosX64()
    macosArm64()
    wasmJs {
        browser {
            binaries.executable()
        }
        nodejs()
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(projects.codegenModels)
            }
        }

        val jvmMain by getting {}

        val iosX64Main by getting

        val iosArm64Main by getting
        val iosSimulatorArm64Main by getting
        val wasmJsMain by getting
        val linuxX64Main by getting
        val macosX64Main by getting
        val macosArm64Main by getting
    }
}
