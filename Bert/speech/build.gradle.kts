// Gradle build script (Kotlin style) for the "speech" module
plugins {
    id("bert.kotlin-common-conventions")
}

repositories {
    google()
    mavenCentral()
}

dependencies {
    implementation(project(":common"))
    implementation(project(":database"))
    implementation(project(":syntax"))
    implementation(files("../libs/antlr-runtime-4.7.2.jar"))
    implementation("com.google.code.gson:gson:2.8.5")
}