@file:OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)

plugins {
    kotlin("multiplatform")
}


kotlin {
    wasmJs {
        browser {
            commonWebpackConfig {
                outputFileName = "figlekt.js"
            }
        }
        binaries.executable()
    }

    sourceSets {
        named("wasmJsMain") {
            dependencies {
                implementation(project(":figlekt-generator"))
                implementation("org.jetbrains.kotlinx:kotlinx-browser:0.5.0")
            }
        }
    }
}