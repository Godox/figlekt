import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("multiplatform")
    `maven-publish`
}


kotlin {
    jvm {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    js {
        nodejs()
    }

    wasmJs {
        nodejs()
    }

    iosX64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonTest {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        jvmTest {
            dependencies {
                implementation("org.junit.jupiter:junit-jupiter-api:5.10.1")
                runtimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.1")
            }
        }
    }
}

publishing {
    publications.withType<MavenPublication>().configureEach {
        pom {
            name.set("figlekt-core")
            description.set("A FIGlet utility for Kotlin Multiplatform, with embedded fonts compiled into common code.")
        }
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

val macHost = System.getProperty("os.name").orEmpty().startsWith("Mac", ignoreCase = true)
if (!macHost) {
    tasks.matching { task ->
        task.name.contains("ios", ignoreCase = true) && task.name.contains("test", ignoreCase = true)
    }.configureEach {
        enabled = false
    }
}