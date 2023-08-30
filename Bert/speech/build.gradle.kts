// Gradle build script (Kotlin style) for the "speech" module
plugins {
    id("bert.kotlin-common-conventions")
}


// Make special reference to the compiled ANTLR classes
repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":common"))
    implementation(project(":database"))
    implementation(project(":syntax"))
    implementation(files("../libs/antlr-runtime-4.7.2.jar"))
    implementation(files("syntax.jar"))
}