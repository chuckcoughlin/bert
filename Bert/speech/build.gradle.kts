// Gradle build script (Kotlin style) for the "speech" module
plugins {
    kotlin("jvm")
}

// Make special reference to the compiled ANTLR classes
repositories {
    mavenCentral()
    flatDir {
        dirs("../lib")
    }
}

dependencies {
    implementation(project(":common"))
    implementation(project(":database"))
    implementation(project(":syntax"))
    implementation(files("syntax.jar"))
}