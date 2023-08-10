// Gradle build script (Kotlin style) for the "speech" module

// Make special reference to the compiled ANTLR classes
repositories {
    mavenCentral()
    flatDir {
        dirs("../lib")
    }
}

dependencies {
    project(":common")
    project(":database")
    project(":syntax")
    files("syntax.jar")
}