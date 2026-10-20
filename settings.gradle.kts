pluginManagement {
    val quarkusVersion = providers.gradleProperty("quarkusVersion").get()
    val kotlinVersion = providers.gradleProperty("kotlinVersion").get()
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
    plugins {
        id("io.quarkus") version quarkusVersion
        id("io.quarkus.extension") version quarkusVersion
        id("org.jetbrains.kotlin.jvm") version kotlinVersion
        id("org.jetbrains.kotlin.plugin.allopen") version kotlinVersion
        // 0.10.x required for Gradle 9 (detachedConfiguration extension error on 0.9.x)
        id("com.google.protobuf") version "0.10.0"
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
include("integration-tests-security")
include("examples:quarkus-kotlin-server")
include("conformance")

// Published artifact ids (extension plugin uses project.name for coordinates)
project(":runtime").name = "quarkus-connect"
project(":deployment").name = "quarkus-connect-deployment"
