import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation

plugins {
    alias(libs.plugins.kotlin.multiplatform) apply true
    alias(libs.plugins.maven.publish) apply true
}

@OptIn(ExperimentalAbiValidation::class, ExperimentalWasmDsl::class)
kotlin {
    abiValidation {
        enabled = true
        // work around https://youtrack.jetbrains.com/issue/KT-78525
        tasks.named("check") {
            dependsOn(legacyDump.legacyCheckTaskProvider)
        }
    }

    androidNativeArm32()
    androidNativeArm64()
    androidNativeX64()
    androidNativeX86()

    iosArm64()
    iosSimulatorArm64()
    iosX64()

    js {
        nodejs {
            testTask {
                useMocha {
                    timeout = "5s"
                }
            }
        }
    }

    jvm {
        compilerOptions { jvmTarget.set(JvmTarget.JVM_1_8) }
        testRuns["test"].executionTask.configure { useJUnit() }
    }

    linuxArm64()
    linuxX64()

    macosArm64()
    macosX64()

    mingwX64()

    tvosArm64()
    tvosSimulatorArm64()
    tvosX64()

    wasmJs().nodejs()
    wasmWasi().nodejs()

    watchosArm32()
    watchosArm64()
    watchosDeviceArm64()
    watchosSimulatorArm64()
    watchosX64()

    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.kotlinx.coroutines.core)
            }
        }
        commonTest {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.kotlinx.coroutines.test)
            }
            languageSettings.optIn("kotlinx.coroutines.ExperimentalCoroutinesApi")
        }
        jvmTest {
            languageSettings.optIn("kotlinx.coroutines.ExperimentalCoroutinesApi")
        }
    }
}

// ---- publishing --- //

group = "at.florianschuster.store"
version = System.getenv("storeVersionTag") ?: "local"

mavenPublishing {
    publishToMavenCentral(automaticRelease = true)
    if (version != "local") signAllPublications()
    coordinates(group.toString(), "store", version.toString())
    pom {
        name = "store"
        description = "an opinionated kotlin coroutines multiplatform library to manage state"
        inceptionYear = "2025"
        url = "https://github.com/floschu/store"
        licenses {
            license {
                name = "The Apache Software License, Version 2.0"
                url = "http://www.apache.org/licenses/LICENSE-2.0.txt"
                distribution = "http://www.apache.org/licenses/LICENSE-2.0.txt"
            }
        }
        developers {
            developer {
                id = "floschu"
                name = "Florian Schuster"
                url = "https://github.com/floschu"
            }
        }
        scm {
            url = "https://github.com/floschu/store"
            connection = "scm:git@github.com:floschu/store.git"
            developerConnection = "scm:git@github.com:floschu/store.git"
        }
        issueManagement {
            system = "GitHub"
            url = "https://github.com/floschu/store/issues"
        }
    }
}

// ---- end publishing --- //
