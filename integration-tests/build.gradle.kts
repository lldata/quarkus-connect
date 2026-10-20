plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.allopen")
    id("io.quarkus")
}

val quarkusVersion = rootProject.property("quarkusVersion") as String
val jdkToolchainVersion = rootProject.property("jdkToolchainVersion") as String
val jvmTargetVersion = rootProject.property("jvmTargetVersion") as String

description = "Quarkus Connect RPC - Integration Tests"

allOpen {
    annotation("jakarta.enterprise.context.ApplicationScoped")
    annotation("io.quarkus.test.junit.QuarkusTest")
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

    testImplementation("io.quarkus:quarkus-junit5")
    testImplementation("io.rest-assured:rest-assured")
    testImplementation("io.grpc:grpc-stub")
    implementation("com.google.api.grpc:proto-google-common-protos")
}
