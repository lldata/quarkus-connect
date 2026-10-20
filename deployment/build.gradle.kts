plugins {
    `java-library`
}

val quarkusVersion = rootProject.property("quarkusVersion") as String
val jdkToolchainVersion = rootProject.property("jdkToolchainVersion") as String

description = "Quarkus Connect RPC - Deployment"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(jdkToolchainVersion.toInt()))
    }
}

dependencies {
    implementation(project(":quarkus-connect"))

    // `platform` (not enforcedPlatform) — Central rejects published enforced platforms.
    implementation(platform("io.quarkus.platform:quarkus-bom:$quarkusVersion"))
    implementation("io.quarkus:quarkus-arc-deployment")
    implementation("io.quarkus:quarkus-vertx-http-deployment")
    implementation("io.quarkus:quarkus-kotlin-deployment")
    implementation("io.quarkus:quarkus-grpc-deployment")

    annotationProcessor("io.quarkus:quarkus-extension-processor:$quarkusVersion")

    testImplementation("io.quarkus:quarkus-junit5-internal")
}
