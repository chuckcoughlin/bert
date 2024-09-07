// Gradle build script (Kotlin style) for the "motor" module
plugins {
    id("bert.kotlin-common-conventions")
}

repositories {
    google()
    mavenCentral()
}

// It appears as if jssc requires log4j
dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.6.4")
    implementation("io.github.java-native:jssc:2.9.4")
    implementation("org.slf4j:slf4j-jdk14:1.7.29")
    implementation("com.google.code.gson:gson:2.8.5")
    implementation(project(":common"))
    implementation(project(":database"))
}