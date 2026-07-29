import org.gradle.api.publish.maven.MavenPublication

plugins {
    `java-gradle-plugin`
    `maven-publish`
    kotlin("jvm") version "2.2.10"
    id("com.gradle.plugin-publish") version "2.0.0"
}

group = "dev.kiki"
version = providers.gradleProperty("version").orElse("0.1.0-SNAPSHOT").get()

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
    withSourcesJar()
    withJavadocJar()
}

repositories {
    mavenCentral()
    google()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.2.10")
    implementation("org.openapitools:openapi-generator-gradle-plugin:7.14.0")
    implementation("org.openapitools:openapi-generator:7.14.0")
    // Used only when the consuming project applies an Android plugin.
    compileOnly("com.android.tools.build:gradle:8.8.2")

    testImplementation(kotlin("test"))
    testImplementation(gradleTestKit())
}

tasks.test {
    useJUnitPlatform()
}

gradlePlugin {
    website.set("https://github.com/peterQin18/openapi-ktor-gradle-plugin")
    vcsUrl.set("https://github.com/peterQin18/openapi-ktor-gradle-plugin.git")

    plugins {
        create("openApiKtor") {
            id = "dev.kiki.openapi-ktor"
            implementationClass = "dev.kiki.openapi.ktor.OpenApiKtorPlugin"
            displayName = "OpenAPI Ktor Generator"
            description = "Generates Kotlin serializable models and Ktor suspend APIs from OpenAPI specifications."
            tags.set(listOf("openapi", "kotlin", "ktor", "android", "codegen"))
        }
    }
}

publishing {
    publications.withType<MavenPublication>().configureEach {
        pom {
            name.set("OpenAPI Ktor Gradle Plugin")
            description.set("OpenAPI Generator wrapper for Kotlin models and Ktor suspend APIs.")
            url.set("https://github.com/peterQin18/openapi-ktor-gradle-plugin")
            licenses {
                license {
                    name.set("MIT License")
                    url.set("https://opensource.org/license/mit")
                }
            }
            developers {
                developer {
                    id.set("peterqin")
                    name.set("Peter Qin")
                }
            }
            scm {
                connection.set("scm:git:https://github.com/peterQin18/openapi-ktor-gradle-plugin.git")
                developerConnection.set("scm:git:ssh://git@github.com/peterQin18/openapi-ktor-gradle-plugin.git")
                url.set("https://github.com/peterQin18/openapi-ktor-gradle-plugin")
            }
        }
    }
}
