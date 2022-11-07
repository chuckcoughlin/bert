// Gradle build script (Kotlin style) for the "motor" module

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
    api(project(":database"))
}