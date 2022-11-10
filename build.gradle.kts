import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.7.20"
}

group = "de.htw-berlin.covid-spings"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.github.samtools:htsjdk:3.0.2")
    implementation("me.tongfei:progressbar:0.9.5")
    implementation("com.google.code.gson:gson:2.10")
    implementation("io.reactivex.rxjava3:rxkotlin:3.0.1")
    testImplementation(kotlin("test"))

}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}