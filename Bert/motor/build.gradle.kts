// Gradle build script (Kotlin style) for the "motor" module

repositories {
    mavenCentral()
    flatDir {
        dirs("../lib")
    }
}

dependencies {
    project(":common")
    project(":database")
}