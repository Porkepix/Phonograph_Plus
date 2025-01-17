plugins {
    val AGPVersion = "7.4.2"
    id("com.android.application") version AGPVersion apply false
    // id("com.android.library") version AGPVersion apply false
    val kotlinVersion = "1.8.10"
    id("org.jetbrains.kotlin.android") version kotlinVersion apply false
    id("org.jetbrains.kotlin.plugin.serialization") version kotlinVersion apply false
    id("org.jetbrains.kotlin.plugin.parcelize") version kotlinVersion apply false
    // enable if needed
    // id("com.github.ben-manes.versions") version "0.44.0"
}
repositories {
    gradlePluginPortal()
    google()
    mavenCentral()
}