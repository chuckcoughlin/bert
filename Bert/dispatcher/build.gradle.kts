// Gradle build script (Kotlin style) for the "dispatcher" module
// The dispatcher is the entry point for the entire application.
plugins {
    kotlin("jvm")
}
repositories {
    mavenCentral()
}


dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.6.4")
    implementation("org.apache.commons:commons-text:1.10.0")
    implementation(project(":command"))
    implementation(project(":common"))
    implementation(project(":control"))
    implementation(project(":motor"))
    implementation(project(":database"))
    implementation(project(":terminal"))
}


