// Kotlin Gradle build file for the SQLite database module
plugins {
    kotlin("jvm")
}

repositories {
    mavenCentral()
    flatDir {
        dirs("../lib")
    }
}

dependencies {
    implementation(project(":common"))
    implementation("org.xerial:sqlite-jdbc:3.40.1.0")
}