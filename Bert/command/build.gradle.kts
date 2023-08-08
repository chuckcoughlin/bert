plugins {
    id("bert.library-conventions")
}

repositories {
    flatDir {
        dirs("../lib")
    }
}

dependencies {
    implementation(project(":common"))
    implementation(project(":database"))
    implementation(project(":speech"))
}
