# OpenAPI Ktor Gradle Plugin

从 OpenAPI 3 规范生成 **Kotlin `@Serializable` Model** 与 **Ktor `suspend` API** 的 Gradle Plugin。它不生成业务 Repository、Hilt Module 或 DTO → UI Mapper；这些属于应用的业务层，应保持手写。

## 安装

发布到 Gradle Plugin Portal 后：

```kotlin
plugins {
    id("io.github.peterqin.openapi-ktor") version "0.1.0"
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

生成的 API 接收 `HttpClient` 和可选 `baseUrl`。如果不传 `baseUrl`，请通过 Ktor 的 `DefaultRequest` 配置基地址。

```kotlin
val api = QuestApi(httpClient, baseUrl = "https://api.example.com")
val response = api.getQuest(id = "42")
```

Hilt 是可选的：在你的 `NetworkModule` 中提供 `HttpClient`，再手写需要的 API/Repository binding。不要把业务 Repository 交给 OpenAPI Generator 生成。

## 本地开发

```bash
./gradlew test
./gradlew publishToMavenLocal
./gradlew publishPlugins --validate-only
```

发布到 Plugin Portal 需要配置 `GRADLE_PUBLISH_KEY` 和 `GRADLE_PUBLISH_SECRET`，仅通过 GitHub Actions Secret 注入；不要提交密钥。

## 许可

[MIT](LICENSE)
