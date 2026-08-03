package dev.kiki.openapi.ktor

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

class OpenApiKtorPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create("openApiKtor", OpenApiKtorExtension::class.java)

        project.afterEvaluate {
            if (extension.specs.isEmpty()) return@afterEvaluate

            val aggregate = project.tasks.register("generateOpenApiKtor", DefaultTask::class.java) { task ->
                task.group = "openapi"
                task.description = "Generates Kotlin models and Ktor APIs from all configured OpenAPI specifications."
            }

            extension.specs.forEach { spec ->
                require(spec.inputSpec.isPresent) { "openApiKtor.spec(\"${spec.name}\") must define inputSpec." }
                require(spec.packageName.isPresent) { "openApiKtor.spec(\"${spec.name}\") must define packageName." }

                val output = project.layout.buildDirectory.dir("generated/openapi/${spec.name}")
                val taskName = "generate${spec.name.replaceFirstChar { it.uppercase() }}OpenApiKtor"
                val generateTask = project.tasks.register(taskName, GenerateOpenApiKtorTask::class.java) { task ->
                    task.specFile.set(spec.inputSpec)
                    task.basePackage.set(spec.packageName)
                    task.includeTags.set(spec.includeTags)
                    task.generatedOutput.set(output)
                    task.inputSpec.set(spec.inputSpec.map { it.asFile.absolutePath })
                    task.outputDir.set(output.map { it.asFile.absolutePath })
                    task.packageName.set(spec.packageName)
                    task.apiPackage.set(spec.packageName.map { "$it.api" })
                    task.modelPackage.set(spec.packageName.map { "$it.model" })
                    task.invokerPackage.set(spec.packageName.map { "$it.core" })
                    task.validateSpec.set(spec.validateSpec)
                    // `includeTags` is reserved by OpenAPI Generator and causes it to suppress
                    // operations. Keep this plugin's filtering option in a private namespace.
                    task.additionalProperties.put("openApiKtorIncludeTags", spec.includeTags.map { it.joinToString(",") })
                    task.additionalProperties.put("baseUrlExpression", spec.baseUrlExpression)
                    task.additionalProperties.put("useHilt", spec.useHilt.map(Boolean::toString))
                }
                aggregate.configure { task -> task.dependsOn(generateTask) }
                registerGeneratedSources(project, output, generateTask)
            }
        }
    }

    private fun registerGeneratedSources(
        project: Project,
        output: org.gradle.api.provider.Provider<org.gradle.api.file.Directory>,
        generateTask: TaskProvider<GenerateOpenApiKtorTask>,
    ) {
        val sourceDirectory = output.map { it.dir("src/main/kotlin").asFile }

        project.plugins.withId("com.android.application") {
            project.extensions.getByType(ApplicationExtension::class.java)
                .sourceSets.getByName("main").java.srcDir(sourceDirectory)
        }
        project.plugins.withId("com.android.library") {
            project.extensions.getByType(LibraryExtension::class.java)
                .sourceSets.getByName("main").java.srcDir(sourceDirectory)
        }
        project.tasks.withType(KotlinCompile::class.java).configureEach { task ->
            task.dependsOn(generateTask)
            task.source(sourceDirectory)
        }
        project.tasks.matching { it.name == "preBuild" }.configureEach { task -> task.dependsOn(generateTask) }
    }
}
