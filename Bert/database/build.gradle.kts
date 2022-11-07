// Kotlin Gradle build file for the SQLite database module

plugins {
    id("bert.library-conventions")
}

repositories {
    mavenCentral()
    flatDir {
        dirs("../lib")
    }
}

dependencies {
    api(project(":common"))
    implementation("org.sqlite:sqlite-jdbc:3.23.1")
}