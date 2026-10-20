pluginManagement {
    val quarkusVersion: String by settings
    val kotlinVersion: String by settings
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
    plugins {
        id("io.quarkus") version quarkusVersion
        id("io.quarkus.extension") version quarkusVersion
        id("org.jetbrains.kotlin.jvm") version kotlinVersion
        id("org.jetbrains.kotlin.plugin.allopen") version kotlinVersion
        id("com.google.protobuf") version "0.9.4"
    }
}

plugins {
    // Provisions the JDK the toolchains ask for, so a checkout builds on whatever JDK is installed.
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "quarkus-connect-parent"

include("runtime")
include("deployment")
include("integration-tests")

// Published artifact ids (extension plugin uses project.name for coordinates)
project(":runtime").name = "quarkus-connect"
project(":deployment").name = "quarkus-connect-deployment"
