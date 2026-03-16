@file:Suppress("UnstableApiUsage")

import com.android.build.api.dsl.ManagedVirtualDevice
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import java.io.FileInputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Properties

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.jetbrains.kotlin.android)
  alias(libs.plugins.ktlint)
  alias(libs.plugins.compose.compiler)
  id("androidx.navigation.safeargs")
  id("kotlin-parcelize")
  id("com.squareup.wire")
  id("translations")
  id("licenses")
  id("com.google.gms.google-services")

}

apply(from = "static-ips.gradle.kts")
//val canonicalVersionName = "1.0.0"
val canonicalVersionCode = 1568
val canonicalVersionName = "7.51.9.beta.1"
val currentHotfixVersion = 7
val maxHotfixVersions = 100


val keystores: Map<String, Properties?> = mapOf(
  "debug" to loadKeystoreProperties("D:/project/signal/key/keystore.release.properties"),
  "release" to loadKeystoreProperties("D:/project/signal/key/keystore.release.properties")
)

val selectableVariants = listOf(
  "nightlyBackupSpinner",
  "nightlyProdSpinner",
  "nightlyProdPerf",
  "nightlyProdRelease",
  "nightlyStagingRelease",
  "playProdDebug",
  "playProdSpinner",
  "playProdCanary",
  "playProdPerf",
  "playProdBenchmark",
  "playProdInstrumentation",
  "playProdRelease",
  "playStagingDebug",
  "playStagingCanary",
  "playStagingSpinner",
  "playStagingPerf",
  "playStagingInstrumentation",
  "playStagingRelease",
  "websiteProdSpinner",
  "websiteProdRelease"
)

val signalBuildToolsVersion: String by rootProject.extra
val signalCompileSdkVersion: String by rootProject.extra
val signalTargetSdkVersion: Int by rootProject.extra
val signalMinSdkVersion: Int by rootProject.extra
val signalNdkVersion: String by rootProject.extra
val signalJavaVersion: JavaVersion by rootProject.extra
val signalKotlinJvmTarget: String by rootProject.extra

wire {
  kotlin {
    javaInterop = true
  }

  sourcePath {
    srcDir("src/main/protowire")
  }

  protoPath {
    srcDir("${project.rootDir}/libsignal-service/src/main/protowire")
  }
  // Handled by libsignal
  prune("signalservice.DecryptionErrorMessage")
}

ktlint {
  version.set("1.2.1")
}

android {
  namespace = "org.thoughtcrime.securesms"
//  namespace = "group.ba.voiceapp"

  buildToolsVersion = signalBuildToolsVersion
  compileSdkVersion = signalCompileSdkVersion
  ndkVersion = signalNdkVersion

  flavorDimensions += listOf("distribution", "environment")
  testBuildType = "instrumentation"

  android.bundle.language.enableSplit = false

  kotlinOptions {
    jvmTarget = signalKotlinJvmTarget
    freeCompilerArgs = listOf("-Xjvm-default=all")
    suppressWarnings = true
  }

  keystores["debug"]?.let { properties ->
    signingConfigs.getByName("debug").apply {
      val storeFilePath = properties.getProperty("storeFile")
      storeFile = if (File(storeFilePath).isAbsolute) {
        file(storeFilePath)
      } else {
        file(storeFilePath)
      }
      storePassword = properties.getProperty("storePassword")
      keyAlias = properties.getProperty("keyAlias")
      keyPassword = properties.getProperty("keyPassword")
    }
  }

  keystores["release"]?.let { properties ->
    signingConfigs.create("release").apply {
      val storeFilePath = properties.getProperty("storeFile")
      storeFile = if (File(storeFilePath).isAbsolute) {
        file(storeFilePath)
      } else {
        file(storeFilePath)
      }
      storePassword = properties.getProperty("storePassword")
      keyAlias = properties.getProperty("keyAlias")
      keyPassword = properties.getProperty("keyPassword")
    }
  }

  testOptions {
    execution = "ANDROIDX_TEST_ORCHESTRATOR"

    unitTests {
      isIncludeAndroidResources = true
    }

    managedDevices {
      devices {
        create<ManagedVirtualDevice>("pixel3api30") {
          device = "Pixel 3"
          apiLevel = 30
          systemImageSource = "google-atd"
          require64Bit = false
        }
      }
    }
  }

  sourceSets {
    getByName("test") {
      java.srcDir("$projectDir/src/testShared")
    }

    getByName("androidTest") {
      java.srcDir("$projectDir/src/testShared")
    }
  }

  compileOptions {
    isCoreLibraryDesugaringEnabled = true
    sourceCompatibility = signalJavaVersion
    targetCompatibility = signalJavaVersion
  }

  packaging {
    jniLibs {
      excludes += setOf(
        "**/*.dylib",
        "**/*.dll"
      )
    }
    resources {
      excludes += setOf(
        "LICENSE.txt",
        "LICENSE",
        "NOTICE",
        "asm-license.txt",
        "META-INF/LICENSE",
        "META-INF/LICENSE.md",
        "META-INF/NOTICE",
        "META-INF/LICENSE-notice.md",
        "META-INF/proguard/androidx-annotations.pro",
        "**/*.dylib",
        "**/*.dll"
      )
    }
  }

  buildFeatures {
    buildConfig = true
    viewBinding = true
    compose = true
  }

  composeOptions {
    kotlinCompilerExtensionVersion = "1.5.4"
  }

  defaultConfig {
//    applicationId = "group.ba.voiceapp"
    applicationId = "com.baxs.bachat"
    versionCode = (canonicalVersionCode * maxHotfixVersions) + currentHotfixVersion
    versionName = canonicalVersionName
    minSdk = signalMinSdkVersion
    targetSdk = signalTargetSdkVersion
    vectorDrawables.useSupportLibrary = true
    project.ext.set("archivesBaseName", "Signal")
    manifestPlaceholders["mapsKey"] = "AIzaSyB9QhtCY97zGjWk5FY4mes1WeBixeNLaoA"
    buildConfigField("long", "BUILD_TIMESTAMP", getLastCommitTimestamp() + "L")
    buildConfigField("String", "GIT_HASH", "\"${getGitHash()}\"")

    buildConfigField("String", "SIGNAL_URL", "\"https://chat.ba-chat.com\"")
    buildConfigField("String", "STORAGE_URL", "\"https://storage.ba-chat.com\"")
    buildConfigField("String", "SIGNAL_CDN_URL", "\"https://cdn.ba-chat.com\"")
    buildConfigField("String", "SIGNAL_CDN2_URL", "\"https://cdn2.ba-chat.com\"")///--------
    buildConfigField("String", "SIGNAL_CDN3_URL", "\"https://cdn3.ba-chat.com\"")
    buildConfigField("String", "SIGNAL_CDSI_URL", "\"https://cdsi.ba-chat.com\"")
    buildConfigField("String", "SIGNAL_SERVICE_STATUS_URL", "\"uptime.ba-chat.com\"")
    buildConfigField("String", "SIGNAL_SVR2_URL", "\"https://svr2.ba-chat.com\"")

    buildConfigField("String", "SIGNAL_SFU_URL", "\"https://sfu.ba-chat.com\"")
    buildConfigField("String", "SIGNAL_STAGING_SFU_URL", "\"https://sfu.ba-chat.com\"")
    buildConfigField("String[]", "SIGNAL_SFU_INTERNAL_URLS", "new String[]{\"https://sfu.ba-chat.com\", \"https://sfu.ba-chat.com\", \"https://sfu.ba-chat.com\"}")


    buildConfigField("String[]", "SIGNAL_SFU_INTERNAL_NAMES", "new String[]{\"Test\", \"Staging\", \"Development\"}")
    buildConfigField("String", "CONTENT_PROXY_HOST", "\"contentproxy.signal.org\"")
    buildConfigField("int", "CONTENT_PROXY_PORT", "443")
    buildConfigField("String[]", "SIGNAL_SERVICE_IPS", rootProject.extra["service_ips"] as String)
    buildConfigField("String[]", "SIGNAL_STORAGE_IPS", rootProject.extra["storage_ips"] as String)
    buildConfigField("String[]", "SIGNAL_CDN_IPS", rootProject.extra["cdn_ips"] as String)
    buildConfigField("String[]", "SIGNAL_CDN2_IPS", rootProject.extra["cdn2_ips"] as String)
    buildConfigField("String[]", "SIGNAL_CDN3_IPS", rootProject.extra["cdn3_ips"] as String)
    buildConfigField("String[]", "SIGNAL_SFU_IPS", rootProject.extra["sfu_ips"] as String)
    buildConfigField("String[]", "SIGNAL_CONTENT_PROXY_IPS", rootProject.extra["content_proxy_ips"] as String)
    buildConfigField("String[]", "SIGNAL_CDSI_IPS", rootProject.extra["cdsi_ips"] as String)
    buildConfigField("String[]", "SIGNAL_SVR2_IPS", rootProject.extra["svr2_ips"] as String)
    buildConfigField("String", "SIGNAL_AGENT", "\"OWA\"")

    buildConfigField("String", "SVR2_MRENCLAVE_LEGACY_LEGACY", "\"b49a2d7aa6a92623713541be3342cc2432cbb4052a9ab83b50aef3375651e68f\"")
    buildConfigField("String", "SVR2_MRENCLAVE_LEGACY", "\"b49a2d7aa6a92623713541be3342cc2432cbb4052a9ab83b50aef3375651e68f\"")
    buildConfigField("String", "SVR2_MRENCLAVE", "\"b49a2d7aa6a92623713541be3342cc2432cbb4052a9ab83b50aef3375651e68f\"")
    buildConfigField("String", "UNIDENTIFIED_SENDER_TRUST_ROOT", "\"Bd8hujwt+PY1jMqO5xC/8pmIuxwzwuX7ZjHKoJ2BVL4g\"")
    buildConfigField("String", "ZKGROUP_SERVER_PUBLIC_PARAMS", "\"AOSwc07bu3ImyxbdBax3eJsIIsjzyXELmZpQj3IUp1wbcGL/eeUU2b3LuYJnA+jbXA5z/VYyAm3shM1Fd6NIkhVMzsF4vkKTvBAEjDpXuYR8cFz5YzNdYum1sOwMVVKedIzQAT9YRr+qHwVZ3bFJM69AifuC8MrbhzvWwHWEZ1dU0LRos1YtWCtXtW3w/KZuEqJWJvf9rA6y708DMt0swBE27QeWRdOJKhRnxhMj7R+6bEh92/jXjtZfY9awQo1mX+wSu4qvXDopKGubupTLBa+DDgs9VG7xCewJPzh/cM5jhN7AS8BN0nwXNwjh9UOR1twZDp0RZ+B6yDBxJbTVN1Wo8szJewewkZ6riE7dGEp3ypTnZ+8/JZoB8xKAvzMEYzaEc6CGGqqtx7O5Wty7GpnumYtBxotao2WHgqbYiB44ENPEFWwPg8XSqM6ZJW5cyi5XAD31ubQ49hQs1asJUUsoWHcru8VIBX9UvN128yoDQcNouTXzeVrK+c9VMtzkLtRNVVGtEzCcmGsSpCg0oMqvXAgZTXCT6KdEnO0CMGIJ5kINHo6yC/1AMu8E9pvDBlfLyt1PQh9up5UxDXgRZUTQDkrnv8eoRWb/wx8perlTQGZV7QmWNN7f3W2B9bKOL26Le/0MfTFue3VDqUN4uGqlzzm4bVwV2nKf4Z5gwlFK9gOn9k6UiZ9aN5/Fm7zXL9btLT79Ly2K3s52ZfiZKnoe5bPAULcJ1MRawuS8XFSPo9soklHqaAhDwqjsHs0RfaxOKAkAIV/4ZQP1Ty5BGRg7XP1ZuaHMNEzQFjBdHdcW5qGR6AJtUOKVJn7+SVErGnw1Tu5KbZ6V+sLc0QTrLzqecKgrb0q77eXCv6r7THP79imh/Turlk1rgRHyqgRiPQ==\"")
    buildConfigField("String", "GENERIC_SERVER_PUBLIC_PARAMS", "\"ACoSrbFfXNCwT2TOgSXKl+mqxApYYxFvA+fqP9TnhDZlMpxZfzVHKNaYn44P6lJWTT6YIzNHB1S1XeoxG8vT7iVU+AhVsESiC/wAxel3mz8QLsYYUu1WwhGdS9SiCrZbA1hdvWdPR9aevqDckr0reXY2b80Yvx2MXwanJZxty6MyTJhyzeQE62+clBZwPY4sTd/hwn+ye7V0h6yrZ2JXPSSGXKDJsozGVnOmi/JFtAyh0AK6ItRG94omkTid8zhxWSYN5bct4svdQOWhWOZoQT5/1NJOfTGUM5WfX4i+TvtL\"")
    buildConfigField("String", "BACKUP_SERVER_PUBLIC_PARAMS", "\"ALy0PQXREe4drGk4xClPewbfE3pnFLALZ4mJ7TUFj0lvLAVpI4yQChBt20gxD7PfYZvVK8sGzYQ4G8UhM7sJY3Gw2aNwg3IjheOzY5web/1nVmnxVdt2gIco3+fVBG8jfRotXjJ2IGP/x9ayTqCBeQEc0xGtQZioKZo2PtFIy3oUQgn02z4gpnIXcT5VJu2G9YYC3cfiycEMEd+AZyB22w3qb3YWr2LfDA/aXDcQE/pio4jju5JQQIA2W1QBKNNqDrrlG765zfVBQDFEr8ruAn+gE3QvVKi2k+pH6jl6vdYv\"")
    buildConfigField("String[]", "LANGUAGES", "new String[]{ ${languageList().map { "\"$it\"" }.joinToString(separator = ", ")} }")
    buildConfigField("int", "CANONICAL_VERSION_CODE", "$canonicalVersionCode")
    buildConfigField("String", "DEFAULT_CURRENCIES", "\"EUR,AUD,GBP,CAD,CNY\"")
    buildConfigField("String", "GIPHY_API_KEY", "\"3o6ZsYH6U6Eri53TXy\"")
    buildConfigField("String", "SIGNAL_CAPTCHA_URL", "\"https://captcha.ba-chat.com/registration/generate.html\"")
    buildConfigField("String", "RECAPTCHA_PROOF_URL", "\"https://captcha.ba-chat.com/challenge/generate.html\"")
    buildConfigField("org.signal.libsignal.net.Network.Environment", "LIBSIGNAL_NET_ENV", "org.signal.libsignal.net.Network.Environment.PRODUCTION")
    buildConfigField("int", "LIBSIGNAL_LOG_LEVEL", "org.signal.libsignal.protocol.logging.SignalProtocolLogger.INFO")

    buildConfigField("String", "BUILD_DISTRIBUTION_TYPE", "\"unset\"")
    buildConfigField("String", "BUILD_ENVIRONMENT_TYPE", "\"unset\"")
    buildConfigField("String", "BUILD_VARIANT_TYPE", "\"unset\"")
//    buildConfigField("String", "BADGE_STATIC_ROOT", "\"https://updates2.signal.org/static/badges/\"")
    buildConfigField("String", "BADGE_STATIC_ROOT", "\"https://updates2.ba-chat.com/static/badges/\"")
    buildConfigField("String", "STRIPE_BASE_URL", "\"https://api.stripe.com/v1\"")
    buildConfigField("String", "STRIPE_PUBLISHABLE_KEY", "\"pk_live_6cmGZopuTsV8novGgJJW9JpC00vLIgtQ1D\"")
    buildConfigField("boolean", "TRACING_ENABLED", "false")
    buildConfigField("boolean", "MESSAGE_BACKUP_RESTORE_ENABLED", "false")
    ndk {
      abiFilters += listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
    }
    resourceConfigurations += listOf()

    splits {
      abi {
        isEnable = !project.hasProperty("generateBaselineProfile")
        reset()
        include("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
        isUniversalApk = true
      }
    }

    testInstrumentationRunner = "org.thoughtcrime.securesms.testing.SignalTestRunner"
    testInstrumentationRunnerArguments["clearPackageData"] = "true"
  }

  buildTypes {
    getByName("debug") {
      if (keystores["debug"] != null) {
        signingConfig = signingConfigs["debug"]
      }
      isDefault = true
      isMinifyEnabled = false
      proguardFiles(
        getDefaultProguardFile("proguard-android.txt"),
        "proguard/proguard-firebase-messaging.pro",
        "proguard/proguard-google-play-services.pro",
        "proguard/proguard-jackson.pro",
        "proguard/proguard-sqlite.pro",
        "proguard/proguard-appcompat-v7.pro",
        "proguard/proguard-square-okhttp.pro",
        "proguard/proguard-square-okio.pro",
        "proguard/proguard-rounded-image-view.pro",
        "proguard/proguard-glide.pro",
        "proguard/proguard-shortcutbadger.pro",
        "proguard/proguard-retrofit.pro",
        "proguard/proguard-klinker.pro",
        "proguard/proguard-mobilecoin.pro",
        "proguard/proguard-retrolambda.pro",
        "proguard/proguard-okhttp.pro",
        "proguard/proguard.cfg",

        "proguard/proguard-ez-vcard.pro"

      )
      testProguardFiles(
        "proguard/proguard-automation.pro",
        "proguard/proguard.cfg"
      )

      manifestPlaceholders["mapsKey"] = getMapsKey()

      buildConfigField("String", "BUILD_VARIANT_TYPE", "\"Debug\"")
    }

    getByName("release") {
      if (keystores["release"] != null) {
        signingConfig = signingConfigs["release"]
      }
      isMinifyEnabled = true
      proguardFiles(*buildTypes["debug"].proguardFiles.toTypedArray())
      buildConfigField("String", "BUILD_VARIANT_TYPE", "\"Release\"")
    }

    create("instrumentation") {
      initWith(getByName("debug"))
      isDefault = false
      isMinifyEnabled = false
      matchingFallbacks += "debug"
      applicationIdSuffix = ".instrumentation"

      buildConfigField("String", "BUILD_VARIANT_TYPE", "\"Instrumentation\"")
      buildConfigField("String", "STRIPE_BASE_URL", "\"http://127.0.0.1:8080/stripe\"")
    }

    create("spinner") {
      initWith(getByName("debug"))
      isDefault = false
      isMinifyEnabled = false
      matchingFallbacks += "debug"
      buildConfigField("String", "BUILD_VARIANT_TYPE", "\"Spinner\"")
    }

    create("perf") {
      initWith(getByName("debug"))
      isDefault = false
      isDebuggable = false
      isMinifyEnabled = true
      matchingFallbacks += "debug"
      buildConfigField("String", "BUILD_VARIANT_TYPE", "\"Perf\"")
      buildConfigField("boolean", "TRACING_ENABLED", "true")
    }

    create("benchmark") {
      initWith(getByName("debug"))
      isDefault = false
      isDebuggable = false
      isMinifyEnabled = true
      matchingFallbacks += "debug"
      buildConfigField("String", "BUILD_VARIANT_TYPE", "\"Benchmark\"")
      buildConfigField("boolean", "TRACING_ENABLED", "true")
    }

    create("canary") {
      initWith(getByName("debug"))
      isDefault = false
      isMinifyEnabled = false
      matchingFallbacks += "debug"
      buildConfigField("String", "BUILD_VARIANT_TYPE", "\"Canary\"")
    }
  }

  productFlavors {
    create("play") {
      dimension = "distribution"
      isDefault = true
      buildConfigField("boolean", "MANAGES_APP_UPDATES", "false")
      buildConfigField("String", "APK_UPDATE_MANIFEST_URL", "null")
      buildConfigField("String", "BUILD_DISTRIBUTION_TYPE", "\"play\"")
    }

    create("website") {
      dimension = "distribution"
      buildConfigField("boolean", "MANAGES_APP_UPDATES", "true")
      buildConfigField("String", "APK_UPDATE_MANIFEST_URL", "\"https://updates.ba-chat.com/android/latest.json\"")
      buildConfigField("String", "BUILD_DISTRIBUTION_TYPE", "\"website\"")
    }

    create("nightly") {
      val apkUpdateManifestUrl = if (file("${project.rootDir}/nightly-url.txt").exists()) {
        file("${project.rootDir}/nightly-url.txt").readText().trim()
      } else {
        "<unset>"
      }

      dimension = "distribution"
      versionNameSuffix = "-nightly-untagged-${getDateSuffix()}"
      buildConfigField("boolean", "MANAGES_APP_UPDATES", "true")
      buildConfigField("String", "APK_UPDATE_MANIFEST_URL", "\"${apkUpdateManifestUrl}\"")
      buildConfigField("String", "BUILD_DISTRIBUTION_TYPE", "\"nightly\"")
      buildConfigField("boolean", "MESSAGE_BACKUP_RESTORE_ENABLED", "true")
    }

    create("prod") {
      dimension = "environment"

      isDefault = true

      buildConfigField("String", "MOBILE_COIN_ENVIRONMENT", "\"mainnet\"")
      buildConfigField("String", "BUILD_ENVIRONMENT_TYPE", "\"Prod\"")
    }

    create("staging") {
      dimension = "environment"
//      applicationIdSuffix = ".staging"
      buildConfigField("String", "SIGNAL_URL", "\"https://chat.imba-test.com\"")
      buildConfigField("String", "STORAGE_URL", "\"https://storage.imba-test.com\"")
      buildConfigField("String", "SIGNAL_CDN_URL", "\"https://cdn.imba-test.com\"")
      buildConfigField("String", "SIGNAL_CDN2_URL", "\"https://cdn2.imba-test.com\"")
      buildConfigField("String", "SIGNAL_CDN3_URL", "\"https://cdn3.imba-test.com\"")
      buildConfigField("String", "SIGNAL_CDSI_URL", "\"https://cdsi.imba-test.com\"")
      buildConfigField("String", "SIGNAL_SVR2_URL", "\"https://svr2.imba-test.com\"")
      buildConfigField("String", "SIGNAL_SFU_URL", "\"https://sfu.imba-test.com\"")
      buildConfigField("String", "SIGNAL_STAGING_SFU_URL", "\"https://sfu.imba-test.com\"")
      buildConfigField("String[]", "SIGNAL_SFU_INTERNAL_URLS", "new String[]{\"https://sfu.imba-test.com\", \"https://sfu.imba-test.com\", \"https://sfu.imba-test.com\"}")


      buildConfigField("String", "SVR2_MRENCLAVE", "\"97858810df5aaecaa10048fc56931badce985e5b260ca623993a363b8f5d760e\"")
      buildConfigField("String", "SVR2_MRENCLAVE_LEGACY", "\"b49a2d7aa6a92623713541be3342cc2432cbb4052a9ab83b50aef3375651e68f\"")
      buildConfigField("String", "SVR2_MRENCLAVE_LEGACY_LEGACY", "\"b49a2d7aa6a92623713541be3342cc2432cbb4052a9ab83b50aef3375651e68f\"")

      buildConfigField("String", "UNIDENTIFIED_SENDER_TRUST_ROOT", "\"BX4nQt7OxWnkqgcYeYyIA1XX43ZfPTEfusNoYTV5NJlj\"")
      buildConfigField("String", "ZKGROUP_SERVER_PUBLIC_PARAMS", "\"AHbJ9KmFfwzDoqJhN6Vouyqdv5B9jqpZZBC1Nj4CRPdRur1cvdvE38qtK+a7fMy/m3SR0oK3PJ5UozxVvuUE6zQcQ50e8e/1dVceVfh80g1WPRpQu5c6MJnrKDkTPifMQ7wd87L7PmgijxKaDD+zz3k9IRLtdrTjCoimFtvt7uoZpNB2ufr6vr2b7VgOEvD9BqPtPErEw9LejE6sHFDhfy/anH9IU7s/Sc4veQBbYgJlaGY7wewt1xSC5k3uxnyQVSYjSh0aYbaSas9LquAFb0fLezOkLZLoFTvj/CbQ6to0dikNvCwVwCQOBQ5sfc8sPwT0Sik59lej6g8NU54DI3XeTjFXOSPpH0XGIVG5jHrIEKCjkc74RqsLaG846m3/cqQm3nHhVffEMAVx6yXAQU9sYiDZpYBJS2R1XiqGWtl2HNfBJwaKvvJ96SOIYOhMNMYm0SU023g2M4/RVhL8WQqyPlzyoZfTk2OvFCRcweQ14GlTzzBJLdYXUEh7Gi/KFdbjaA9Lg8bxlA8OzeyarzAenNU/CrIHCqLNU5re8l08+t7CXF8KftkwdcWjkKIfKDKHrZTNNBRxz1cRcPKhQzgC5I9YW2WsTaeCkMExzOxMA8HvzQv9mZDuNDS7Re8lZ3rGkzyQpKC8QBh7vlVd/qy6JZVCeAJxWO/HRTtj9GsbGt90ioDy4n3byEBg9QXksAyCYTdQvIv3nzzVcpcrZwLsz+z83QBpsdmqPfgwb9BYdZqPt4bSheUZZRU87r/IL+xG2N6QZPFS8vAknjJ+XUk8NvhXU53oT02Omq9EOrtc6LVNgG9BfT/c/lo+WTJfE5WUdGE7Xp13Qnss9Ej8PTiqzabyS+fu98QPcqoNxIyKv6bvcw3Uggofe0tNaumuFg==\"")
      buildConfigField("String", "GENERIC_SERVER_PUBLIC_PARAMS", "\"ADKN7E6cEU87eJMvv69wLvPBwFKJq3JZkAKxWahgwd4jAiHjn31mbvn3eUhAKN2W5ub2C+wj6L6EIr2XZNvoIwO47X5IEjEnCpRrMofYfriKQIDIbFFSyft7PaUT50GbHHJ+Fw+NMVCz1VxNb8HBjIgtqqqj2+OUbUBERqL52mok7qajiPtrwMeTA0iCXDAxUPyGYFfWoa+yrfWJdUJ+byd4lIwfIQnFuF07Fo5VIkK17+QTPMUeyMNB+BZRlCs9asAsM+eOv3H4YOXomc2/Dm0YpwcoThQ5MOo2gJI+tcU/\"")
      buildConfigField("String", "BACKUP_SERVER_PUBLIC_PARAMS", "\"AIIe881NQpY7o8wTPlTiIlMjUX8gvzqp4MVQb63dvX4JTLDLty9gz3EirUPkgSecfSjwY/eNZwDIcTc5gYzMBSj46Roe1r5wRp6Qemvkade9mXY/VMi4bNZgBX/+k11OU+JbPl5ADt2FCLI0L6MRyFr3ZLesYdEEaztCkxkISChj8g8BhFaRTjb+g4CHrm/d3DWuyyOMJ3VNtt2/qeWsqn/SpVpiHC0QM8sS83b5U4IAe7MiwZ+2nCPdAMqwrZwZcO6l+IRes7Za3SyYEABV+0GhmlmpQtHdcCEiWFb4FvtK\"")
      buildConfigField("String", "MOBILE_COIN_ENVIRONMENT", "\"testnet\"")
      buildConfigField("String", "SIGNAL_CAPTCHA_URL", "\"https://captcha.imba-test.com/registration/generate.html\"")
      buildConfigField("String", "RECAPTCHA_PROOF_URL", "\"https://captcha.imba-test.com/challenge/generate.html\"")
      buildConfigField("org.signal.libsignal.net.Network.Environment", "LIBSIGNAL_NET_ENV", "org.signal.libsignal.net.Network.Environment.STAGING")
      buildConfigField("int", "LIBSIGNAL_LOG_LEVEL", "org.signal.libsignal.protocol.logging.SignalProtocolLogger.DEBUG")
      buildConfigField("String", "BUILD_ENVIRONMENT_TYPE", "\"Staging\"")
      buildConfigField("String", "STRIPE_PUBLISHABLE_KEY", "\"pk_test_sngOd8FnXNkpce9nPXawKrJD00kIDngZkD\"")
      buildConfigField("boolean", "MESSAGE_BACKUP_RESTORE_ENABLED", "true")
      buildConfigField("String", "BADGE_STATIC_ROOT", "\"https://updates2.imba-test.com/static/badges/\"")
    }

    create("backup") {
      initWith(getByName("staging"))

      dimension = "environment"

      applicationIdSuffix = ".backup"

      buildConfigField("boolean", "MANAGES_APP_UPDATES", "true")
      buildConfigField("String", "BUILD_ENVIRONMENT_TYPE", "\"Backup\"")
      buildConfigField("boolean", "MESSAGE_BACKUP_RESTORE_ENABLED", "true")
    }
  }

  lint {
    abortOnError = true
    baseline = file("lint-baseline.xml")
    checkReleaseBuilds = false
    ignoreWarnings = true
    quiet = true
    disable += "LintError"
  }

  applicationVariants.all {
    outputs
      .map { it as com.android.build.gradle.internal.api.ApkVariantOutputImpl }
      .forEach { output ->
        if (output.baseName.contains("nightly")) {
          var tag = getCurrentGitTag()
          if (!tag.isNullOrEmpty()) {
            if (tag.startsWith("v")) {
              tag = tag.substring(1)
            }
            output.versionNameOverride = tag
            output.outputFileName = output.outputFileName.replace(".apk", "-${output.versionNameOverride}.apk")
          } else {
            output.outputFileName = output.outputFileName.replace(".apk", "-$versionName.apk")
          }
        } else {
          output.outputFileName = output.outputFileName.replace(".apk", "-$versionName.apk")

          if (currentHotfixVersion >= maxHotfixVersions) {
            throw AssertionError("Hotfix version is too large!")
          }
        }
        output.outputFileName = output.outputFileName.replace("Signal", "BA")
      }
  }

  androidComponents {
    beforeVariants { variant ->
      variant.enable = variant.name in selectableVariants
    }
    onVariants { variant ->
      // Include the test-only library on debug builds.
      if (variant.buildType != "instrumentation") {
        variant.packaging.jniLibs.excludes.add("**/libsignal_jni_testing.so")
      }
    }
  }

  val releaseDir = "$projectDir/src/release/java"
  val debugDir = "$projectDir/src/debug/java"

  android.buildTypes.configureEach {
    val path = if (name == "release") releaseDir else debugDir
    sourceSets.named(name) {
      java.srcDir(path)
    }
  }
}

dependencies {
  lintChecks(project(":lintchecks"))
  ktlintRuleset(libs.ktlint.twitter.compose)
  coreLibraryDesugaring(libs.android.tools.desugar)

  implementation(project(":libsignal-service"))
  implementation(project(":paging"))
  implementation(project(":core-util"))
  implementation(project(":glide-config"))
  implementation(project(":video"))
  implementation(project(":device-transfer"))
  implementation(project(":image-editor"))
  implementation(project(":donations"))
  implementation(project(":debuglogs-viewer"))
  implementation(project(":contacts"))
  implementation(project(":qr"))
  implementation(project(":sticky-header-grid"))
  implementation(project(":photoview"))
  implementation(project(":core-ui"))

  implementation(libs.androidx.fragment.ktx)
  implementation(libs.androidx.fragment.compose)
  implementation(libs.androidx.appcompat) {
    version {
      strictly("1.6.1")
    }
  }
  implementation(libs.androidx.window.window)
  implementation(libs.androidx.window.java)
  implementation(libs.androidx.recyclerview)
  implementation(libs.material.material)
  implementation(libs.androidx.legacy.support)
  implementation(libs.androidx.preference)
  implementation(libs.androidx.legacy.preference)
  implementation(libs.androidx.gridlayout)
  implementation(libs.androidx.exifinterface)
  implementation(libs.androidx.compose.rxjava3)
  implementation(libs.androidx.compose.runtime.livedata)
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.constraintlayout)
  implementation(libs.androidx.navigation.fragment.ktx)
  implementation(libs.androidx.navigation.ui.ktx)
  implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.lifecycle.viewmodel.ktx)
  implementation(libs.androidx.lifecycle.livedata.ktx)
  implementation(libs.androidx.lifecycle.process)
  implementation(libs.androidx.lifecycle.viewmodel.savedstate)
  implementation(libs.androidx.lifecycle.common.java8)
  implementation(libs.androidx.lifecycle.reactivestreams.ktx)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.camera.core)
  implementation(libs.androidx.camera.camera2)
  implementation(libs.androidx.camera.extensions)
  implementation(libs.androidx.camera.lifecycle)
  implementation(libs.androidx.camera.view)
  implementation(libs.androidx.concurrent.futures)
  implementation(libs.androidx.autofill)
  implementation(libs.androidx.biometric)
  implementation(libs.androidx.sharetarget)
  implementation(libs.androidx.profileinstaller)
  implementation(libs.androidx.asynclayoutinflater)
  implementation(libs.androidx.asynclayoutinflater.appcompat)
  implementation(libs.androidx.emoji2)
  implementation(libs.firebase.messaging) {
    exclude(group = "com.google.firebase", module = "firebase-core")
    exclude(group = "com.google.firebase", module = "firebase-analytics")
  }
  implementation(libs.google.play.services.maps)
  implementation(libs.google.play.services.auth)
  implementation(libs.google.signin)
  implementation(libs.bundles.media3)
  implementation(libs.conscrypt.android)
  implementation(libs.signal.aesgcmprovider)
  implementation(libs.libsignal.android)
  implementation(libs.mobilecoin)
  implementation(libs.signal.ringrtc)
  implementation(libs.leolin.shortcutbadger)
  implementation(libs.emilsjolander.stickylistheaders)
  implementation(libs.glide.glide)
  implementation(libs.roundedimageview)
  implementation(libs.materialish.progress)
  implementation(libs.greenrobot.eventbus)
  implementation(libs.google.zxing.android.integration)
  implementation(libs.google.zxing.core)
  implementation(libs.google.flexbox)
  implementation(libs.subsampling.scale.image.view) {
    exclude(group = "com.android.support", module = "support-annotations")
  }
  implementation(libs.android.tooltips) {
    exclude(group = "com.android.support", module = "appcompat-v7")
  }
  implementation(libs.stream)
  implementation(libs.lottie)
  implementation(libs.lottie.compose)
  implementation(libs.signal.android.database.sqlcipher)
  implementation(libs.androidx.sqlite)
  testImplementation(libs.androidx.sqlite.framework)
  implementation(libs.google.ez.vcard) {
    exclude(group = "com.fasterxml.jackson.core")
    exclude(group = "org.freemarker")
  }
  implementation(libs.dnsjava)
  implementation(libs.kotlinx.collections.immutable)
  implementation(libs.accompanist.permissions)
  implementation(libs.accompanist.drawablepainter)
  implementation(libs.kotlin.stdlib.jdk8)
  implementation(libs.kotlin.reflect)
  implementation(libs.kotlinx.coroutines.play.services)
  implementation(libs.kotlinx.coroutines.rx3)
  implementation(libs.jackson.module.kotlin)
  implementation(libs.rxjava3.rxandroid)
  implementation(libs.rxjava3.rxkotlin)
  implementation(libs.rxdogtag)
  implementation(libs.androidx.credentials)
  implementation(libs.androidx.credentials.compat)

  "playImplementation"(project(":billing"))
  "nightlyImplementation"(project(":billing"))

  "spinnerImplementation"(project(":spinner"))

  "canaryImplementation"(libs.square.leakcanary)

  "instrumentationImplementation"(libs.androidx.fragment.testing) {
    exclude(group = "androidx.test", module = "core")
  }

  testImplementation(testLibs.junit.junit)
  testImplementation(testLibs.assertk)
  testImplementation(testLibs.androidx.test.core)
  testImplementation(testLibs.robolectric.robolectric) {
    exclude(group = "com.google.protobuf", module = "protobuf-java")
  }
  testImplementation(testLibs.bouncycastle.bcprov.jdk15on) {
    version {
      strictly("1.70")
    }
  }
  testImplementation(testLibs.bouncycastle.bcpkix.jdk15on) {
    version {
      strictly("1.70")
    }
  }
  testImplementation(testLibs.conscrypt.openjdk.uber)
  testImplementation(testLibs.mockk)
  testImplementation(testFixtures(project(":libsignal-service")))
  testImplementation(testLibs.espresso.core)
  testImplementation(testLibs.kotlinx.coroutines.test)

  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(testLibs.androidx.test.ext.junit)
  androidTestImplementation(testLibs.espresso.core)
  androidTestImplementation(testLibs.androidx.test.core)
  androidTestImplementation(testLibs.androidx.test.core.ktx)
  androidTestImplementation(testLibs.androidx.test.ext.junit.ktx)
  androidTestImplementation(testLibs.assertk)
  androidTestImplementation(testLibs.mockk.android)
  androidTestImplementation(testLibs.square.okhttp.mockserver)
  androidTestImplementation(testLibs.diff.utils)

  androidTestUtil(testLibs.androidx.test.orchestrator)
}

fun assertIsGitRepo() {
  if (!file("${project.rootDir}/.git").exists()) {
    throw IllegalStateException("Must be a git repository to guarantee reproducible builds! (git hash is part of APK)")
  }
}

fun getLastCommitTimestamp(): String {
  assertIsGitRepo()

  return providers.exec {
    commandLine("git", "log", "-1", "--pretty=format:%ct")
  }.standardOutput.asText.get() + "000"
}

fun getGitHash(): String {
  assertIsGitRepo()

  return providers.exec {
    commandLine("git", "rev-parse", "HEAD")
  }.standardOutput.asText.get().trim().substring(0, 12)
}

fun getCurrentGitTag(): String? {
  assertIsGitRepo()

  val output = providers.exec {
    commandLine("git", "tag", "--points-at", "HEAD")
  }.standardOutput.asText.get().trim()

  return if (output.isNotEmpty()) {
    val tags = output.split("\n").toList()
    tags.firstOrNull { it.contains("nightly") } ?: tags[0]
  } else {
    null
  }
}

tasks.withType<Test>().configureEach {
  testLogging {
    events("failed")
    exceptionFormat = TestExceptionFormat.FULL
    showCauses = true
    showExceptions = true
    showStackTraces = true
  }
}

gradle.taskGraph.whenReady {
  if (gradle.startParameter.taskNames.any { it.contains("nightly", ignoreCase = true) }) {
    if (!file("${project.rootDir}/nightly-url.txt").exists()) {
      throw GradleException("Missing required file: nightly-url.txt")
    }
  }
}

fun loadKeystoreProperties(filename: String): Properties? {
  val keystorePropertiesFile = if (File(filename).isAbsolute) {
    file(filename)
  } else {
    file("${project.rootDir}/$filename")
  }

  return if (keystorePropertiesFile.exists()) {
    val keystoreProperties = Properties()
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
    keystoreProperties
  } else {
    null
  }
}

fun ensureDebugKeystore() {
  val keystoreFile = file("${project.rootDir}/debug.keystore")

  if (!keystoreFile.exists()) {
    println("Debug keystore not found, creating one at: ${keystoreFile.absolutePath}")

    try {
      val result = providers.exec {
        commandLine(
          "keytool", "-genkey", "-v",
          "-keystore", keystoreFile.absolutePath,
          "-alias", "androiddebugkey",
          "-keyalg", "RSA",
          "-keysize", "2048",
          "-validity", "10000",
          "-storepass", "android",
          "-keypass", "android",
          "-dname", "CN=Android Debug,O=Android,C=US"
        )
        isIgnoreExitValue = true
      }.result

      if (result.get().exitValue == 0) {
        println("Debug keystore created successfully!")
      } else {
        println("Warning: Failed to create debug keystore automatically")
        println("Please run the following command manually:")
        println("keytool -genkey -v -keystore \"${keystoreFile.absolutePath}\" -alias androiddebugkey -keyalg RSA -keysize 2048 -validity 10000 -storepass android -keypass android -dname \"CN=Android Debug,O=Android,C=US\"")
      }
    } catch (e: Exception) {
      println("Warning: Could not create debug keystore automatically: ${e.message}")
      println("Please run the following command manually:")
      println("keytool -genkey -v -keystore \"${keystoreFile.absolutePath}\" -alias androiddebugkey -keyalg RSA -keysize 2048 -validity 10000 -storepass android -keypass android -dname \"CN=Android Debug,O=Android,C=US\"")
    }
  } else {
    println("Using existing debug keystore at: ${keystoreFile.absolutePath}")
  }
}

// Ensure debug keystore exists before loading properties
// ensureDebugKeystore()

fun getDateSuffix(): String {
  return SimpleDateFormat("yyyy-MM-dd-HH:mm").format(Date())
}

fun getMapsKey(): String {
  val mapKey = file("${project.rootDir}/maps.key")

  return if (mapKey.exists()) {
    mapKey.readLines()[0]
  } else {
    "AIzaSyB9QhtCY97zGjWk5FY4mes1WeBixeNLaoA"
  }
}

fun Project.languageList(): List<String> {
  // In API 35, language codes for Hebrew and Indonesian now use the ISO 639-1 code ("he" and "id").
  // However, the value resources still only support the outdated code ("iw" and "in") so we have
  // to manually indicate that we support these languages.
  val updatedLanguageCodes = listOf("he", "id")

  return fileTree("src/main/res") { include("**/strings.xml") }
    .map { stringFile -> stringFile.parentFile.name }
    .map { valuesFolderName -> valuesFolderName.replace("values-", "") }
    .filter { valuesFolderName -> valuesFolderName != "values" }
    .map { languageCode -> languageCode.replace("-r", "_") }
    .distinct()
    .sorted() + updatedLanguageCodes + "en"
}

fun String.capitalize(): String {
  return this.replaceFirstChar { it.uppercase() }
}
