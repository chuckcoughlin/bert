plugins {
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

dependencies {
    antlr("org.antlr:antlr4:4.11.1")
}

// Run ANTLR lexer/parser on the .g4 source file, generating Java.
// Result is build/generated-src, then compiled into build/classes
tasks.generateGrammarSource {
    maxHeapSize = "64m"
    arguments = arguments + listOf("-visitor", "-long-messages", "-package", "chuckcoughlin.bert.syntax")
}



