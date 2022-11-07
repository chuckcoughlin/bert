// Gradle build script (Kotlin style) for the "dispatcher" module

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
    implementation(project(":common"))
    implementation(project(":control"))
    implementation(project(":motor"))
    implementation(project(":database"))
}

application {
    mainClass.set("chuckcoughlin.bert.dispatch.controller.Dispatcher")
}


