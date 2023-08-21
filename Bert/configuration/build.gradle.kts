// Copy build scripts into external bin
import org.apache.tools.ant.filters.ReplaceTokens
plugins {
    id("bert.kotlin-common-conventions")
}
// We would like this to run at the very end
// --- doesn't quite
dependencies {
    testImplementation(project(":bertApp"))
    testImplementation(project(":dispatcher"))
}

//Files included in the copy
val dataContent = copySpec {
    from("src/main")
    include("*")
}

// Execute this at the very end of configuration project
tasks.named("testClasses") { finalizedBy("install") }

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
