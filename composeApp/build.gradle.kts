import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import java.util.Properties

plugins {

    alias(libs.plugins.kotlinMultiplatform)

    alias(libs.plugins.androidApplication)

    alias(libs.plugins.jetbrainsCompose)

    kotlin("plugin.serialization") version "1.9.22"
}

kotlin {

    jvmToolchain(17)

    androidTarget {

        compilations.all {

            kotlinOptions {

                jvmTarget = "17"
            }
        }
    }

    jvm("desktop")

    listOf(

        iosX64(),

        iosArm64(),

        iosSimulatorArm64()

    ).forEach { iosTarget ->

        iosTarget.binaries.framework {

            baseName = "ComposeApp"

            isStatic = true
        }
    }

    sourceSets {

        val commonMain by getting {

            dependencies {

                implementation(kotlin("stdlib-common"))

                implementation(compose.runtime)

                implementation(compose.foundation)

                implementation(compose.material3)

                implementation(compose.ui)

                implementation(compose.materialIconsExtended)

                implementation(compose.components.resources)

                implementation(compose.components.uiToolingPreview)

                implementation(libs.kamel.image)

                implementation(
                    "org.jetbrains.kotlinx:kotlinx-datetime:0.6.0"
                )

                implementation(
                    libs.kotlinx.coroutines.core
                )

                implementation(
                    "org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2"
                )

                implementation(
                    libs.ktor.client.core
                )

                implementation(
                    libs.ktor.client.content.negotiation
                )

                implementation(
                    "io.ktor:ktor-serialization-kotlinx-json:2.3.8"
                )
            }
        }

        val androidMain by getting {

            dependencies {

                implementation(
                    libs.androidx.core.ktx
                )

                implementation(
                    libs.androidx.activity.compose
                )

                implementation(
                    libs.media3.exoplayer
                )

                implementation(
                    libs.media3.session
                )

                implementation(
                    libs.media3.ui
                )

                implementation(
                    libs.media3.cast
                )

                implementation(
                    libs.androidx.biometric
                )

                implementation(
                    libs.androidx.palette
                )

                implementation(
                    libs.androidx.splashscreen
                )

                implementation(
                    "androidx.palette:palette-ktx:1.0.0"
                )

                implementation(
                    "com.microsoft.onnxruntime:onnxruntime-android:1.17.1"
                )

                implementation(
                    "androidx.work:work-runtime-ktx:2.9.0"
                )

                implementation(
                    "io.github.junkfood02.youtubedl-android:library:0.18.1"
                )

                implementation(
                    "io.github.junkfood02.youtubedl-android:ffmpeg:0.18.1"
                )

                implementation(
                    "com.google.android.gms:play-services-cast-framework:21.4.0"
                )

                implementation(
                    "androidx.mediarouter:mediarouter:1.7.0"
                )

                implementation(
                    "io.ktor:ktor-client-okhttp:2.3.8"
                )

                implementation(
                    libs.glance.appwidget
                )

                implementation(
                    libs.glance.material3
                )

                implementation(
                    "net.jthink:jaudiotagger:3.0.1"
                )
            }
        }

        val desktopMain by getting {

            dependencies {

                implementation(
                    compose.desktop.currentOs
                )

                implementation(
                    libs.kotlinx.coroutines.swing
                )

                implementation(
                    libs.ktor.client.cio
                )

                /*
                 * =========================
                 * JAVAFX
                 * =========================
                 */

                val javafxVersion = "21.0.2"

                val platform =
                    when {

                        System.getProperty("os.name")
                            .lowercase()
                            .contains("mac")
                                &&
                                System.getProperty("os.arch")
                                    .contains("aarch64") -> {

                            "mac-aarch64"
                        }

                        System.getProperty("os.name")
                            .lowercase()
                            .contains("mac") -> {

                            "mac"
                        }

                        System.getProperty("os.name")
                            .lowercase()
                            .contains("win") -> {

                            "win"
                        }

                        else -> {

                            "linux"
                        }
                    }

                implementation(
                    "org.openjfx:javafx-base:$javafxVersion:$platform"
                )

                implementation(
                    "org.openjfx:javafx-graphics:$javafxVersion:$platform"
                )

                implementation(
                    "org.openjfx:javafx-controls:$javafxVersion:$platform"
                )

                /*
                 * =========================
                 * VLCJ
                 * =========================
                 */

                implementation(
                    "uk.co.caprica:vlcj:4.8.3"
                )

                implementation(
                    "uk.co.caprica:vlcj-natives:4.8.3"
                )

                implementation(
                    "net.java.dev.jna:jna:5.14.0"
                )

                implementation(
                    "net.java.dev.jna:jna-platform:5.14.0"
                )

                implementation(
                    "net.jthink:jaudiotagger:3.0.1"
                )
            }
        }

        val iosX64Main by getting

        val iosArm64Main by getting

        val iosSimulatorArm64Main by getting

        val iosMain by creating {

            dependsOn(commonMain)

            iosX64Main.dependsOn(this)

            iosArm64Main.dependsOn(this)

            iosSimulatorArm64Main.dependsOn(this)

            dependencies {

                implementation(
                    libs.ktor.client.darwin
                )
            }
        }
    }
}

android {

    namespace = "org.audiocular.peq.app"

    compileSdk = 34

    sourceSets["main"].manifest.srcFile(
        "src/androidMain/AndroidManifest.xml"
    )

    sourceSets["main"].res.srcDirs(
        "src/androidMain/res"
    )

    defaultConfig {

        applicationId = "org.audiocular.peq.app"

        minSdk = 24

        targetSdk = 34

        versionCode = 1

        versionName = "1.0.0"
    }

    packaging {

        jniLibs {

            useLegacyPackaging = true
        }

        resources {

            excludes +=
                "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    val localProps = Properties().apply {
        val locFile = rootProject.file("local.properties")
        if (locFile.exists()) {
            locFile.inputStream().use { load(it) }
        }
    }

    signingConfigs {
        create("release") {
            val keyPath = localProps["RELEASE_KEYSTORE_PATH"] as? String ?: ""
            if (keyPath.isNotEmpty()) {
                storeFile = file(keyPath)
                storePassword = localProps["RELEASE_STORE_PASSWORD"] as? String ?: ""
                keyAlias = localProps["RELEASE_KEY_ALIAS"] as? String ?: ""
                keyPassword = localProps["RELEASE_KEY_PASSWORD"] as? String ?: ""
            }
        }
    }

    buildTypes {

        getByName("release") {

            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            val keyPath = localProps["RELEASE_KEYSTORE_PATH"] as? String ?: ""
            if (keyPath.isNotEmpty()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {

        sourceCompatibility =
            JavaVersion.VERSION_17

        targetCompatibility =
            JavaVersion.VERSION_17
    }

    dependencies {

        debugImplementation(
            compose.uiTooling
        )
    }
}

compose.desktop {

    application {

        mainClass =
            "org.audiocular.peq.app.MainKt"

        /*
         * Disable Proguard
         */

        buildTypes.release.proguard {

            isEnabled.set(false)
        }

        /*
         * IMPORTANT
         *
         * DO NOT ADD:
         *
         * -Djna.library.path
         * -Djava.library.path
         * -Dvlcj.plugin.path
         *
         * These are now set dynamically
         * inside DesktopAudioEngine.
         *
         * Humans keep hardcoding paths,
         * then wonder why packaging breaks.
         */

        nativeDistributions {

            modules(

                "java.base",

                "java.desktop",

                "java.logging",

                "java.sql",

                "jdk.unsupported"
            )

            targetFormats(

                TargetFormat.Dmg,

                TargetFormat.Msi,

                TargetFormat.Deb
            )


    jvmArgs(

    "-Djna.library.path=/Users/manpreet/Desktop/AndroidStudioProjects/pEQ-App/composeApp/src/desktopMain/resources/vlc/lib",

    "-DVLC_PLUGIN_PATH=/Users/manpreet/Desktop/AndroidStudioProjects/pEQ-App/composeApp/src/desktopMain/resources/vlc/plugins",

    "-Djava.awt.headless=false",

    "-Dapple.awt.transparentTitleBar=true"
)
            packageName =
                "pEQNeuralWorkstation"

            packageVersion =
                "1.0.0"

            description =
                "AI Powered Neural Media Workstation"

            vendor =
                "pEQ Neural Team"

            macOS {

                bundleID =
                    "org.audiocular.peq.app"

                dockName =
                    "pEQ"

                minimumSystemVersion =
                    "12.0"

                infoPlist {
                    extraKeysRawXml = """
                        <key>CFBundleDocumentTypes</key>
                        <array>
                            <dict>
                                <key>CFBundleTypeRole</key>
                                <string>Viewer</string>
                                <key>LSHandlerRank</key>
                                <string>Alternate</string>
                                <key>CFBundleTypeName</key>
                                <string>Audio File</string>
                                <key>CFBundleTypeExtensions</key>
                                <array>
                                    <string>mp3</string>
                                    <string>flac</string>
                                    <string>wav</string>
                                    <string>m4a</string>
                                    <string>ogg</string>
                                    <string>aac</string>
                                </array>
                            </dict>
                            <dict>
                                <key>CFBundleTypeRole</key>
                                <string>Viewer</string>
                                <key>LSHandlerRank</key>
                                <string>Alternate</string>
                                <key>CFBundleTypeName</key>
                                <string>Video File</string>
                                <key>CFBundleTypeExtensions</key>
                                <array>
                                    <string>mp4</string>
                                    <string>mkv</string>
                                    <string>mov</string>
                                    <string>avi</string>
                                    <string>webm</string>
                                </array>
                            </dict>
                        </array>
                    """.trimIndent()
                }
            }
        }
    }
}