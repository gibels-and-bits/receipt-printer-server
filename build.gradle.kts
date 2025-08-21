import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.9.20"
    kotlin("plugin.serialization") version "1.9.20"
    application
}

group = "com.example.receipt.server"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    // Ktor server
    implementation("io.ktor:ktor-server-core:2.3.5")
    implementation("io.ktor:ktor-server-netty:2.3.5")
    implementation("io.ktor:ktor-server-content-negotiation:2.3.5")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.5")
    implementation("io.ktor:ktor-server-cors:2.3.5")
    implementation("io.ktor:ktor-server-status-pages:2.3.5")
    implementation("io.ktor:ktor-server-host-common:2.3.5")
    
    // Kotlin scripting for interpreter execution
    implementation("org.jetbrains.kotlin:kotlin-scripting-jsr223:1.9.20")
    implementation("org.jetbrains.kotlin:kotlin-script-runtime:1.9.20")
    implementation("org.jetbrains.kotlin:kotlin-compiler-embeddable:1.9.20")
    implementation("org.jetbrains.kotlin:kotlin-scripting-compiler-embeddable:1.9.20")
    
    // Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    
    // Logging
    implementation("ch.qos.logback:logback-classic:1.4.11")
    implementation("io.github.microutils:kotlin-logging-jvm:3.0.5")
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    
    // Testing
    testImplementation(kotlin("test"))
    testImplementation("io.ktor:ktor-server-tests:2.3.5")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:1.9.20")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs = listOf("-Xjsr305=strict")
    }
}

tasks.test {
    useJUnitPlatform()
}

application {
    mainClass.set("com.example.receipt.server.ApplicationKt")
}

// Task to create a fat JAR
tasks.register<Jar>("fatJar") {
    dependsOn.addAll(listOf("compileJava", "compileKotlin", "processResources"))
    archiveClassifier.set("standalone")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    manifest {
        attributes["Main-Class"] = "com.example.receipt.server.ApplicationKt"
    }
    val sourcesMain = sourceSets.main.get()
    val contents = configurations.runtimeClasspath.get()
        .map { if (it.isDirectory) it else zipTree(it) } +
            sourcesMain.output
    from(contents)
}

// Task to run the server
tasks.register<JavaExec>("runServer") {
    dependsOn("classes")
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("com.example.receipt.server.ApplicationKt")
    
    // JVM arguments for better performance
    jvmArgs = listOf(
        "-Xms256m",
        "-Xmx1024m",
        "-XX:+UseG1GC"
    )
}