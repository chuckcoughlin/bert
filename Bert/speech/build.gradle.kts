plugins {
    id("bert.library-conventions")
}

// Make special reference to the compiled ANTLR classes
repositories {
    flatDir {
        dirs("../syntax/buiild/libs")
    }
}

dependencies {
    api(project(":common"))
    api(project(":syntax"))
    implementation(files("syntax.jar"))
}