// Gradle build script (Kotlin style) for the "terminal" module
// The terminal opens stdio for unscripted communication with the application

plugins {
    id("bert.library-conventions")
}

repositories {
    flatDir {
        dirs("../lib")
    }
}

dependencies {
    implementation("org.apache.commons:commons-text")
    implementation(project(":common"))
    implementation(project(":speech"))
}