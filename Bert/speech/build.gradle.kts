plugins {
    id("bert.library-conventions")
}

// Make special reference to the compiled ANTLR classes
repositories {
    mavenCentral()
    flatDir {
        dirs("../lib")
    }
}

dependencies {
    api(project(":common"))
    api(project(":database"))
    api(project(":syntax"))
    implementation(files("syntax.jar"))
}