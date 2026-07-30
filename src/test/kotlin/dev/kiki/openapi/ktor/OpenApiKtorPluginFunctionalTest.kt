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
    fun `generates Ktor api and serializable model that compiles`() {
        projectDir.resolve("settings.gradle.kts").writeText("rootProject.name = \"functional-test\"")
        projectDir.resolve("build.gradle.kts").writeText(
            """
            plugins {
                id("io.github.peterqin18.openapi-ktor")
                kotlin("jvm") version "2.2.10"
            }

            repositories { mavenCentral() }

            dependencies {
                implementation("io.ktor:ktor-client-core:3.2.0")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")
            }

            openApiKtor {
                spec("sample") {
                    inputSpec.set(layout.projectDirectory.file("openapi.yaml"))
                    packageName.set("com.example.generated")
                    useHilt.set(false)
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
            servers:
              - url: https://api.example.com
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
            .withArguments("compileKotlin", "--stacktrace")
            .build()

        assertTrue(result.output.contains("BUILD SUCCESSFUL"))
        val output = projectDir.resolve("build/generated/openapi/sample/src/main/kotlin/com/example/generated")
        val apiFile = output.resolve("api/GreetingApi.kt")
        assertTrue(apiFile.isFile)
        assertTrue(output.resolve("model/Greeting.kt").isFile)
        assertTrue(apiFile.readText().contains("const val BASE_URL"))
    }

    @Test
    fun `compiles generated APIs for Hilt and common OpenAPI operations`() {
        projectDir.resolve("settings.gradle.kts").writeText("rootProject.name = \"operations-test\"")
        projectDir.resolve("build.gradle.kts").writeText(
            """
            plugins {
                id("io.github.peterqin18.openapi-ktor")
                kotlin("jvm") version "2.2.10"
            }

            repositories { mavenCentral() }

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
                    // Deliberately leave useHilt at its default value of true.
                }
            }
            """.trimIndent(),
        )
        projectDir.resolve("service.yaml").writeText(
            """
            openapi: 3.0.3
            info:
              title: Pet service
              version: 1.0.0
            servers:
              - url: https://pets.example.com/v1
            paths:
              /pets/{id}:
                get:
                  operationId: getPet
                  tags: [Pets]
                  parameters:
                    - name: id
                      in: path
                      required: true
                      schema: { type: string }
                    - name: includeDetails
                      in: query
                      schema: { type: boolean }
                    - name: X-Trace-Id
                      in: header
                      schema: { type: string }
                  responses:
                    '200':
                      description: OK
                      content:
                        application/json:
                          schema: { ${'$'}ref: '#/components/schemas/Pet' }
                put:
                  operationId: replacePet
                  tags: [Pets]
                  parameters:
                    - name: id
                      in: path
                      required: true
                      schema: { type: string }
                  requestBody:
                    required: true
                    content:
                      application/json:
                        schema: { ${'$'}ref: '#/components/schemas/CreatePet' }
                  responses:
                    '200':
                      description: OK
                      content:
                        application/json:
                          schema: { ${'$'}ref: '#/components/schemas/Pet' }
                delete:
                  operationId: deletePet
                  tags: [Pets]
                  parameters:
                    - name: id
                      in: path
                      required: true
                      schema: { type: string }
                  responses:
                    '204': { description: Deleted }
                patch:
                  operationId: updatePet
                  tags: [Pets]
                  parameters:
                    - name: id
                      in: path
                      required: true
                      schema: { type: string }
                  requestBody:
                    required: true
                    content:
                      application/json:
                        schema: { ${'$'}ref: '#/components/schemas/CreatePet' }
                  responses:
                    '200':
                      description: OK
                      content:
                        application/json:
                          schema: { ${'$'}ref: '#/components/schemas/Pet' }
              /pets:
                post:
                  operationId: createPet
                  tags: [Pets]
                  requestBody:
                    required: true
                    content:
                      application/json:
                        schema: { ${'$'}ref: '#/components/schemas/CreatePet' }
                  responses:
                    '201':
                      description: Created
                      content:
                        application/json:
                          schema: { ${'$'}ref: '#/components/schemas/Pet' }
              /health:
                get:
                  operationId: health
                  tags: [System]
                  responses:
                    '204': { description: Healthy }
            components:
              schemas:
                Pet:
                  type: object
                  required: [id, name, state, labels]
                  properties:
                    id: { type: string }
                    name: { type: string }
                    state: { ${'$'}ref: '#/components/schemas/PetState' }
                    labels:
                      type: array
                      items: { type: string }
                    createdAt: { type: string, format: date-time }
                CreatePet:
                  type: object
                  required: [name]
                  properties:
                    name: { type: string }
                    labels:
                      type: array
                      items: { type: string }
                PetState:
                  type: string
                  enum: [ACTIVE, ARCHIVED]
            """.trimIndent(),
        )

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("compileKotlin", "--stacktrace")
            .build()

        assertTrue(result.output.contains("BUILD SUCCESSFUL"))
        val apiSources = projectDir.resolve("build/generated/openapi/service/src/main/kotlin/com/example/service/api")
            .walkTopDown()
            .filter(File::isFile)
            .joinToString("\n") { it.readText() }
        assertTrue(apiSources.contains("@Inject constructor"))
        assertTrue(apiSources.contains("HttpMethod.Get"), apiSources)
        assertTrue(apiSources.contains("HttpMethod.Post"))
        assertTrue(apiSources.contains("HttpMethod.Put"))
        assertTrue(apiSources.contains("HttpMethod.Delete"))
        assertTrue(apiSources.contains("HttpMethod.Patch"))
        assertTrue(!apiSources.contains("fun health"))
    }

    @Test
    fun `aggregate task generates every configured specification`() {
        projectDir.resolve("settings.gradle.kts").writeText("rootProject.name = \"aggregate-test\"")
        projectDir.resolve("build.gradle.kts").writeText(
            """
            plugins { id("io.github.peterqin18.openapi-ktor") }

            openApiKtor {
                spec("catalog") {
                    inputSpec.set(layout.projectDirectory.file("catalog.yaml"))
                    packageName.set("com.example.catalog")
                }
                spec("identity") {
                    inputSpec.set(layout.projectDirectory.file("identity.yaml"))
                    packageName.set("com.example.identity")
                }
            }
            """.trimIndent(),
        )
        val specification =
            """
            openapi: 3.0.3
            info: { title: Aggregate, version: 1.0.0 }
            paths:
              /status:
                get:
                  operationId: status
                  responses:
                    '204': { description: OK }
            """.trimIndent()
        projectDir.resolve("catalog.yaml").writeText(specification)
        projectDir.resolve("identity.yaml").writeText(specification)

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("generateOpenApiKtor", "--stacktrace")
            .build()

        assertTrue(result.output.contains("BUILD SUCCESSFUL"))
        assertTrue(projectDir.resolve("build/generated/openapi/catalog/src/main/kotlin").isDirectory)
        assertTrue(projectDir.resolve("build/generated/openapi/identity/src/main/kotlin").isDirectory)
    }
}
