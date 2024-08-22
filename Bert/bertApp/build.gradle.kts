/*
 * Bert is the main entry point, but this project contains
 * several other main classes for test applications
 */
plugins {
    id("bert.kotlin-application-conventions")
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.6.4")
    implementation("io.github.java-native:jssc:2.9.4")
    implementation("org.slf4j:slf4j-jdk14:1.7.29")
    implementation("org.apache.commons:commons-text:1.10.0")
    implementation(project(":common"))
    implementation(project(":command"))
    implementation(project(":database"))
    implementation(project(":motor"))
    implementation(project(":speech"))
    implementation(project(":terminal"))
    implementation(project(":dispatcher"))
}

// Execute this at the very end of configuration project
tasks.named("jar") { finalizedBy("distTar") }

// Define the main class for the application.
application {
    mainClass.set("chuckcoughlin.bert.BertKt")
}

tasks {
    getByName<Delete>("clean") {
        delete.add("build") // add accepts argument with Any type
        delete.add("../libs") // add accepts argument with Any type
    }
}