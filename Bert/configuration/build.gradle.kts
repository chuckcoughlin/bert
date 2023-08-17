// Copy build scripts into external bin
import org.apache.tools.ant.filters.ReplaceTokens
plugins {
    id("bert.kotlin-common-conventions")
}
//for including in the copy task
val dataContent = copySpec {
    from("src/data")
    include("*.data")
}

tasks {
    register("initConfig", Copy::class) {

        val tokens = mapOf("version" to "2.3.1")
        inputs.properties(tokens)

        from("src/main/") {
            include("**/*.properties")
            include("**/*.xml")
            filter<ReplaceTokens>("tokens" to tokens)
        }

        from("src/main/languages") {
            rename("EN_US_(.*)", "$1")
        }

        into("build/target/config")
        exclude("**/*.bak")
        includeEmptyDirs = false
        with(dataContent)
    }
}