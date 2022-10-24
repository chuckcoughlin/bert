plugins {
    antlr
}

repositories {
    mavenCentral()
}


dependencies {
    antlr("org.antlr:antlr4:4.7.1")
    implementation("org.antlr:antlr-runtime:4.7.1")
}

// Run ANTLR lexer/parser on the .g4 source file, generating Java
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
}



