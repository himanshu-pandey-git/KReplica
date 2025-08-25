plugins {
    alias(libs.plugins.kotlinJvm)
    `maven-publish`
    application
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(projects.modelKspAnnotations)
    implementation(projects.codegenModels)
    implementation(libs.kotlinpoet)
    implementation(libs.kotlinpoet.metadata)
    implementation(libs.kotlinpoet.metadata.specs)
    implementation(libs.kotlinpoet.ksp)
    implementation(libs.ksp.symbol.processing.api)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.logback)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.datetime)

}

application {
    mainClass.set("io.availe.ApplicationKt")
}