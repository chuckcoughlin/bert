// Gradle build script (Kotlin style) for the "common" module
plugins {
    id("bert.kotlin-common-conventions")
}
repositories {
    google()
    mavenCentral()
}
dependencies {
    implementation("org.apache.commons:commons-text:1.10.0")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.12.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.6.4")
    implementation ("com.google.code.gson:gson:2.8.9")
    implementation("org.hipparchus:hipparchus-geometry:2.2")
}