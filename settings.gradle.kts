// SPDX-FileCopyrightText: 2025 Eric Joldasov
//
// SPDX-License-Identifier: 0BSD

rootProject.name = "NukeLogin"

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("build.gradle.toml"))
        }
    }
}