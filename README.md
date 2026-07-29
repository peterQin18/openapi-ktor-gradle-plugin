# OpenAPI Ktor Gradle Plugin

从 OpenAPI 3 规范生成 **Kotlin `@Serializable` Model** 与 **Ktor `suspend` API** 的 Gradle Plugin。它不生成业务 Repository、Hilt Module 或 DTO → UI Mapper；这些属于应用的业务层，应保持手写。

## 安装

发布到 Gradle Plugin Portal 后：

```kotlin
plugins {
    id("dev.kiki.openapi-ktor") version "0.1.0"
}
```

## 配置

```kotlin
openApiKtor {
    spec("quests") {
        inputSpec.set(layout.projectDirectory.file("src/main/openapi/quests.yaml"))
        packageName.set("com.example.generated.quests")
        includeTags.set(setOf("Quest")) // 可选：只生成指定 tag
        validateSpec.set(true)
        useHilt.set(true) // 默认 true：生成 @Singleton + @Inject constructor
    }
}
```

运行：

```bash
./gradlew generateOpenApiKtor
./gradlew generateQuestsOpenApiKtor
```

生成目录：

```text
build/generated/openapi/quests/src/main/kotlin/
├── com/example/generated/quests/api/
└── com/example/generated/quests/model/
```

Android application/library 或 Kotlin JVM 编译任务会依赖对应生成任务，并包含上述目录。生成目录应加入 `.gitignore`，而不是提交到 Git。

## 使用生成的 API

默认会从 OpenAPI `servers`/`basePath` 生成 `BASE_URL`，并生成可直接由 Hilt/Dagger 构造的 API：

```kotlin
@Singleton
class QuestApi @Inject constructor(
    private val httpClient: HttpClient,
)
```

因此只要你的 `NetworkModule` 已提供 `HttpClient`，生成的 API 会自动进入 Hilt 图。插件仍不生成业务 Repository、Hilt `@Module` 或 DTO → UI Mapper。

非 Hilt 项目可关闭注入代码：

```kotlin
openApiKtor {
    spec("quests") {
        useHilt.set(false)
    }
}
```

关闭后生成的 API 接收 `HttpClient` 和可选 `baseUrl` 构造参数。

## 本地开发

```bash
./gradlew test
./gradlew publishToMavenLocal
./gradlew publishPlugins --validate-only
```

发布到 Plugin Portal 需要配置 `GRADLE_PUBLISH_KEY` 和 `GRADLE_PUBLISH_SECRET`，仅通过 GitHub Actions Secret 注入；不要提交密钥。

## 许可

[MIT](LICENSE)
