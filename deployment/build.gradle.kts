plugins {
    `java-library`
}

val quarkusVersion = rootProject.property("quarkusVersion") as String

description = "Quarkus Connect RPC - Deployment"

dependencies {
    implementation(project(":quarkus-connect"))

    implementation(enforcedPlatform("io.quarkus.platform:quarkus-bom:$quarkusVersion"))
    implementation("io.quarkus:quarkus-arc-deployment")
    implementation("io.quarkus:quarkus-vertx-http-deployment")
    implementation("io.quarkus:quarkus-kotlin-deployment")

    annotationProcessor("io.quarkus:quarkus-extension-processor:$quarkusVersion")

    testImplementation("io.quarkus:quarkus-junit5-internal")
}
