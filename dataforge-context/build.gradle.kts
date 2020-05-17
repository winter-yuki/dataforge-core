import scientifik.useCoroutines

plugins {
    id("scientifik.mpp")
}

description = "Context and provider definitions"


useCoroutines()

kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                api(project(":dataforge-meta"))
                api("io.github.microutils:kotlin-logging-common:1.7.9")
            }
        }
        val jvmMain by getting {
            dependencies {
                api(kotlin("reflect"))
                api("io.github.microutils:kotlin-logging:1.7.9")
                api("ch.qos.logback:logback-classic:1.2.3")
            }
        }
        val jsMain by getting {
            dependencies {
                api("io.github.microutils:kotlin-logging-js:1.7.9")
            }
        }
    }
}