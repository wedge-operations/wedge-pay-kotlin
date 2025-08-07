# Wedge Onboarding SDK

A lightweight Android SDK for integrating Wedge Onboarding into your Android applications.

## Features

- 🔐 Secure API key-based authentication
- 🌐 WebView-based onboarding experience
- 📱 Full-page integration (no bottom sheet)
- 🎯 Simple callback-based API
- 🔄 Support for sandbox and production environments

## Installation

### JitPack (Recommended)

1. **Add JitPack repository** to your `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

2. **Add the dependency** to your `app/build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.github.yourusername:wedge-sdk:1.0.0")
}
```

**Note**: Replace `yourusername` with your actual GitHub username and `1.0.0` with the version you want to use (can be a tag, commit hash, or branch name).

### Version Options

You can use different version formats:

- **Release tag**: `com.github.yourusername:wedge-sdk:1.0.0`
- **Commit hash**: `com.github.yourusername:wedge-sdk:abc1234`
- **Branch name**: `com.github.yourusername:wedge-sdk:main-SNAPSHOT`

## Usage

### Basic Integration

1. **Initialize the SDK** in your activity:

```kotlin
import com.wedge.wedgesdk.sdk.OnboardingCallback
import com.wedge.wedgesdk.sdk.OnboardingSDK

class MainActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Your UI setup here
        
        startOnboardingButton.setOnClickListener {
            val apiKey = "your-api-key-here"
            val environment = "sandbox" // or "production"
            
            OnboardingSDK.startOnboarding(
                activity = this@MainActivity,
                apiKey = apiKey,
                environment = environment,
                callback = object : OnboardingCallback {
                    override fun onSuccess(data: String) {
                        // Handle successful onboarding completion
                        Log.d("Onboarding", "Success: $data")
                    }
                    
                    override fun onExit(reason: String) {
                        // Handle user exit
                        Log.d("Onboarding", "Exit: $reason")
                    }
                    
                    override fun onError(error: String) {
                        // Handle errors
                        Log.e("Onboarding", "Error: $error")
                    }
                }
            )
        }
    }
}
```

### Callback Handling

The SDK provides three callback methods:

- `onSuccess(data: String)`: Called when onboarding is completed successfully
- `onExit(reason: String)`: Called when the user exits the onboarding process
- `onError(error: String)`: Called when an error occurs during onboarding

## Requirements

- Android API level 24+ (Android 7.0+)
- Kotlin 1.8+
- AndroidX

## Permissions

The SDK requires the following permission:

```xml
<uses-permission android:name="android.permission.INTERNET" />
```

## ProGuard/R8

If you're using ProGuard or R8, add the following rules to your `proguard-rules.pro`:

```proguard
# Wedge SDK
-keep class com.wedge.wedgesdk.** { *; }
-keepclassmembers class com.wedge.wedgesdk.** { *; }
```

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Support

- GitHub Issues: [Create an issue](https://github.com/yourusername/wedge-sdk/issues) 