// Copy build scripts into external bin
import org.apache.tools.ant.filters.ReplaceTokens

plugins {
    id("bert.kotlin-common-conventions")
}
// We would like this to run at the very end
// --- doesn't quite
dependencies {
    implementation(project(":bertApp"))
}

//Files included in the copy
val dataContent = copySpec {
    from("src/main")
    include("*")
}

// Execute this at the very end of configuration project
tasks.named("build") { finalizedBy("install") }

tasks {
    register("install", Copy::class) {
        println("Configuration: Install registered for " + System.getenv("BERT_HOME"))
        outputs.upToDateWhen { false }  // Always run task
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
            exec {
                commandLine("./src/main/bin/unpack_distribution.sh")
            }
            println("Configuration: Install complete")
        }
    }
}
