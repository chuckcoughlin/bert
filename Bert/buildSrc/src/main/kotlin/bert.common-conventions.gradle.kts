// Kotlin Gradle build file for library modules
import org.gradle.internal.impldep.junit.runner.Version.id

plugins {
    id("org.jetbrains.kotlin.jvm") 
}

repositories {
    mavenCentral() 
}

dependencies {
    constraints {
        implementation("org.apache.commons:commons-text:1.9")
        implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    }

    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.0")
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.12.1")
    implementation("io.github.java-native:jssc:2.9.4")
}