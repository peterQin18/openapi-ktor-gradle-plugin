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
        // 可选：Kotlin 表达式。Android 通常配置为 BuildConfig 字段。
        baseUrlExpression.set("com.example.app.BuildConfig.API_BASE_URL")
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

默认情况下，OpenAPI 的 `servers` / `basePath` 会成为生成代码中的 `BASE_URL`。Android 需要测试、预发和正式环境时，设置 `baseUrlExpression` 指向 BuildConfig 字段即可；生成 API 会在运行时采用该表达式的值，而不是固定使用 Swagger 中的地址。

生成 API 会先检查 HTTP 状态码，再反序列化响应体。非 2xx 响应会抛出生成 API 内嵌的 `ApiHttpException`（例如 `QuestApi.ApiHttpException`），其中包含 `statusCode` 和 `responseBody`。

## Configuration Cache

当前版本声明不支持 Gradle Configuration Cache；普通 Gradle / Android 构建和代码生成功能不受影响。
