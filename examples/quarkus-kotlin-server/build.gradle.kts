plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.allopen")
    id("io.quarkus")
}

val quarkusVersion = rootProject.property("quarkusVersion") as String
val jdkToolchainVersion = rootProject.property("jdkToolchainVersion") as String
val jvmTargetVersion = rootProject.property("jvmTargetVersion") as String

description = "Example Quarkus Kotlin server — @GrpcService on one HTTP port (gRPC + Connect + gRPC-Web)"

allOpen {
    annotation("jakarta.enterprise.context.ApplicationScoped")
    annotation("io.quarkus.grpc.GrpcService")
}

kotlin {
    jvmToolchain(jdkToolchainVersion.toInt())
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.fromTarget(jvmTargetVersion))
        freeCompilerArgs.add("-Xjdk-release=$jvmTargetVersion")
        javaParameters.set(true)
    }
}

dependencies {
    implementation(enforcedPlatform("io.quarkus.platform:quarkus-bom:$quarkusVersion"))
    implementation(project(":quarkus-connect"))
    implementation("io.quarkus:quarkus-kotlin")
    implementation("io.quarkus:quarkus-grpc")
    implementation("io.quarkus:quarkus-opentelemetry")
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("com.google.protobuf:protobuf-kotlin")

    testImplementation("io.quarkus:quarkus-junit5")
    testImplementation("io.rest-assured:rest-assured")
    // Enables quarkus.otel.traces.exporter=logging in tests (CI without OTLP collector).
    testImplementation("io.opentelemetry:opentelemetry-exporter-logging")
}

/**
 * Black-box Connect curl matrix against this example's uber-jar.
 * Wired into `check` so CI (`./gradlew check`) covers live interop without a separate workflow.
 * Depends on quarkusBuild in this same Gradle invocation — do not nest ./gradlew in the
 * script (that rewrote :quarkus-connect:jar under parallel IT; see #52).
 */
tasks.register<Exec>("interopMatrix") {
    group = "verification"
    description = "Build example uber-jar and run scripts/run-interop-matrix.sh"
    dependsOn("quarkusBuild")
    workingDir = rootProject.projectDir
    commandLine("bash", "scripts/run-interop-matrix.sh")
}

tasks.named("check") {
    dependsOn("interopMatrix")
}

// Belt-and-suspenders with the same-build quarkusBuild: keep example packaging after ITs
// so parallel project execution cannot race SharedArchivePathTree on the runtime jar.
gradle.projectsEvaluated {
    val itTest = rootProject.findProject(":integration-tests")?.tasks?.findByName("test")
    val secTest = rootProject.findProject(":integration-tests-security")?.tasks?.findByName("test")
    listOfNotNull(tasks.findByName("quarkusBuild"), tasks.findByName("interopMatrix")).forEach { downstream ->
        listOfNotNull(itTest, secTest).forEach { upstream ->
            downstream.mustRunAfter(upstream)
        }
    }
}
