package io.github.peterqin.openapi.ktor

import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class OpenApiKtorPluginFunctionalTest {
    @TempDir
    lateinit var projectDir: File

    @Test
    fun `generates Ktor api and serializable model`() {
        projectDir.resolve("settings.gradle.kts").writeText("rootProject.name = \"functional-test\"")
        projectDir.resolve("build.gradle.kts").writeText(
            """
            plugins {
                id("io.github.peterqin.openapi-ktor")
                kotlin("jvm") version "2.2.10"
            }

            repositories { mavenCentral() }

            openApiKtor {
                spec("sample") {
                    inputSpec.set(layout.projectDirectory.file("openapi.yaml"))
                    packageName.set("com.example.generated")
                }
            }
            """.trimIndent(),
        )
        projectDir.resolve("openapi.yaml").writeText(
            """
            openapi: 3.0.3
            info:
              title: Sample
              version: 1.0.0
            paths:
              /greetings/{id}:
                get:
                  operationId: getGreeting
                  tags: [Greeting]
                  parameters:
                    - name: id
                      in: path
                      required: true
                      schema: { type: string }
                  responses:
                    '200':
                      description: OK
                      content:
                        application/json:
                          schema:
                            ${'$'}ref: '#/components/schemas/Greeting'
            components:
              schemas:
                Greeting:
                  type: object
                  required: [message]
                  properties:
                    message: { type: string }
            """.trimIndent(),
        )

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("generateSampleOpenApiKtor", "--stacktrace")
            .build()

        assertTrue(result.output.contains("BUILD SUCCESSFUL"))
        val output = projectDir.resolve("build/generated/openapi/sample/src/main/kotlin/com/example/generated")
        assertTrue(output.resolve("api/GreetingApi.kt").isFile)
        assertTrue(output.resolve("model/Greeting.kt").isFile)
    }
}
