plugins {
    kotlin("jvm")
    application
}


dependencies {
    implementation(project(":figlekt-generator"))
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

application {
    mainClass.set("fr.godox.figlekt.cli.MainKt")
}