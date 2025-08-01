import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.9.21"
    kotlin("plugin.serialization") version "1.9.21"
    application
}

group = "com.jules"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    // Kord for Discord API interaction
    implementation("dev.kord:kord-core:0.13.0")

    // Kotlinx Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")

    // Kotlinx Serialization for JSON parsing
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    // Ktor client for HTTP requests
    implementation("io.ktor:ktor-client-core:2.3.9")
    implementation("io.ktor:ktor-client-cio:2.3.9") // CIO engine for Ktor

    // Logging framework for better diagnostics
    implementation("ch.qos.logback:logback-classic:1.4.14")
}

application {
    mainClass.set("BotKt")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
}

tasks.jar {
    manifest {
        attributes(
            "Main-Class" to "BotKt"
        )
    }
    configurations["runtimeClasspath"].forEach { file ->
        from(zipTree(file.absoluteFile))
    }
}