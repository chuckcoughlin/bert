// Gradle build script (Kotlin style) for the "control" module

plugins {
    id("bert.kotlin-common-conventions")
}

dependencies {
    implementation(project(":common"))
    implementation(project(":terminal"))
    implementation("org.hipparchus:hipparchus-geometry:2.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.6.4")
}



