import org.gradle.kotlin.dsl.support.serviceOf

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id ("kotlin-parcelize")
}

android {
    signingConfigs {
        create("launcher") {
            storeFile = file("C:\\Users\\Zeyro\\OneDrive\\Desktop\\keys\\selaura.jks")
            storePassword = "391720pass"
            keyPassword = "391720pass"
            keyAlias = "launcher"
        }
    }
    namespace = "com.selauraclient.launcher"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.selauraclient.launcher"
        minSdk = 28
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {//noinspection ChromeOsAbiSupport
            abiFilters += listOf("arm64-v8a")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("launcher")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        buildConfig = true
        compose = true
    }
}

val prepareLauncherDex by tasks.registering {
    group = "build"
    description = "build launcher jar, dex it, rename & copy into assets, then update client"
    dependsOn(":launcher:createFullJarRelease")
    val launcherJar = project(":launcher").layout.buildDirectory.file(
        "intermediates/full_jar/release/createFullJarRelease/full.jar"
    )
    val assetsDir = layout.projectDirectory.dir("src/main/assets")
    inputs.file(launcherJar)
    outputs.file(assetsDir.file("launcher.dex"))

    doFirst {
        val jarFile = launcherJar.get().asFile
        val outDir = assetsDir.asFile
        val sdkDir = android.sdkDirectory.absolutePath
        val btVersion = android.buildToolsVersion
        val isWindows = System.getProperty("os.name").startsWith("Windows")
        val d8Path = "$sdkDir${File.separator}build-tools${File.separator}$btVersion${File.separator}d8${if (isWindows) ".bat" else ""}"
        val execOperations = project.serviceOf<ExecOperations>()

        execOperations.exec {
            commandLine(d8Path, jarFile.absolutePath, "--output", outDir.absolutePath)
        }

        val classesDex = outDir.resolve("classes.dex")
        val launcherDex = outDir.resolve("launcher.dex")
        if (launcherDex.exists()) launcherDex.delete()
        classesDex.renameTo(launcherDex)
    }
}

tasks.named("preBuild") {
    dependsOn(prepareLauncherDex)
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.browser)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    implementation(libs.androidx.ui.text.google.fonts)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.okhttp)
    implementation(libs.gson)
    implementation(libs.ackpine.core)
    implementation(libs.ackpine.ktx)
    implementation(libs.ackpine.splits)
    implementation(libs.ackpine.splits.ktx)
    implementation(libs.gplayapi)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.documentfile)
}