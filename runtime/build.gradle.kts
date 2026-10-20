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
        freeCompilerArgs.add("-Xjdk-release=$jvmTargetVersion")
        javaParameters.set(true)
    }
}

dependencies {
    implementation(enforcedPlatform("io.quarkus.platform:quarkus-bom:$quarkusVersion"))
    implementation("io.quarkus:quarkus-arc")
    implementation("io.quarkus:quarkus-vertx-http")
    implementation("io.quarkus:quarkus-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("com.google.protobuf:protobuf-java:$protobufVersion")
    implementation("com.google.protobuf:protobuf-java-util:$protobufVersion")

    annotationProcessor("io.quarkus:quarkus-extension-processor:$quarkusVersion")
}
