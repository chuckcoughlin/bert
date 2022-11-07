// Gradle build script (Kotlin style) for the "control" module

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
    implementation("org.hipparchus:hipparchus-core-1.5")
    implementation(files("hipparchus-core-1.5.jar"))
    testImplementation(files("hipparchus-core-1.5.jar"))
}

application {
    mainClass.set("chuckcoughlin.bert.control.solver.Solver")
}