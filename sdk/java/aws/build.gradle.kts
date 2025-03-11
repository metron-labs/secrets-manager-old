import org.gradle.kotlin.dsl.`maven-publish`
import org.gradle.kotlin.dsl.signing

group = "com.keepersecurity.secrets-manager.aws.kms"
version = "1.0.0"

plugins {
    id ("java");
    kotlin("jvm") version "2.0.20"
    kotlin("plugin.serialization") version "2.0.20"
    `maven-publish`
    signing
    id("io.github.gradle-nexus.publish-plugin") version "2.0.0"
}
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))  // Ensure it uses Java 11
    }
}

repositories {
    // Use Maven Central for resolving dependencies.
    mavenCentral()
}

dependencies {
	implementation("com.keepersecurity.secrets-manager:core:17.0.0")
		
	implementation ("software.amazon.awssdk:kms:2.20.28")
    implementation ("software.amazon.awssdk:auth:2.20.28")
	
	implementation("com.fasterxml.jackson.core:jackson-databind:2.18.2")
	implementation("com.fasterxml.jackson.core:jackson-core:2.18.2")
	
	implementation("com.google.code.gson:gson:2.12.1")

    implementation("org.slf4j:slf4j-api:1.7.32"){
        exclude("org.slf4j:slf4j-log4j12")
    }
	implementation("ch.qos.logback:logback-classic:1.2.6")
	implementation("ch.qos.logback:logback-core:1.2.6")
}



tasks.named<Test>("test") {
    // Use JUnit Platform for unit tests.
    useJUnitPlatform()
}
