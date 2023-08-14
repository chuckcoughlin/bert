// The top-level gradle config file contains common project-wide configuration

// Allows gradle configuration with Kotlin
plugins {
    `kotlin-dsl`
    kotlin("jvm") version "1.3.70" apply false
    application
}

// Define the repositories globally
allprojects {
    group = "chuckcoughlin.bert"
    version = "2.0"

    repositories {
        mavenCentral()
    }
}


application {
    mainClass.set( "chuckcoughlin.bert.dispatch.controller.Dispatcher")
}
tasks.jar {
    doLast {
        println("bert:jar task ....")
    }
}







