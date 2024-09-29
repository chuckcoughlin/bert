// Kotlin Gradle build file for the SQLite database module
plugins {
    id("bert.kotlin-common-conventions")
}

dependencies {
    implementation(project(":common"))
    implementation("org.xerial:sqlite-jdbc:3.40.1.0")
    implementation ("com.google.code.gson:gson:2.8.9")
}