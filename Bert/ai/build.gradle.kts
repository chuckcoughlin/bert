import org.codehaus.groovy.tools.shell.util.Logger.io

// AI Module
plugins {
    id("bert.kotlin-common-conventions")
    kotlin("jvm")
    kotlin("plugin.serialization").version("1.6.21")
}
repositories {
    mavenCentral()
    maven { url = uri("https://maven.pkg.jetbrains.space/public/p/ktor/eap") }
}
dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.6.4")
    implementation ("com.google.code.gson:gson:2.8.9")
    implementation("io.ktor:ktor-client-core:2.2.4")
    implementation("io.ktor:ktor-client-cio:2.2.4")
    implementation("io.ktor:ktor-client-content-negotiation:2.2.4")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.2.4")
    implementation(project(":common"))
}