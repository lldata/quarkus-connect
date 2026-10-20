plugins {
    base
    id("org.jetbrains.kotlin.jvm") apply false
    id("org.jetbrains.kotlin.plugin.allopen") apply false
    id("io.quarkus") apply false
    id("io.quarkus.extension") apply false
    id("com.google.protobuf") apply false
}

allprojects {
    group = property("group") as String
    version = property("version") as String
}

subprojects {
    val jdkToolchainVersion = rootProject.property("jdkToolchainVersion") as String
    val jvmTargetVersion = rootProject.property("jvmTargetVersion") as String

    repositories {
        mavenCentral()
    }

    pluginManager.withPlugin("java") {
        the<JavaPluginExtension>().apply {
            toolchain {
                languageVersion.set(JavaLanguageVersion.of(jdkToolchainVersion.toInt()))
            }
            // Library floor (bytecode / API), independent of the build toolchain above.
            sourceCompatibility = JavaVersion.toVersion(jvmTargetVersion)
            targetCompatibility = JavaVersion.toVersion(jvmTargetVersion)
        }
        tasks.withType<Test>().configureEach {
            useJUnitPlatform()
            systemProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager")
        }
    }
}
