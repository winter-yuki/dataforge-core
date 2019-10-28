plugins {
    id("scientifik.jvm")
}

description = "YAML meta IO"

dependencies {
    api(project(":dataforge-io"))
    api("org.yaml:snakeyaml:1.25")
    testImplementation(kotlin("test"))
    testImplementation(kotlin("test-junit"))
}