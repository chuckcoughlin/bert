// The top-level gradle config file contains common project-wide configuration

// Allows gradle configuration with Kotlin
plugins {
    `kotlin-dsl`
    kotlin("jvm") version "1.3.70" apply false
    application
}

// Define the repositories globally
allprojects {
    group = "chuckcoughlin.bert"
    version = "2.0"

    repositories {
        mavenCentral()
        flatDir {
            dirs("libs")
        }
    }
}


application {
    mainClass.set( "chuckcoughlin.bert.dispatch.controller.Dispatcher")
}

/*
dependencies {
    constraints {
        implementation("org.apache.commons:commons-text:1.9")
        implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    }
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.12.1")
    implementation("io.github.java-native:jssc:2.9.4")
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
}
 */




