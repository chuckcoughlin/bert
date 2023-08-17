// Gradle build script (Kotlin style) for the "motor" module
plugins {
    id("bert.kotlin-common-conventions")
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.6.4")
    implementation("io.github.java-native:jssc:2.9.4")
    implementation(project(":common"))
    implementation(project(":database"))
}