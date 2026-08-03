package dev.kiki.openapi.ktor

import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class OpenApiKtorPluginFunctionalTest {
    @TempDir
    lateinit var projectDir: File

    @Test
    fun `compiles generated APIs for Hilt and common OpenAPI operations`() {
        writeBuild(
            """
            dependencies {
                implementation("io.ktor:ktor-client-core:3.2.0")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")
                implementation("javax.inject:javax.inject:1")
            }
            openApiKtor {
                spec("service") {
                    inputSpec.set(layout.projectDirectory.file("service.yaml"))
                    packageName.set("com.example.service")
                    includeTags.set(setOf("Pets"))
                }
            }
            """,
        )
        projectDir.resolve("service.yaml").writeText(
            """
            openapi: 3.0.3
            info: { title: Pet service, version: 1.0.0 }
            servers: [{ url: https://pets.example.com/v1 }]
            paths:
              /pets/{id}:
                get:
                  operationId: getPet
                  tags: [Pets]
                  parameters:
                    - { name: id, in: path, required: true, schema: { type: string } }
                    - { name: includeDetails, in: query, schema: { type: boolean } }
                    - { name: X-Trace-Id, in: header, schema: { type: string } }
                  responses:
                    '200':
                      description: OK
                      content: { application/json: { schema: { ${'$'}ref: '#/components/schemas/Pet' } } }
                put:
                  operationId: replacePet
                  tags: [Pets]
                  parameters: [{ name: id, in: path, required: true, schema: { type: string } }]
                  requestBody:
                    required: true
                    content: { application/json: { schema: { ${'$'}ref: '#/components/schemas/Pet' } } }
                  responses: { '200': { description: OK } }
                delete:
                  operationId: deletePet
                  tags: [Pets]
                  parameters: [{ name: id, in: path, required: true, schema: { type: string } }]
                  responses: { '204': { description: Deleted } }
                patch:
                  operationId: updatePet
                  tags: [Pets]
                  parameters: [{ name: id, in: path, required: true, schema: { type: string } }]
                  responses: { '200': { description: OK } }
              /pets:
                post:
                  operationId: createPet
                  tags: [Pets]
                  responses: { '201': { description: Created } }
              /health:
                get:
                  operationId: health
                  tags: [System]
                  responses: { '204': { description: Healthy } }
            components:
              schemas:
                Pet:
                  type: object
                  required: [id]
                  properties: { id: { type: string } }
            """.trimIndent(),
        )

        val result = run("compileKotlin")
        assertTrue(result.output.contains("BUILD SUCCESSFUL"))
        val apiSources = projectDir.resolve("build/generated/openapi/service/src/main/kotlin/com/example/service/api")
            .walkTopDown().filter(File::isFile).joinToString("\n") { it.readText() }
        assertTrue(apiSources.contains("@Inject constructor"))
        listOf("Get", "Post", "Put", "Delete", "Patch").forEach {
            assertTrue(apiSources.contains("HttpMethod.$it"), apiSources)
        }
        assertTrue(apiSources.contains("encodedPath = \"/pets/${'$'}id\""), apiSources)
        assertTrue(!apiSources.contains("fun health"), apiSources)
    }

    @Test
    fun `generated API supports a build variant base URL and handles non success responses`() {
        writeBuild(
            """
            dependencies {
                implementation("io.ktor:ktor-client-core:3.0.3")
                implementation("io.ktor:ktor-client-content-negotiation:3.0.3")
                implementation("io.ktor:ktor-serialization-kotlinx-json:3.0.3")
                testImplementation("io.ktor:ktor-client-mock:3.0.3")
                testImplementation(kotlin("test"))
            }
            openApiKtor {
                spec("sample") {
                    inputSpec.set(layout.projectDirectory.file("openapi.yaml"))
                    packageName.set("com.example.generated")
                    useHilt.set(false)
                    baseUrlExpression.set("com.example.generated.BuildConfig.API_BASE_URL")
                }
            }
            """,
        )
        projectDir.resolve("src/main/kotlin/com/example/generated/BuildConfig.kt").apply {
            parentFile.mkdirs()
            writeText("""package com.example.generated

                object BuildConfig { const val API_BASE_URL = "https://api.example.com" }
            """.trimIndent())
        }
        projectDir.resolve("openapi.yaml").writeText(sampleOpenApi)
        projectDir.resolve("src/test/kotlin/com/example/generated/GreetingApiTest.kt").apply {
            parentFile.mkdirs()
            writeText(
                """
                package com.example.generated

                import com.example.generated.api.GreetingApi
                import io.ktor.client.HttpClient
                import io.ktor.client.engine.mock.MockEngine
                import io.ktor.client.engine.mock.respond
                import io.ktor.http.HttpStatusCode
                import kotlinx.coroutines.runBlocking
                import kotlin.test.Test
                import kotlin.test.assertEquals
                import kotlin.test.assertFailsWith

                class GreetingApiTest {
                    @Test fun nonSuccessResponseIncludesTheHttpStatus() = runBlocking {
                        val client = HttpClient(MockEngine { respond("bad request", HttpStatusCode.BadRequest) })
                        val error = assertFailsWith<GreetingApi.ApiHttpException> {
                            GreetingApi(client).getGreeting("missing")
                        }
                        assertEquals(400, error.statusCode)
                        assertEquals("bad request", error.responseBody)
                    }
                }
                """.trimIndent(),
            )
        }

        val result = run("test")
        assertTrue(result.output.contains("BUILD SUCCESSFUL"))
        val generatedApi = projectDir.resolve(
            "build/generated/openapi/sample/src/main/kotlin/com/example/generated/api/GreetingApi.kt",
        ).readText()
        assertTrue(generatedApi.contains("private val baseUrl: String = com.example.generated.BuildConfig.API_BASE_URL"))
        assertTrue(generatedApi.contains("if (!response.status.isSuccess())"))
        assertTrue(generatedApi.contains("class ApiHttpException"))
    }

    @Test
    fun `aggregate task generates every configured specification`() {
        writeBuild(
            """
            openApiKtor {
                spec("catalog") { inputSpec.set(layout.projectDirectory.file("catalog.yaml")); packageName.set("com.example.catalog") }
                spec("identity") { inputSpec.set(layout.projectDirectory.file("identity.yaml")); packageName.set("com.example.identity") }
            }
            """,
        )
        val specification = """
            openapi: 3.0.3
            info: { title: Aggregate, version: 1.0.0 }
            paths: { /status: { get: { operationId: status, responses: { '204': { description: OK } } } } }
        """.trimIndent()
        projectDir.resolve("catalog.yaml").writeText(specification)
        projectDir.resolve("identity.yaml").writeText(specification)

        val result = run("generateOpenApiKtor")
        assertTrue(result.output.contains("BUILD SUCCESSFUL"))
        assertTrue(projectDir.resolve("build/generated/openapi/catalog/src/main/kotlin").isDirectory)
        assertTrue(projectDir.resolve("build/generated/openapi/identity/src/main/kotlin").isDirectory)
    }

    private fun writeBuild(configuration: String) {
        projectDir.resolve("settings.gradle.kts").writeText("rootProject.name = \"functional-test\"")
        projectDir.resolve("build.gradle.kts").writeText(
            """
            plugins {
                id("io.github.peterqin18.openapi-ktor")
                kotlin("jvm") version "2.2.10"
            }
            repositories { mavenCentral() }
            $configuration
            """.trimIndent(),
        )
    }

    private fun run(task: String) = GradleRunner.create()
        .withProjectDir(projectDir)
        .withPluginClasspath()
        .withArguments(task, "--stacktrace")
        .build()

    private val sampleOpenApi = """
        openapi: 3.0.3
        info: { title: Sample, version: 1.0.0 }
        servers: [{ url: https://test-api.example.com }]
        paths:
          /greetings/{id}:
            get:
              operationId: getGreeting
              tags: [Greeting]
              parameters: [{ name: id, in: path, required: true, schema: { type: string } }]
              responses:
                '200':
                  description: OK
                  content: { application/json: { schema: { ${'$'}ref: '#/components/schemas/Greeting' } } }
        components:
          schemas:
            Greeting:
              type: object
              required: [message]
              properties: { message: { type: string } }
    """.trimIndent()
}
