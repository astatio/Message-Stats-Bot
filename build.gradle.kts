plugins {
    kotlin("jvm") version "2.1.20-Beta1"
    application
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("io.realm.kotlin") version "3.0.0"
}

group = "madeby.astatio"
version = "1.1"

repositories {
    google()
    mavenCentral()
    maven {
        url = uri("https://m2.dv8tion.net/releases")
        name = "m2-dv8tion"
    }
    maven("https://jitpack.io/")
    maven("https://plugins.gradle.org/m2/")
    maven("https://oss.sonatype.org/content/repositories/snapshots")
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

dependencies {
    testImplementation(kotlin("test"))

    //JDA, KTX, COROUTINES
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
    implementation("net.dv8tion:JDA:5.2.3")
    implementation("io.github.minndevelopment:jda-ktx:9370cb1")

    //LOGGING
    implementation("ch.qos.logback:logback-classic:1.5.6")

    //DATABASE
    implementation("io.realm.kotlin:library-base:2.1.0")
}

tasks.test {
    useJUnitPlatform()
}

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(22)
	}
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    isZip64 = true
    // minimize() it does not work with Caffeine unfortunately
}

application {
    mainClass.set("MainKt")
}
