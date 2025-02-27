// SPDX-FileCopyrightText: 2025 Eric Joldasov
//
// SPDX-License-Identifier: 0BSD

import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm") version "2.1.20-RC"
    id("app.cash.sqldelight") version "2.0.2"
    id("com.gradleup.shadow") version "8.3.6"
    id("io.gitlab.arturbosch.detekt") version "1.23.8"
}

group = "net.landlesscity"
version = "0.1.0"

repositories {
    mavenCentral()
    maven {
        url = uri("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
        name = "spigotmc-repo"
    }
    maven {
        url = uri("https://oss.sonatype.org/content/groups/public/")
        name = "sonatype"
    }
}

dependencies {
    // Basic kit for making Spigot plugin in Kotlin:
    compileOnly(
        group = "org.spigotmc",
        name = "spigot-api",
        version = "1.12.2-R0.1-SNAPSHOT",
    )
    implementation(
        group = "org.jetbrains.kotlin",
        name = "kotlin-stdlib-jdk8",
    )

    implementation(
        group = "app.cash.sqldelight",
        name = "sqlite-driver",
        version = "2.0.2",
    )
    implementation(
        group = "com.password4j",
        name = "password4j",
        version = "1.8.2",
    )
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        jvmTarget = JvmTarget.JVM_1_8
        progressiveMode = true
    }
}
java {
    targetCompatibility = JavaVersion.VERSION_1_8
}

tasks {
    build {
        dependsOn(shadowJar)
    }

    check {
        this.dependsOn.removeAll({
            it is TaskProvider<*> && it.name == "detekt"
        })
    }

    detekt {
        basePath = rootProject.projectDir.absolutePath
    }

    processResources {
        val props = mapOf("version" to version)
        inputs.properties(props)
        filteringCharset = "UTF-8"
        filesMatching("plugin.yml") {
            expand(props)
        }
    }

    shadowJar {
        dependencies {
            // SQLDelight pulls sqlite-jdbc which has ~20 MB
            // of native libraries for each platform, which is
            // too much for plugin JAR.
            // Since they are already included by Spigot, just omit them.
            exclude(dependency("org.xerial:sqlite-jdbc:.*"))
        }
        isEnableRelocation = true
        relocationPrefix = "net.landlesscity.nukelogin.libs"
        minimize()
    }
}

sqldelight {
    databases {
        create("SQLite") {
            packageName = "net.landlesscity.nukelogin.sql"
            // SpigotMC 1.12.2 includes SQLite version 3.21
            dialect("app.cash.sqldelight:sqlite-3-18-dialect:2.0.2")
        }
    }
}