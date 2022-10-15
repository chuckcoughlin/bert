plugins {
    id("bert.application-conventions")
    application
}

dependencies {
    implementation("org.apache.commons:commons-text")
    implementation(project(":common"))
}

application {
    mainClass.set("chuckcoughlin.bert.control.solver.Solver")
}