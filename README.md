# OpenAPI Ktor Gradle Plugin

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

By default, the OpenAPI `servers` / `basePath` value becomes `BASE_URL`. With Hilt enabled, a generated API looks like this:

```kotlin
@Singleton
class QuestApi @Inject constructor(
    private val httpClient: HttpClient,
)
```

Your application must provide the `HttpClient`; the plugin does not create a network module or repositories. With `useHilt.set(false)`, generated APIs instead accept `HttpClient` and an optional `baseUrl` constructor parameter.

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

# 中文说明

该插件根据 OpenAPI 3 的 YAML 或 JSON 规范生成 Kotlin `@Serializable` 数据模型和 Ktor `suspend` API。它不会生成 Repository、依赖注入 Module 或 UI Mapper，这些应由业务项目自行维护。

## 前置条件

- Gradle 8.11 或更高版本
- JDK 17
- Kotlin JVM 或 Android 项目
- 一份 OpenAPI 3 的 YAML / JSON 规范文件

## 安装

当 `0.1.0` 通过 Gradle Plugin Portal 审核后，在生成代码所属模块应用插件：

```kotlin
plugins {
    id("io.github.peterqin18.openapi-ktor") version "0.1.0"
}
```

生成代码依赖 Ktor 与 Kotlin Serialization，请在使用方模块中添加兼容版本：

```kotlin
dependencies {
    implementation("io.ktor:ktor-client-core:<ktor-version>")
    implementation("io.ktor:ktor-client-content-negotiation:<ktor-version>")
    implementation("io.ktor:ktor-serialization-kotlinx-json:<ktor-version>")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:<serialization-version>")
}
```

默认 `useHilt = true` 时，请配置 Hilt / Dagger，并提供 `HttpClient` 绑定；不使用 Hilt / Dagger 的项目设为 `false`。

## 配置

```kotlin
openApiKtor {
    spec("quests") {
        inputSpec.set(layout.projectDirectory.file("src/main/openapi/quests.yaml"))
        packageName.set("com.example.generated.quests")
        includeTags.set(setOf("Quest")) // 可选：仅生成指定 Tag
        validateSpec.set(true)
        useHilt.set(true) // 默认：生成 @Inject / @Singleton API
    }
}
```

运行全部 spec 或某一份 spec：

```bash
./gradlew generateOpenApiKtor
./gradlew generateQuestsOpenApiKtor
```

生成文件位于 `build/generated/openapi/<spec-name>/src/main/kotlin/`，插件会自动加入 Kotlin / Android 源码集，并让编译任务依赖生成任务。不要提交生成目录。

## 生成结果

例如 OpenAPI 中的 `GET /pets/{id}` 会生成对应的 `suspend` 函数；路径、查询、Header、JSON Body 参数及 GET/POST/PUT/DELETE/PATCH 均受支持。默认 Hilt 模式下 API 通过构造函数接收 `HttpClient`。

## Configuration Cache

当前版本声明不支持 Gradle Configuration Cache；普通 Gradle / Android 构建和代码生成功能不受影响。
