import com.android.build.gradle.internal.tasks.factory.dependsOn
import java.io.IOException
import java.security.MessageDigest
import java.util.regex.Pattern
import javax.xml.bind.DatatypeConverter

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlinx.gettext)
    alias(libs.plugins.aboutlibraries.android)
}

fun String.runCommand(workingDir: File = File(".")): String? {
    try {
        val proc = ProcessBuilder(*trim().split("\\s".toRegex()).toTypedArray())
            .directory(workingDir)
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .redirectError(ProcessBuilder.Redirect.PIPE)
            .start()
        assert(proc.waitFor(1, TimeUnit.MINUTES))
        return proc.inputStream.bufferedReader().readText()
    } catch (e: IOException) {
        e.printStackTrace()
        return null
    }
}

fun getGitHash(): String? = "git log --format=%h -1".runCommand()?.trim()

val versionMajor: String? = System.getenv("EMULATOR_VERSION_MAJOR")
val versionMinor: String? = System.getenv("EMULATOR_VERSION_MINOR")

fun getVersionName(): String {
    if (versionMajor != null && versionMinor != null)
        return "$versionMajor.$versionMinor"
    return getGitHash() ?: "1.0"
}

fun getVersionCode(): Int = System.getenv("VERSION_CODE")?.toIntOrNull() ?: 1

val cemuDataFilesFolder = "../../../bin"

android {
    namespace = "info.cemu.cemu"
    compileSdk = 36
    ndkVersion = "26.3.11579264"
    defaultConfig {
        applicationId = "info.cemu.cemu"
        minSdk = 31
        targetSdk = 35
        versionCode = getVersionCode()
        versionName = getVersionName()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    androidResources {
        ignoreAssetsPattern = "!*cemu.mo:"
    }

    sourceSets.getByName("main") {
        assets {
            srcDir(cemuDataFilesFolder)
        }
    }

    packaging {
        jniLibs.useLegacyPackaging = true
    }

    val keystoreFilePath: String? = System.getenv("ANDROID_KEYSTORE_FILE")

    signingConfigs {
        if (keystoreFilePath != null) {
            create("release") {
                storeFile = file(keystoreFilePath)
                storePassword = System.getenv("ANDROID_KEY_STORE_PASSWORD")
                keyAlias = System.getenv("ANDROID_KEY_ALIAS")
                keyPassword = System.getenv("ANDROID_KEY_STORE_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
        }
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = if (keystoreFilePath != null) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
        }
    }

    compileOptions {
        sourceCompatibility(JavaVersion.VERSION_17)
        targetCompatibility(JavaVersion.VERSION_17)
    }

    externalNativeBuild {
        cmake {
            version = "3.25.0+"
            path = file("../../../CMakeLists.txt")
        }
    }

    defaultConfig {
        externalNativeBuild {
            cmake {
                arguments(
                    "-DANDROID_STL=c++_shared",
                    "-DENABLE_VCPKG=ON",
                    "-DVCPKG_TARGET_ANDROID=ON",
                    "-DENABLE_SDL=OFF",
                    "-DENABLE_WXWIDGETS=OFF",
                    "-DENABLE_OPENGL=OFF",
                    "-DENABLE_BLUEZ=OFF",
                    "-DBUNDLE_SPEEX=ON",
                    "-DENABLE_DISCORD_RPC=OFF",
                    "-DENABLE_NSYSHID_LIBUSB=OFF",
                    "-DENABLE_WAYLAND=OFF",
                    "-DENABLE_HIDAPI=OFF"
                )
                if (versionMajor != null && versionMinor != null) {
                    arguments.addAll(
                        arrayOf(
                            "-DEMULATOR_VERSION_MAJOR=$versionMajor",
                            "-DEMULATOR_VERSION_MINOR=$versionMinor"
                        )
                    )
                }
                abiFilters("arm64-v8a")
            }
        }
    }

    buildFeatures {
        buildConfig = true
        dataBinding = true
        viewBinding = true
        compose = true
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

abstract class ComputeCemuDataFilesHashTask : DefaultTask() {
    private val ignoreFilePatterns = arrayOf(
        Pattern.compile(".*cemu\\.mo"),
        Pattern.compile(".*Cemu_(?:debug|release)"),
    )

    @get:Input
    abstract val cemuDataFolder: Property<String>

    private fun isFileIgnored(file: File): Boolean {
        return ignoreFilePatterns.any { pattern -> pattern.matcher(file.path).matches() }
    }

    @TaskAction
    fun computeCemuDataFilesHash() {
        val assetDir = File(project.projectDir, "src/main/assets")
        if (!assetDir.exists()) {
            assetDir.mkdirs()
        }

        val cemuDataFilesDir = File(project.projectDir, cemuDataFolder.get())
        val hashFile = File(assetDir, "hash.txt")
        val md = MessageDigest.getInstance("SHA-256")

        if (!cemuDataFilesDir.isDirectory) {
            hashFile.writeText("invalid")
            return
        }

        val fileHashes = cemuDataFilesDir.walkTopDown()
            .filter { it.isFile && !isFileIgnored(it) }
            .sortedBy { it.path }
            .map {
                md.reset()
                md.update(it.path.toByteArray())
                md.update(it.readBytes())
                md.digest()
            }
            .toList()

        md.reset()
        fileHashes.forEach { md.update(it) }

        hashFile.writeText(DatatypeConverter.printHexBinary(md.digest()))
    }
}

val computeCemuDataFilesHashTask =
    tasks.register<ComputeCemuDataFilesHashTask>("computeCemuDataFilesHash") {
        cemuDataFolder = cemuDataFilesFolder
    }
tasks.preBuild.dependsOn(computeCemuDataFilesHashTask)

gettext {
    potFile.set(File(projectDir, "cemu_kt.pot"))
    keywords.set(listOf("tr", "trNoop"))
}

dependencies {
    implementation(libs.aboutlibraries.compose.m3)
    implementation(libs.kotlinx.gettext)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.compose.material3)
    testImplementation(libs.junit)
    testImplementation(libs.archunit.junit4)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    implementation(libs.okhttp)
    implementation(libs.okhttp.coroutines)
    implementation(libs.androidx.appcompat)
    implementation(libs.google.android.material)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.core.ktx)
}
