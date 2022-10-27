plugins {
    id("bert.application-conventions")
    application
}

dependencies {
    implementation("org.apache.commons:commons-text")
    implementation(project(":common"))
    implementation(project(":speech"))
}

application {
    mainClass.set("chuckcoughlin.bert.term.controller.Terminal")
}