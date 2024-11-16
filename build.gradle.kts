plugins {
    kotlin("jvm") version "2.1.0-Beta2"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    `maven-publish`
    id("xyz.jpenilla.run-paper") version "2.3.1" // Paper server for testing/hotloading JVM
}

group = "net.mvndicraft"
version = "1.0.0-SNAPSHOT"
description = "Misc stuff for mvndi"
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
    compileOnly("net.mvndicraft:mvndicore:2.0.0-SNAPSHOT")
    compileOnly("net.mvndicraft:mvndiequipment:2.0.0-SNAPSHOT")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    testImplementation("org.junit.jupiter:junit-jupiter:5.11.3")
    testImplementation("com.github.seeseemelk:MockBukkit-v1.21:3.107.0")
}

val targetJavaVersion = 21
kotlin {
    jvmToolchain(targetJavaVersion)
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
