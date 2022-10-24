plugins {
    id("bert.library-conventions")
}
/*
compile.Java.dependsOn("antlr4")
sourceSets.main.java.sourceDirs += antlr4.output
// Add antlr4 to the classpath
configurations {
    compile.extendsFrom antlr4
}
*/
dependencies {
    api(project(":common"))
    api(project(":syntax"))
}