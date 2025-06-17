plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.vanniktech.maven.publish) apply false
    alias(libs.plugins.versions)
    alias(libs.plugins.versionCatalogUpdate)
    `maven-publish`
}

allprojects {
    group   = providers.gradleProperty("GROUP").get()
    version = providers.gradleProperty("VERSION_NAME").get()
}


subprojects {
    apply(plugin = "com.github.ben-manes.versions")
    apply(plugin = "maven-publish")
}

subprojects {
    pluginManager.apply {
        withPlugin("org.jetbrains.kotlin.jvm")            { apply("com.vanniktech.maven.publish") }
        withPlugin("org.jetbrains.kotlin.multiplatform")  { apply("com.vanniktech.maven.publish") }
        withPlugin("java-gradle-plugin")                  { apply("com.vanniktech.maven.publish") }
    }
}

tasks.register("checkDependencyUpdates") {
    group = "versioning"
    description = "Checks for dependency updates (does not apply them)."

    dependsOn(":dependencyUpdates")
}

publishing {
    repositories {
        mavenLocal()
    }
}

private fun isVanniktech(t: Task) =
    t::class.java.name.startsWith("com.vanniktech.maven.publish")

tasks.register("publishViaVanniktech") {
    group = "publishing"
    dependsOn(subprojects.flatMap { p ->
        p.tasks.withType<Sign>().matching(::isVanniktech)
    })
    dependsOn(subprojects.flatMap { p ->
        p.tasks.withType<AbstractPublishToMaven>().matching(::isVanniktech)
    })
}

tasks.register("publishViaManual") {
    group = "publishing"
    dependsOn(subprojects.flatMap { p ->
        p.tasks.withType<AbstractPublishToMaven>()
            .matching {
                it.name.endsWith("PublicationToMavenLocal")
            }
    })
}