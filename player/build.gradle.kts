plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    id("jacoco")
}

android {
    namespace = "com.cabletech.iptvplayer.player"
    compileSdk = 34

    defaultConfig {
        minSdk = 22
        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions { jvmTarget = "17" }

    buildFeatures { viewBinding = true }
}

dependencies {
    implementation(project(":core"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.exoplayer.hls)
    implementation(libs.media3.exoplayer.dash)
    implementation(libs.media3.exoplayer.rtsp)
    implementation(libs.media3.ui)
    implementation(libs.media3.session)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
}

tasks.withType<Test> {
    configure<JacocoTaskExtension> {
        isIncludeNoLocationClasses = true
        excludes = listOf("jdk.internal.*")
    }
}

tasks.register<JacocoReport>("jacocoTestReport") {
    dependsOn(tasks.named("testDebugUnitTest"))

    reports {
        xml.required.set(true)
        html.required.set(true)
    }

    val fileFilter = listOf(
        "**/R.class", "**/R\$*.class", "**/BuildConfig.*",
        "**/Manifest*.*", "**/*Test*.*", "android/**/*.*"
    )
    classDirectories.setFrom(
        fileTree("${layout.buildDirectory.get()}/tmp/kotlin-classes/debug") {
            exclude(fileFilter)
        }
    )
    sourceDirectories.setFrom(files("${projectDir}/src/main/kotlin", "${projectDir}/src/main/java"))
    executionData.setFrom(fileTree(layout.buildDirectory.get()) {
        include("jacoco/testDebugUnitTest.exec")
    })
}
