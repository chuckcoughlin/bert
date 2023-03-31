// Gradle build script (Kotlin style) for the "dispatcher" module
// The dispatcher is the entry point for the entire application.

plugins {
    id("bert.application-conventions")
    application
}

repositories {
    mavenCentral()
    flatDir {
        dirs("../lib")
    }
}

dependencies {
    implementation("org.apache.commons:commons-text")
    implementation(project(":command"))
    implementation(project(":common"))
    implementation(project(":control"))
    implementation(project(":motor"))
    implementation(project(":database"))
    implementation(project(":terminal"))
}

application {
    mainClass.set("chuckcoughlin.bert.dispatch.controller.Dispatcher")
}


