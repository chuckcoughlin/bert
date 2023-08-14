// Command Module
plugins {
    kotlin("jvm")
}
dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.6.4")
    implementation(project(":common"))
    implementation(project(":database"))
    implementation(project(":speech"))
}
