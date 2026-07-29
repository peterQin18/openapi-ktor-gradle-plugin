package dev.kiki.openapi.ktor

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.openapitools.generator.gradle.plugin.tasks.GenerateTask
import javax.inject.Inject

abstract class GenerateOpenApiKtorTask @Inject constructor(objects: ObjectFactory) : GenerateTask(objects) {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val specFile: RegularFileProperty

    @get:Input
    abstract val basePackage: Property<String>

    @get:OutputDirectory
    abstract val generatedOutput: DirectoryProperty

    @get:Input
    abstract val includeTags: SetProperty<String>

    init {
        group = "openapi"
        description = "Generates Kotlin models and Ktor APIs from an OpenAPI specification."
        generatorName.set("peterqin-kotlin-ktor")
    }
}
