plugins {
    antlr
    java
    `java-library`
}

repositories {
    mavenCentral()
    flatDir {
        dirs("../lib")
    }
}

dependencies {
    antlr("org.antlr:antlr4:4.7.1")
    implementation(files("syntax.jar"))
    //testImplementation("org.antlr:antlr-runtime-4.7.2")
}

// Run ANTLR lexer/parser on the .g4 source file, generating Java.
// Result is build/generated-src, then compiled into build/classes
tasks.generateGrammarSource {
    maxHeapSize = "64m"
    arguments = arguments + listOf("-visitor", "-long-messages", "-package", "chuckcoughlin.bert.syntax")
    // Keep a copy of generated sources
    doLast {
        println("Copying generated grammar lexer/parser files to main directory.")
        copy {
            from("${buildDir}/generated-src/antlr/main")
            into("generated-src/main/java")
        }
    }
    finalizedBy(listOf("jar"))
}

// Generates libs/syntax.jar after executing run-configuration bert:syntax[jar]
tasks.jar {
    doLast {
        println("syntax:jar task ....")
        from(listOf("${buildDir}/classes/java/main"))
        // Put the jar file in a public place
        copy {
            from("${buildDir}/libs/syntax.jar")
            into("${buildDir}/../lib/syntax.jar")
        }
        println("syntax:jar task complete.")
    }
}

