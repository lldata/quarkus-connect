plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.allopen")
    id("io.quarkus")
}

val quarkusVersion = rootProject.property("quarkusVersion") as String
val jdkToolchainVersion = rootProject.property("jdkToolchainVersion") as String
val jvmTargetVersion = rootProject.property("jvmTargetVersion") as String

description =
    "Thin connectrpc/conformance ConformanceService host for quarkus-connect (ServerCompat stdin/stdout)"

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
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("com.google.protobuf:protobuf-kotlin")
    implementation("com.google.api.grpc:proto-google-common-protos")
    implementation("io.grpc:grpc-protobuf")
    implementation("io.grpc:grpc-stub")
}

/**
 * Official connectconformance suite against this module's uber-jar.
 * Downloads the runner if missing; partial feature matrix + known-failing (see config/).
 * Wired into `check` so CI covers it without a separate workflow.
 */
tasks.register<Exec>("conformanceSuite") {
    group = "verification"
    description = "Build conformance uber-jar and run scripts/run-conformance.sh"
    dependsOn("quarkusBuild")
    workingDir = rootProject.projectDir
    commandLine("bash", "scripts/run-conformance.sh")
}

tasks.named("check") {
    dependsOn("conformanceSuite")
}
