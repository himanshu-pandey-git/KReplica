plugins {
    alias(libs.plugins.kotlinJvm)
    `java-gradle-plugin`
    `kotlin-dsl`
    `maven-publish`
}

group = "io.availe"
version = "1.0.0"

gradlePlugin {
    plugins {
        create("kreplica") {
            id = "io.availe.kreplica"
            implementationClass = "io.availe.KReplicaPlugin"
        }
    }
}

dependencies {
    implementation(projects.codegen)
    implementation(projects.modelKspProcessor)
    implementation(projects.codegenRuntime)
    implementation(projects.modelKspAnnotations)
    compileOnly(libs.kotlin.gradle.plugin)
    compileOnly(libs.kotlin.gradle.plugin.api)
    compileOnly(libs.ksp.gradle)
}
