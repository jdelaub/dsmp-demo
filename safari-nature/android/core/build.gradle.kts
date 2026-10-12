// The catalogue, the explorer's progress (stars, levels, medals), the daily challenge and the parents' summary.
// Pure Kotlin on the JVM so it is tested in seconds; the app only draws it.
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

// Built with JDK 21; bytecode stays 17 so Android can consume it.
java { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }

dependencies {
    api(libs.kotlinx.serialization.json)
    testImplementation(libs.junit)
}

tasks.test {
    // The photo checks look at the app's assets: every animal must have its picture.
    systemProperty("photos.dir", rootProject.file("app/src/main/assets/photos").absolutePath)
    testLogging { events("failed"); exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL }
}
