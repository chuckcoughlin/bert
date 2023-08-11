// Gradle build script (Kotlin style) for the "control" module

plugins {
    kotlin("jvm")
    application
}

dependencies {
    implementation(project(":common"))
    implementation(project(":terminal"))
    implementation("org.hipparchus:hipparchus-geometry:1.3")
    testImplementation("org.hipparchus:hipparchus-geometry:1.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.5.0")
    testImplementation("org.apache.commons:commons-text:1.9")
}

application {
    mainClass.set("chuckcoughlin.bert.control.solver.Solver")
}