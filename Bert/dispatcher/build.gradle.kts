// Gradle build script (Kotlin style) for the "dispatcher" module
// The dispatcher is the entry point for the entire application.

plugins {
    application
}


dependencies {
    "org.apache.commons:commons-text"
    project(":command")
    project(":common")
    project(":control")
    project(":motor")
    project(":database")
    project(":terminal")
}

application {
    mainClass.set("chuckcoughlin.bert.dispatch.controller.Dispatcher")
}


