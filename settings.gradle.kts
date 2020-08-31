pluginManagement {
    repositories {
        mavenLocal()
        jcenter()
        gradlePluginPortal()
        maven("https://dl.bintray.com/kotlin/kotlin-eap")
        maven("https://dl.bintray.com/kotlin/kotlinx")
        maven("https://dl.bintray.com/mipt-npm/kscience")
        maven("https://dl.bintray.com/mipt-npm/dev")
    }

    val toolsVersion = "0.6.0"
    val kotlinVersion = "1.4.0"

    plugins {
        kotlin("jvm") version kotlinVersion
        id("scientifik.mpp") version toolsVersion
        id("scientifik.jvm") version toolsVersion
        id("scientifik.js") version toolsVersion
        id("scientifik.publish") version toolsVersion
    }

    resolutionStrategy {
        eachPlugin {
            when (requested.id.id) {
                "kscience.mpp", "kscience.jvm", "kscience.js", "kscience.publish" -> useModule("ru.mipt.npm:gradle-tools:${toolsVersion}")
            }
        }
    }
}

include(
    ":dataforge-meta",
    ":dataforge-io",
    ":dataforge-io:dataforge-io-yaml",
    ":dataforge-context",
    ":dataforge-data",
    ":dataforge-output",
    ":dataforge-output:dataforge-output-html",
    ":dataforge-tables",
    ":dataforge-workspace",
    ":dataforge-scripting"
)