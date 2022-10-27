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
    implementation(files("antlr-runtime-4.7.2.jar"))
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
}
/*
tasks {
    println("syntax.jar ....")
    val syntaxJar = register<Jar>("syntaxJar") {
        dependsOn.addAll(listOf("compileJava","compileKotlin","classes"))
        archiveFileName.set("bert-syntax")
        val sourcesMain = sourceSets.main
        val contents = configurations.runtimeClasspath.get()
            .map{ if(it.isDirectory) it else zipTree(it) }
        from(listOf("${buildDir}/classes/java/main"))
    }


    build {
        dependsOn(syntaxJar)   // Trigger jar creation during build
    }
}
tasks.withType<Jar> (){
    println("tasks with type ....")
    archiveFileName.set("bert-syntax")
    val sourcesMain = sourceSets.main
    val contents = configurations.runtimeClasspath.get()
        .map{ if(it.isDirectory) it else zipTree(it) }
    from(listOf("${buildDir}/classes/java/main"))
}
*/
tasks.jar {
    println("jar task ....")
    archiveFileName.set("bert-syntax")
    from(listOf("${buildDir}/classes/java/main"))
}

