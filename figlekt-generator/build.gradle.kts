plugins {
    kotlin("multiplatform")
}


kotlin {
    jvm()

    wasmJs {
        nodejs()
    }

    sourceSets {
        commonTest {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}
