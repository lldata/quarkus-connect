plugins {
    `java-library`
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.allopen")
    id("io.quarkus.extension")
}

val quarkusVersion = rootProject.property("quarkusVersion") as String
val protobufVersion = rootProject.property("protobufVersion") as String
val jdkToolchainVersion = rootProject.property("jdkToolchainVersion") as String
val jvmTargetVersion = rootProject.property("jvmTargetVersion") as String

description = "Connect RPC unary server protocol for Quarkus (Vert.x HTTP)"

quarkusExtension {
    deploymentModule.set("quarkus-connect-deployment")
}

allOpen {
    annotation("jakarta.enterprise.context.ApplicationScoped")
}

kotlin {
    jvmToolchain(jdkToolchainVersion.toInt())
    compilerOptions {
        // Library floor, not build floor — same split as the house reference repo.
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.fromTarget(jvmTargetVersion))
        apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_0)
        languageVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_0)
        freeCompilerArgs.add("-Xjdk-release=$jvmTargetVersion")
        javaParameters.set(true)
    }
}

dependencies {
    // `platform` (not enforcedPlatform) — Central rejects published enforced platforms.
    implementation(platform("io.quarkus.platform:quarkus-bom:$quarkusVersion"))
    implementation("io.quarkus:quarkus-arc")
    implementation("io.quarkus:quarkus-vertx-http")
    implementation("io.quarkus:quarkus-kotlin")
    // Bridge ordinary @GrpcService beans (shared HTTP port via use-separate-server=false)
    implementation("io.quarkus:quarkus-grpc")
    implementation("io.quarkus:quarkus-grpc-api")
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("com.google.protobuf:protobuf-java:$protobufVersion")
    implementation("com.google.protobuf:protobuf-java-util:$protobufVersion")
    implementation("io.grpc:grpc-api")
    implementation("io.grpc:grpc-protobuf")
    implementation("io.grpc:grpc-stub")
    implementation("com.google.api.grpc:proto-google-common-protos")

    annotationProcessor("io.quarkus:quarkus-extension-processor:$quarkusVersion")

    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
}
