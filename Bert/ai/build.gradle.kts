// AI Module
plugins {
    id("bert.kotlin-common-conventions")
}
dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.6.4")
    implementation ("com.google.code.gson:gson:2.8.9")
    implementation("io.ktor:ktor-client-core:2.2.4")
    implementation("io.ktor:ktor-client-cio:2.2.4")
    implementation(project(":common"))
}