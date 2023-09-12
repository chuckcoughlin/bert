plugins {
    base
    antlr
    id("java-library")
}
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(18))
    }
}

repositories {
    mavenCentral()
}

// Ran into an inconsistency in runtime when using ANTLR 4.11.1
// The error manifested itself at runtime
dependencies {
    antlr("org.antlr:antlr4:4.7.2")
}

// Run ANTLR lexer/parser on the .g4 source file, generating Java.
// Result is build/generated-src, then compiled into build/classes
tasks.generateGrammarSource {
    maxHeapSize = "64m"
    arguments = arguments + listOf("-visitor", "-long-messages", "-package", "chuckcoughlin.bert.syntax")
}


tasks {
    getByName<Delete>("clean") {
        delete.add("generated-src") // add accepts argument with Any type
    }
}



