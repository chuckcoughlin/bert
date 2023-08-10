// Kotlin Gradle build file for the SQLite database module

repositories {
    mavenCentral()
    flatDir {
        dirs("../lib")
    }
}

dependencies {
    project(":common")
    "org.sqlite:sqlite-jdbc:3.23.1"
}