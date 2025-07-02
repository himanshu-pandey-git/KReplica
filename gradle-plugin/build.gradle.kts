plugins {
    alias(libs.plugins.kotlinJvm)
    `java-gradle-plugin`
    `kotlin-dsl`
    `maven-publish`
}

kotlin {
    jvmToolchain(21)
}

gradlePlugin {
    plugins {
        create("kreplica") {
            id = "io.availe.kreplica"
            implementationClass = "io.availe.KReplicaPlugin"
        }
    }
}

dependencies {
    implementation(projects.codegenModels)
    implementation(projects.modelKspProcessor)
    implementation(projects.codegenRuntime)
    implementation(projects.modelKspAnnotations)
    compileOnly(libs.kotlin.gradle.plugin)
    compileOnly(libs.kotlin.gradle.plugin.api)
    compileOnly(libs.ksp.gradle)
}
