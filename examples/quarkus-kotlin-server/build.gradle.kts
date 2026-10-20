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
