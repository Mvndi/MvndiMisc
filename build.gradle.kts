import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.DetektCreateBaselineTask

plugins {
    kotlin("jvm") version "2.1.0-Beta2"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    `maven-publish`
    id("io.gitlab.arturbosch.detekt") version "1.23.7" // Static code analysis
    id("xyz.jpenilla.run-paper") version "2.3.1" // Paper server for testing/hotloading JVM
}

group = "net.mvndicraft"
version = "1.0.0-SNAPSHOT"
description = "A plugin template in kotlin."
java.sourceCompatibility = JavaVersion.VERSION_21

val mvndiRemote = repositories.maven("https://repo.mvndicraft.net/repository/maven-snapshots/") {
    name = "Mvndi"
    credentials {
        username = project.findProperty("mvndi.user") as String? ?: System.getenv("MVNDI_MVN_USER")
        password = project.findProperty("mvndi.key") as String? ?: System.getenv("MVNDI_MVN_KEY")
    }
}

repositories {
    mavenCentral()

    // Paper
    maven("https://repo.papermc.io/repository/maven-public/")

    // Kotlin
    maven("https://oss.sonatype.org/content/groups/public/")

    // Mvndi
    mvndiRemote
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.1-R0.1-SNAPSHOT")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    testImplementation("org.junit.jupiter:junit-jupiter:5.11.3")
    testImplementation("com.github.seeseemelk:MockBukkit-v1.21:3.107.0")
}

val targetJavaVersion = 21
kotlin {
    jvmToolchain(targetJavaVersion)
}

detekt {
    buildUponDefaultConfig = true // preconfigure defaults
    allRules = false // activate all available (even unstable) rules.
    config.setFrom("$projectDir/config/detekt.yml") // point to your custom config defining rules to run, overwriting default behavior
    baseline = file("$projectDir/config/baseline.xml") // a way of suppressing issues before introducing detekt
}

tasks.withType<Detekt>().configureEach {
    reports {
        html.required.set(true) // observe findings in your browser with structure and code snippets
        xml.required.set(true) // checkstyle like format mainly for integrations like Jenkins
        sarif.required.set(true) // standardized SARIF format (https://sarifweb.azurewebsites.net/) to support integrations with GitHub Code Scanning
        md.required.set(true) // simple Markdown format
    }
}

tasks.withType<Detekt>().configureEach {
    jvmTarget = targetJavaVersion.toString()
}
tasks.withType<DetektCreateBaselineTask>().configureEach {
    jvmTarget = targetJavaVersion.toString()
}

tasks {
    build {
        dependsOn("shadowJar")
    }

    processResources {
        val props = mapOf(
            "name" to project.name,
            "version" to project.version,
            "description" to project.description,
        )
        inputs.properties(props)
        filteringCharset = "UTF-8"
        filesMatching("paper-plugin.yml") {
            expand(props)
        }
    }

    runServer {
        minecraftVersion("1.21.1")
    }

    test {
        useJUnitPlatform()
    }
}

publishing {
    repositories {
        mvndiRemote
    }
    publications.create<MavenPublication>("maven") {
        from(components["java"])
        artifactId = "templatepluginkt"
    }
}
