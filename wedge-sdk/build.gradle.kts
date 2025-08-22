plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    id("maven-publish")
}

android {
    namespace = "com.wedge.wedgesdk"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions { jvmTarget = "11" }

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])

                groupId = "com.wedge-can"
                artifactId = "wedge-sdk"
                version = "1.0.0"

                pom {
                    name.set("Wedge SDK")
                    description.set("Android SDK for Wedge onboarding")
                    url.set("https://github.com/wedge-operations/wedge-pay-kotlin")
                    licenses {
                        license {
                            name.set("MIT")
                            url.set("https://opensource.org/licenses/MIT")
                        }
                    }
                    developers {
                        developer {
                            id.set("wedge-operations")
                            name.set("Wedge Operations")
                            email.set("support@wedge.com")
                        }
                    }
                    scm {
                        connection.set("scm:git:git://github.com/wedge-operations/wedge-pay-kotlin.git")
                        developerConnection.set("scm:git:ssh://git@github.com:wedge-operations/wedge-pay-kotlin.git")
                        url.set("https://github.com/wedge-operations/wedge-pay-kotlin")
                    }
                }
            }
        }
        repositories {
            mavenLocal()
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
