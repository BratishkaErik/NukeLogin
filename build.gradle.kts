// SPDX-FileCopyrightText: 2025 Eric Joldasov
//
// SPDX-License-Identifier: 0BSD

import org.jetbrains.dokka.gradle.engine.parameters.VisibilityModifier
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.dokka)
    alias(libs.plugins.shadow)
    alias(libs.plugins.sqldelight)
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
    compileOnly(libs.spigot.api)
    implementation(libs.kotlin.stdlib)

    implementation(libs.password4j)
    implementation(libs.sqldelight.sqlite)
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

    processResources {
        val props = mapOf(
            "version" to project.version
        )

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
        enableAutoRelocation = true

        relocationPrefix = "net.landlesscity.nukelogin.libs"
        minimize()
    }
}

sqldelight {
    databases {
        create("SQLiteDatabase") {
            packageName = "main.sqlite"
            srcDirs = files("src/main/sqldelight/main/sqlite/")
            dialect(libs.sqldelight.sqlite.dialect)
        }
        create("UserLoginSQLiteDatabase") {
            packageName = "userlogin.sqlite"
            srcDirs = files("src/main/sqldelight/userlogin/sqlite/")
            dialect(libs.sqldelight.sqlite.dialect)
        }
    }
}

tasks.withType<app.cash.sqldelight.gradle.VerifyMigrationTask> {
    enabled = false
}


dokka {
    dokkaSourceSets.main {
        documentedVisibilities = setOf(
            VisibilityModifier.Public,
            VisibilityModifier.Internal,
            VisibilityModifier.Protected,
            VisibilityModifier.Private,
        )
    }
}

/* tasks.register("testMinecraftPlugin") {

    doLast {
        // Build plugin JAR first
        val pluginJar = tasks.named("jar").get().outputs.files.singleFile

        // Start PaperMC server container
        val mc = GenericContainer<Nothing>("itzg/minecraft-paper-server:1.20.4")
            .withEnv("EULA", "TRUE")
            .withEnv("TYPE", "PAPER")
            .withEnv("VERSION", "1.20.4")
            .withExposedPorts(25565)
            .apply {
                // Copy plugin JAR into /plugins
                withCopyFileToContainer(MountableFile.forHostPath(pluginJar.absolutePath), "/plugins/${pluginJar.name}")
            }

        mc.start()

        println("✅ Minecraft server started at localhost:${mc.getMappedPort(25565)}")
        println("📂 Your plugin '${pluginJar.name}' is loaded into /plugins inside the container.")

        // Wait for logs or test interaction
        Thread.sleep(15000) // allow boot time
        val logs = mc.logs
        println("--- Minecraft logs ---")
        println(logs)
    }
}
*/