pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

rootProject.name = "figlekt"

include(":figlekt-core")
include(":figlekt-generator")
include(":figlekt-plugin")
include(":figlekt-cli")
include(":figlekt-webapp")