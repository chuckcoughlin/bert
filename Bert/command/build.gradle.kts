// Command Module
plugins {
    kotlin("jvm")
}
dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.5.0")
    implementation(project(":common"))
    implementation(project(":database"))
    implementation(project(":speech"))
}
