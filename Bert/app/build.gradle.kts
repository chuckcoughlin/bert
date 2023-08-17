/*
 * Dispatcher is the main entry point, but this project contains
 * several other main classes for test applications
 */

plugins {
    id("bert.kotlin-application-conventions")
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.6.4")
    implementation("io.github.java-native:jssc:2.9.4")
    implementation("org.apache.commons:commons-text:1.10.0")
    implementation(project(":common"))
    implementation(project(":control"))
    implementation(project(":database"))
    implementation(project(":motor"))
    implementation(project(":dispatcher"))
    implementation(project(":configuration"))
}

// Define the main class for the application.
application {
    mainClass.set("chuckcoughlin.bert.Bert.kt")
}
