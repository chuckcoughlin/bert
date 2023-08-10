// Gradle build script (Kotlin style) for the "common" module
plugins {
    kotlin("jvm")
}
dependencies {
    implementation("org.apache.commons:commons-text:1.9")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.12.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.5.0")
}