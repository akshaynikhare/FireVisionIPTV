plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.ksp)
    id("kotlin-parcelize")
    alias(libs.plugins.hilt)
    id("com.google.gms.google-services")
    id("io.sentry.android.gradle")
    id("jacoco")
}

android {
    namespace = "com.cadnative.firevisioniptv"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.cadnative.firevisioniptv"
        minSdk = 23
        // English-only UI; without this the APK carries every locale shipped by
        // AndroidX, Material3 and Play Services.
        resourceConfigurations += listOf("en")
        targetSdk = 36
        versionCode = 5
        versionName = if (project.hasProperty("versionName")) {
            project.property("versionName") as String
        } else {
            "1.5"
        }
        
        // API Base URL configuration
        buildConfigField("String", "API_BASE_URL", "\"https://tv.cadnative.com/\"")
        manifestPlaceholders["sentryDsn"] = System.getenv("SENTRY_DSN") ?: ""
        manifestPlaceholders["sentryEnvironment"] = "debug"
    }

    signingConfigs {
        create("release") {
            val signingKeyStore = System.getenv("SIGNING_KEY_STORE")
            if (signingKeyStore != null) {
                storeFile = file(signingKeyStore)
                storePassword = System.getenv("SIGNING_STORE_PASSWORD")
                keyAlias = System.getenv("SIGNING_KEY_ALIAS")
                keyPassword = System.getenv("SIGNING_KEY_PASSWORD")
            }
        }
    }

    buildFeatures {
        buildConfig = true
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }

    lint {
        abortOnError = false
        checkReleaseBuilds = false
        // Release resource shrinking handles legacy XML assets; dependency upgrades and
        // icon/vector redesigns are tracked separately from correctness lint.
        disable += setOf(
            "AndroidGradlePluginVersion",
            "GradleDependency",
            "UnusedResources",
            "PrivateResource",
            "VectorPath",
            "IconLauncherShape",
            "IconDipSize"
        )
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            manifestPlaceholders["sentryEnvironment"] = "production"
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
        create("dev") {
            initWith(getByName("debug"))
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        // java.time is the EPG's core domain type (EpgProgram.startTime et al) and
        // only exists natively from API 26 — desugaring is what lets minSdk drop.
        isCoreLibraryDesugaringEnabled = true
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }
}

dependencies {
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    // AndroidX Leanback (updated)
    implementation(libs.androidx.leanback)
    implementation(libs.androidx.appcompat)

    // Coil for modern image loading
    implementation(libs.coil)
    implementation(libs.coil.compose)

    // Firebase - using BoM for version management
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)

    // TV Provider support
    implementation(libs.androidx.tvprovider)

    // Media3 ExoPlayer (updated)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.exoplayer.hls)
    implementation(libs.androidx.media3.exoplayer.dash)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.media3.datasource.okhttp)

    // Jetpack Compose for TV
    implementation(libs.androidx.compose.runtime)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.tv.foundation)
    implementation(libs.androidx.tv.material)
    implementation(libs.androidx.activity.compose)

    // Hilt Dependency Injection
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.work)
    ksp(libs.hilt.work.compiler)
    implementation(libs.hilt.navigation.compose)

    // Security
    implementation(libs.security.crypto)

    // Material Icons Extended
    implementation(libs.compose.material.icons.extended)

    // Room Database
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Retrofit & OkHttp
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)

    // Kotlin Coroutines
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    // WorkManager
    implementation(libs.androidx.work.runtime.ktx)

    // Sentry
    implementation(libs.sentry.android)

    // Navigation Component (Compose only)
    implementation(libs.androidx.navigation.compose)

    // Lifecycle (updated)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Amazon Appstore SDK (DRM license verification)
    implementation(libs.amazon.appstore)

    // QR Code generation
    implementation(libs.zxing.core)

    // Lottie animation (splash screen)
    implementation(libs.lottie.compose)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

sentry {
    includeSourceContext = true
    org = "cadnative-design-solution"
    projectName = "firevisioniptv"
    authToken = System.getenv("SENTRY_AUTH_TOKEN")
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
        "**/Manifest*.*", "**/*Test*.*", "android/**/*.*",
        // Hilt/DI generated code
        "**/*_Hilt*.*", "**/Hilt_*.*", "**/*_Factory.*",
        "**/*_MembersInjector.*", "**/Dagger*.*",
        "**/di/**", "**/hilt_aggregated_deps/**", "**/dagger/**",
        // Compose UI — requires instrumented tests, not unit tests
        "**/presentation/ui/screens/**",
        "**/presentation/ui/components/**",
        "**/presentation/ui/theme/**",
        "**/presentation/ui/animation/**",
        "**/presentation/ui/utils/**",
        "**/presentation/navigation/**",
        // Android framework classes not testable in unit tests
        "**/*Activity*.*",
        "**/*Application*.*",
        "**/*Service*.*",
        "**/*Receiver*.*",
        "**/worker/**",
        "**/ChannelManager*.*",
        "**/update/**",
        "**/security/**",
        // Room-generated code — requires instrumented tests, not unit tests
        "**/*Dao_Impl*.*",
        "**/*Database_Impl*.*",
        // Hardware-dependent services — not testable in JVM unit tests
        "**/ChannelThumbnailExtractor*.*",
        "**/drm/**"
    )
    classDirectories.setFrom(
        fileTree("${layout.buildDirectory.get()}/intermediates/classes/debug/transformDebugClassesWithAsm/dirs") {
            exclude(fileFilter)
        }
    )
    sourceDirectories.setFrom(files("${projectDir}/src/main/kotlin", "${projectDir}/src/main/java"))
    executionData.setFrom(fileTree(layout.buildDirectory.get()) {
        include("jacoco/testDebugUnitTest.exec")
    })

    // classDirectories points at an AGP-internal intermediates path that only
    // exists because a plugin runs ASM instrumentation. If AGP renames it, or the
    // last instrumenting plugin is removed, jacoco reports 0% rather than failing
    // — so assert we actually resolved some bytecode.
    doFirst {
        val hasClasses = classDirectories.files.any { dir ->
            dir.exists() && dir.walkTopDown().any { it.extension == "class" }
        }
        require(hasClasses) {
            "jacoco resolved no .class files — the AGP ASM intermediates path has moved. " +
                "Check app/build/intermediates/classes/debug/."
        }
    }
}
