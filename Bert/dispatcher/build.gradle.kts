// Gradle build script (Kotlin style) for the "dispatcher" module
// The dispatcher is the entry point for the entire application.
plugins {
    id("bert.kotlin-common-conventions")
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.6.4")
    implementation("org.apache.commons:commons-text:1.10.0")
    implementation(project(":command"))
    implementation(project(":common"))
    implementation(project(":ai"))
    implementation(project(":motor"))
    implementation(project(":database"))
    implementation(project(":terminal"))
}