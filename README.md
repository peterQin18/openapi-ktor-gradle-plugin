# OpenAPI Ktor Gradle Plugin

[中文说明](README.zh-CN.md)

Generates Kotlin `@Serializable` models and Ktor `suspend` APIs from OpenAPI 3 specifications. The plugin intentionally does **not** generate repositories, dependency-injection modules, or UI mappers; those are application-specific code.

## Requirements

- Gradle 8.11 or later
- JDK 17
- Kotlin JVM or Android project
- An OpenAPI 3 specification in YAML or JSON

## Installation

After the plugin is accepted by the Gradle Plugin Portal, apply it in the module that owns the generated source set:

```kotlin
plugins {
    id("io.github.peterqin18.openapi-ktor") version "0.1.0"
}
```

The generated source uses Ktor and Kotlin Serialization. Add compatible versions of these dependencies to the consuming module:

```kotlin
dependencies {
    implementation("io.ktor:ktor-client-core:<ktor-version>")
    implementation("io.ktor:ktor-client-content-negotiation:<ktor-version>")
    implementation("io.ktor:ktor-serialization-kotlinx-json:<ktor-version>")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:<serialization-version>")
}
```

When `useHilt` is `true` (the default), also configure Hilt or Dagger so that `javax.inject.Inject` and `javax.inject.Singleton` are available, and provide a `HttpClient` binding. Set `useHilt` to `false` if the project does not use Hilt/Dagger.

## Configuration

```kotlin
openApiKtor {
    spec("quests") {
        inputSpec.set(layout.projectDirectory.file("src/main/openapi/quests.yaml"))
        packageName.set("com.example.generated.quests")
        includeTags.set(setOf("Quest")) // Optional: generate selected tags only.
        validateSpec.set(true)
        useHilt.set(true) // Default: API classes use @Inject and @Singleton.
        // Optional Kotlin expression. In Android, this normally uses BuildConfig.
        baseUrlExpression.set("com.example.app.BuildConfig.API_BASE_URL")
    }
}
```

Run either the aggregate task or a task for one named specification:

```bash
./gradlew generateOpenApiKtor
./gradlew generateQuestsOpenApiKtor
```

Generated files are written to:

```text
build/generated/openapi/quests/src/main/kotlin/
├── com/example/generated/quests/api/
└── com/example/generated/quests/model/
```

The plugin adds this directory to Kotlin and Android source sets and makes compilation depend on generation. Do not commit the generated directory.

## Generated API usage

By default, the OpenAPI `servers` / `basePath` value becomes `BASE_URL`. To use different Android build-variant environments, set `baseUrlExpression` to a Kotlin expression such as `com.example.app.BuildConfig.API_BASE_URL`; the expression is used at runtime instead of the URL embedded in the OpenAPI file.

With Hilt enabled, a generated API looks like this:

```kotlin
@Singleton
class QuestApi @Inject constructor(
    private val httpClient: HttpClient,
)
```

Your application must provide the `HttpClient`; the plugin does not create a network module or repositories. With `useHilt.set(false)`, generated APIs instead accept `HttpClient` and an optional `baseUrl` constructor parameter.

Each generated API checks the response status before deserializing the body. Non-2xx responses throw `ApiHttpException`, nested under the generated API class (for example, `QuestApi.ApiHttpException`), with `statusCode` and `responseBody` properties.

## Publishing and local development

```bash
./gradlew test
./gradlew publishToMavenLocal
./gradlew publishPlugins -Pversion=0.1.0
```

The Gradle Plugin Portal accepts final versions only: do not publish a version ending in `-SNAPSHOT`. Publishing requires `GRADLE_PUBLISH_KEY` and `GRADLE_PUBLISH_SECRET` through local environment variables or CI secrets; never commit them.

## Configuration Cache

This release declares Configuration Cache as unsupported. The plugin currently registers generator tasks after project evaluation; regular builds work normally, but consumers should not enable Configuration Cache for this plugin yet.

## License

[MIT](LICENSE)
