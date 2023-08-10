// Gradle build script (Kotlin style) for the "terminal" module
// The terminal opens stdio for unscripted communication with the application


repositories {
    flatDir {
        dirs("../lib")
    }
}

dependencies {
    "org.apache.commons:commons-text"
    project(":common")
    project(":speech")
}