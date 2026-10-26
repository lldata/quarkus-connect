import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.JavaLibrary
import com.vanniktech.maven.publish.KotlinJvm
import com.vanniktech.maven.publish.SourcesJar

plugins {
    base
    id("org.jetbrains.kotlin.jvm") apply false
    id("org.jetbrains.kotlin.plugin.allopen") apply false
    id("io.quarkus") apply false
    id("io.quarkus.extension") apply false
    id("com.google.protobuf") apply false
    // Publishing: applied only to the two published modules below.
    id("com.vanniktech.maven.publish") version "0.37.0" apply false
    id("org.jetbrains.dokka") version "2.2.0" apply false
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

val repository = "https://github.com/lldata/quarkus-connect"

/** A `-SNAPSHOT` is not signed and Central does not ask it to be. */
val isSnapshot = version.toString().endsWith("SNAPSHOT")

/**
 * Signing needs a key, and `publishToMavenLocal` is the documented way to try the library out —
 * so it must not require one. Signing therefore turns on when the key is there rather than on
 * the version being a release; the publish workflow refuses to run at all without the secrets.
 */
val canSign =
    listOf("signingInMemoryKey", "signingInMemoryKeyPassword")
        .all { !project.findProperty(it)?.toString().isNullOrBlank() }

/**
 * Published artifacts only. Examples and integration-tests are not published.
 *
 * Central Portal upload is deliberate (not `automaticRelease`): a human presses Publish.
 */
data class PublishedModule(
    val projectPath: String,
    val artifactId: String,
    val summary: String,
    val kotlin: Boolean,
)

val published =
    listOf(
        PublishedModule(
            ":quarkus-connect",
            "quarkus-connect",
            "Connect RPC server for Quarkus — serve @GrpcService as gRPC + Connect + gRPC-Web on one HTTP port.",
            kotlin = true,
        ),
        PublishedModule(
            ":quarkus-connect-deployment",
            "quarkus-connect-deployment",
            "Quarkus build-time steps for quarkus-connect.",
            kotlin = false,
        ),
    )

published.forEach { mod ->
    project(mod.projectPath) {
        apply(plugin = "com.vanniktech.maven.publish")
        if (mod.kotlin) {
            apply(plugin = "org.jetbrains.dokka")
        }

        val configurePublication: () -> Unit = {
            configure<com.vanniktech.maven.publish.MavenPublishBaseExtension> {
                publishToMavenCentral()
                if (!isSnapshot && canSign) {
                    signAllPublications()
                }

                if (mod.kotlin) {
                    configure(
                        KotlinJvm(
                            javadocJar = JavadocJar.Dokka("dokkaGeneratePublicationHtml"),
                            sourcesJar = SourcesJar.Sources(),
                        ),
                    )
                } else {
                    // Deployment module has only build-step classes; empty javadoc jar satisfies Central.
                    configure(
                        JavaLibrary(
                            javadocJar = JavadocJar.Empty(),
                            sourcesJar = SourcesJar.Sources(),
                        ),
                    )
                }

                coordinates(rootProject.group.toString(), mod.artifactId, rootProject.version.toString())

                pom {
                    name.set(mod.artifactId)
                    description.set(mod.summary)
                    url.set(repository)
                    organization {
                        name.set("LL Data ApS")
                        url.set("https://lldata.dk")
                    }
                    issueManagement {
                        system.set("GitHub")
                        url.set("$repository/issues")
                    }
                    licenses {
                        license {
                            name.set("Apache License 2.0")
                            url.set("https://raw.githubusercontent.com/lldata/quarkus-connect/main/LICENSE")
                            distribution.set("repo")
                        }
                    }
                    scm {
                        url.set(repository)
                        connection.set("scm:git:$repository.git")
                        developerConnection.set("scm:git:ssh://git@github.com/lldata/quarkus-connect.git")
                    }
                    developers {
                        developer {
                            id.set("lldata")
                            name.set("Lasse Lindgard")
                            email.set("lasse@lldata.dk")
                        }
                    }
                }
            }
        }

        // Root script is evaluated before module plugins apply; wait for the language plugin.
        if (mod.kotlin) {
            plugins.withId("org.jetbrains.kotlin.jvm") { configurePublication() }
        } else {
            plugins.withId("java-library") { configurePublication() }
        }
    }
}

/**
 * Avoid Quarkus SharedArchivePathTree / ZipCentralDirectory flakes in CI when
 * `:integration-tests:test` (or security ITs) augments while `:conformance:quarkusBuild`
 * rewrites the same runtime jar under Gradle parallel project execution.
 */
gradle.projectsEvaluated {
    val itTest = findProject(":integration-tests")?.tasks?.findByName("test")
    val secTest = findProject(":integration-tests-security")?.tasks?.findByName("test")
    val confBuild = findProject(":conformance")?.tasks?.findByName("quarkusBuild")
    val confSuite = findProject(":conformance")?.tasks?.findByName("conformanceSuite")
    val exampleCheck = findProject(":examples:quarkus-kotlin-server")?.tasks?.findByName("check")
    listOfNotNull(confBuild, confSuite).forEach { downstream ->
        listOfNotNull(itTest, secTest).forEach { upstream ->
            downstream.mustRunAfter(upstream)
        }
    }
    if (exampleCheck != null) {
        listOfNotNull(itTest, secTest).forEach { upstream ->
            exampleCheck.mustRunAfter(upstream)
        }
    }
}
