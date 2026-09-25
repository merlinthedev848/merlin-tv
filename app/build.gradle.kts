plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("dagger.hilt.android.plugin")
    kotlin("kapt")
}

android {
    namespace = "com.example.merlinmedia"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.merlinmedia"
        minSdk = 23
        targetSdk = 35
        versionCode = 203
        versionName = "2.0.3"
    }

    signingConfigs {
        create("merlinSigning") {
            val ksFile = rootProject.file("keystore/merlin.jks")
            if (ksFile.exists()) {
                val storePass = System.getenv("MERLIN_KEYSTORE_PASSWORD") 
                    ?: (project.findProperty("MERLIN_KEYSTORE_PASSWORD") as? String) 
                    ?: "merlinpassword123"
                val keyAliasVal = System.getenv("MERLIN_KEY_ALIAS") 
                    ?: (project.findProperty("MERLIN_KEY_ALIAS") as? String) 
                    ?: "merlin"
                val keyPass = System.getenv("MERLIN_KEY_PASSWORD") 
                    ?: (project.findProperty("MERLIN_KEY_PASSWORD") as? String) 
                    ?: "merlinpassword123"

                storeFile = ksFile
                storePassword = storePass
                keyAlias = keyAliasVal
                keyPassword = keyPass
                enableV1Signing = true
                enableV2Signing = true
                enableV3Signing = true
                enableV4Signing = true
            }
        }
    }

    buildFeatures { compose = true; buildConfig = true }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("merlinSigning")
        }
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("merlinSigning")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    packaging {
        resources {
            excludes += setOf(
                "/META-INF/{AL2.0,LGPL2.1}",
                "/META-INF/*.version",
                "/META-INF/DEPENDENCIES",
                "/META-INF/INDEX.LIST"
            )
        }
    }

    applicationVariants.all {
        val variant = this
        variant.outputs.all {
            val outputImpl = this as? com.android.build.gradle.internal.api.BaseVariantOutputImpl
            if (outputImpl != null) {
                outputImpl.outputFileName = "merlin-tv-${variant.versionName}.apk"
            }
        }
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.media3:media3-exoplayer:1.5.1")
    implementation("androidx.media3:media3-exoplayer-hls:1.5.1")
    implementation("androidx.media3:media3-ui:1.5.1")
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.jakewharton.timber:timber:5.0.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("com.google.dagger:hilt-android:2.51")
    kapt("com.google.dagger:hilt-compiler:2.51")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")
    testImplementation("junit:junit:4.13.2")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
