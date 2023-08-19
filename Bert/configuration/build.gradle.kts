// Copy build scripts into external bin
import org.apache.tools.ant.filters.ReplaceTokens
plugins {
    id("bert.kotlin-common-conventions")
}
dependencies {
    implementation(project(":app"))
}

//for including in the copy task
val dataContent = copySpec {
    from("src/main")
    include("*")
}
tasks.named("classes") { finalizedBy("install") }

tasks {
    register("install", Copy::class) {
        println("Configuration: Install registered")
        println(System.getenv("BERT_HOME"))
        val tokens = mapOf("version" to "2.3.1")
        inputs.properties(tokens)

        from("src/main/") {
            include("**/*.csv")
            include("**/*.py")
            include("bin/*")
            include("**/*.sql")
            include("**/*.xml")
            filter<ReplaceTokens>("tokens" to tokens)
        }

        into(System.getenv("BERT_HOME"))
        exclude("**/*.bak")
        includeEmptyDirs = true
        with(dataContent)

        doLast {
            println("Configuration: Install complete")
        }
    }
}
