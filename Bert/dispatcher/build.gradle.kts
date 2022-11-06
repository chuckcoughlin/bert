
plugins {
    id("bert.application-conventions")
    application
}

dependencies {
    implementation("org.apache.commons:commons-text")
    implementation(project(":common"))
    implementation(project(":control"))
    implementation(project(":motor"))
    implementation(project(":database"))
}

application {
    mainClass.set("chuckcoughlin.bert.dispatch.controller.Dispatcher")
}


