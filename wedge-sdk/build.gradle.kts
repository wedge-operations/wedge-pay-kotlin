import java.io.File
import java.security.MessageDigest
import org.gradle.api.tasks.bundling.Zip

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    id("maven-publish")
    id("signing")
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
                version = "1.1.0"

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

    signing {
        useGpgCmd()
        sign(publishing.publications["release"])
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

val groupIdForZip = "com.wedge-can"
val artifactIdForZip = "wedge-sdk"
val versionForZip = "1.1.0"

tasks.register("checksumsMavenLocal") {
    dependsOn("publishReleasePublicationToMavenLocal")
    doLast {
        val repoRoot = System.getProperty("user.home") + "/.m2/repository"
        val groupPath = groupIdForZip.replace('.', '/')
        val dir = file("$repoRoot/$groupPath/$artifactIdForZip/$versionForZip")

        fun hash(file: File, algo: String): String {
            val md = MessageDigest.getInstance(algo)
            md.update(file.readBytes())
            return md.digest().joinToString("") { "%02x".format(it) }
        }

        val names = listOf(
            "$artifactIdForZip-$versionForZip.aar",
            "$artifactIdForZip-$versionForZip.jar",
            "$artifactIdForZip-$versionForZip-sources.jar",
            "$artifactIdForZip-$versionForZip-javadoc.jar",
            "$artifactIdForZip-$versionForZip.pom",
            "$artifactIdForZip-$versionForZip.module"
        )

        val files = names.map { File(dir, it) }.filter { it.exists() }

        files.forEach { f ->
            File(dir, "${f.name}.sha1").writeText(hash(f, "SHA-1"))
            File(dir, "${f.name}.md5").writeText(hash(f, "MD5"))
        }
        println("Checksums written in: $dir")
    }
}

tasks.register<Zip>("zipForCentral") {
    dependsOn("checksumsMavenLocal")
    archiveFileName.set("$artifactIdForZip-$versionForZip-bundle.zip")

    val repoRoot = System.getProperty("user.home") + "/.m2/repository"
    val groupPath = groupIdForZip.replace('.', '/')

    from(repoRoot) {
        include("$groupPath/$artifactIdForZip/$versionForZip/**")
    }

    destinationDirectory.set(layout.projectDirectory.asFile) // ZIP en el root del módulo
    doLast { println("ZIP ready: ${archiveFile.get().asFile.absolutePath}") }
}
