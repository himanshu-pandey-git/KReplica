package io.availe

import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import javax.inject.Inject

@DslMarker
annotation class KReplicaDsl

abstract class KReplicaExtension @Inject constructor(objects: ObjectFactory) {

    abstract val primaryModelJson: RegularFileProperty

    internal val contextProjects: ListProperty<Project> = objects.listProperty(Project::class.java)
    internal val contextModelJsonsInternal: ConfigurableFileCollection = objects.fileCollection()

    val contextModelJsons: ConfigurableFileCollection = contextModelJsonsInternal

    fun fromContext(contextProject: Project) {
        contextProjects.add(contextProject)
    }
}