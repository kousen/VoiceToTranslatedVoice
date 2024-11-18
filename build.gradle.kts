plugins {
    id("application")
}

group = "com.kousenit"
version = "1.0-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // Gson parser
    implementation("com.google.code.gson:gson:2.11.0")

    // Audio
    implementation("com.assemblyai:assemblyai-java:4.0.0")
    implementation("net.andrewcpu:elevenlabs-api:2.7.8")

    // Testing
    testImplementation(platform("org.junit:junit-bom:5.11.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core:3.25.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

application {
    mainClass.set("com.kousenit.AllTogether")
}
