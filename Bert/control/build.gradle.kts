// Gradle build script (Kotlin style) for the "control" module

plugins {
    kotlin("jvm")
    application
}

dependencies {
    implementation(project(":common"))
    implementation(project(":terminal"))
    implementation("org.hipparchus:hipparchus-geometry:2.2")
    testImplementation("org.hipparchus:hipparchus-geometry:2.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.6.4")
    testImplementation("org.apache.commons:commons-text:1.10.0")
}

application {
    mainClass.set("chuckcoughlin.bert.control.solver.Solver")

}

tasks.getByName("assemble").dependsOn("testJar")
tasks.register<Jar>("testJar") {
    archiveFileName.set("eulenspiegel-testHelpers-$version.jar")
    include("com/eulenspiegel/helpers/*")
    from(project.the<SourceSetContainer>()["test"].output)
}


