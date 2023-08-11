// Gradle build script (Kotlin style) for the "terminal" module
// The terminal opens stdio for unscripted communication with the application
plugins {
    kotlin("jvm")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.apache.commons:commons-text:1.9")
    testImplementation("org.apache.commons:commons-text:1.9")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.5.0")
    implementation(project(":common"))
    implementation(project(":database"))
    implementation(project(":speech"))
}