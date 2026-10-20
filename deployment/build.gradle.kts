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

    implementation(enforcedPlatform("io.quarkus.platform:quarkus-bom:$quarkusVersion"))
    implementation("io.quarkus:quarkus-arc-deployment")
    implementation("io.quarkus:quarkus-vertx-http-deployment")
    implementation("io.quarkus:quarkus-kotlin-deployment")

    annotationProcessor("io.quarkus:quarkus-extension-processor:$quarkusVersion")

    testImplementation("io.quarkus:quarkus-junit5-internal")
}
