plugins {
    kotlin("jvm")
    `java-gradle-plugin`
    `maven-publish`
}


dependencies {
    implementation(project(":figlekt-generator"))
    implementation(gradleApi())
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.20")
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

gradlePlugin {
    plugins {
        create("figlektFonts") {
            id = "fr.godox.figlekt.fonts"
            displayName = "Figlekt fonts"
            description = "Generates FigletFont classes and Font extensions from the .flf files of each source set."
            implementationClass = "fr.godox.figlekt.fonts.FiglektFontsPlugin"
        }
    }
}