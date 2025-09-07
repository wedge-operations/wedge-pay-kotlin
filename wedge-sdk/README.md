# Wedge Onboarding SDK for Android

A powerful Android SDK for integrating Wedge's onboarding and funding flows into your mobile applications.

## 🚀 Features

- **Seamless Integration**: Easy-to-use SDK with minimal setup
- **Multiple Flow Types**: Support for both onboarding and funding flows
- **Environment Support**: Sandbox, production, and integration environments
- **Callback System**: Comprehensive event handling for success, exit, and error scenarios
- **WebView Integration**: Built-in WebView handling with JavaScript bridge
- **Edge-to-Edge Support**: Modern Android UI with proper system bar handling

## 📱 Installation

### Maven

Add the following to your `pom.xml`:

```xml
<dependency>
    <groupId>com.wedge</groupId>
    <artifactId>onboarding-sdk-android</artifactId>
    <version>1.1.0</version>
</dependency>
```

### Gradle

Add the following to your `app/build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.wedge:onboarding-sdk-android:1.1.0")
}
```

## 🔧 Quick Start

### 1. Initialize the SDK

```kotlin
import com.wedge.wedgesdk.sdk.OnboardingSDK
import com.wedge.wedgesdk.sdk.OnboardingCallback

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Your existing code...
    }
}
```

### 2. Start the Onboarding Flow

```kotlin
OnboardingSDK.startOnboarding(
    activity = this,
    token = "your-token-here",
    env = "sandbox", // "integration", "sandbox", or "production"
    type = "onboarding", // "onboarding" or "funding"
    callback = object : OnboardingCallback {
        override fun onSuccess(data: String) {
            Log.d("Onboarding", "Success: $data")
        }
        override fun onClose(reason: String) {
            Log.d("Onboarding", "Closed: $reason")
        }
        override fun onError(error: String) {
            Log.e("Onboarding", "Error: $error")
        }
        override fun onEvent(event: String) {
            Log.d("Onboarding", "Event: $event")
        }
        override fun onLoad(data: String) {
            Log.d("Onboarding", "Loaded: $data")
        }
    }
)
```

## 🎯 Flow Types

### Onboarding Flow (`type = "onboarding"`)
- **Purpose**: Complete onboarding flow for new users
- **Use Case**: First-time user registration and verification
- **Default**: This is the default flow if no type is specified

### Funding Flow (`type = "funding"`)
- **Purpose**: Streamlined flow for existing users making changes to linked funding accounts
- **Use Case**: Users updating payment methods or funding sources
- **Benefits**: Faster, more focused experience for existing users

## 🌍 Environments

| Environment | Use Case |
|-------------|----------|
| `sandbox` | Development and testing |
| `production` | Live production use |
| `integration` | Integration testing |

## 📋 API Reference

### OnboardingSDK

#### `startOnboarding()`

```kotlin
fun startOnboarding(
    activity: FragmentActivity,
    token: String,
    env: String,
    type: String = "onboarding",
    callback: OnboardingCallback
)
```

**Parameters:**
- `activity`: The FragmentActivity to start the onboarding from
- `token`: Your onboarding token
- `env`: Target environment ("integration", "sandbox", "production")
- `type`: Flow type ("onboarding" or "funding") - defaults to "onboarding"
- `callback`: Interface for handling onboarding events

### OnboardingCallback

```kotlin
interface OnboardingCallback {
    fun onSuccess(data: String)      // Called on successful completion
    fun onClose(reason: String)      // Called when user closes/exits the flow
    fun onError(error: String)       // Called when an error occurs
    fun onEvent(event: String) { }   // Optional events from web app
    fun onLoad(data: String) { }     // Optional load event
}
```

## 🔄 Backward Compatibility

The SDK is **100% backward compatible**. Existing implementations will continue to work unchanged:

- If no `type` parameter is provided, it defaults to `"onboarding"`
- All existing method signatures remain the same
- No breaking changes to current implementations

## 📱 Example App

The included example app demonstrates:

- Type selection (onboarding vs funding)
- Dynamic UI based on selected flow type
- Proper error handling and user feedback
- Environment configuration
- Modern Android UI patterns

## 🛠️ Advanced Usage

### Custom URL Construction

The SDK automatically constructs URLs with the following format:
```
{baseUrl}?onboardingToken={token}&type={type}
```

### JavaScript Bridge

The SDK provides a JavaScript bridge (`WedgeSDKAndroid`) for communication between the WebView and native Android code.

## 🔒 Security

- API keys are passed securely via Intent extras
- WebView security settings are properly configured
- JavaScript interface is restricted to necessary functions only

## 📋 Requirements

- **Minimum SDK**: API 21 (Android 5.0)
- **Target SDK**: API 34 (Android 14)
- **Kotlin**: 1.8+
- **AndroidX**: Required

## 🐛 Troubleshooting

### Common Issues

1. **"Missing API key" error**
   - Ensure you're passing a valid API key to `startOnboarding()`

2. **WebView not loading**
   - Check your internet connection
   - Verify the environment URL is correct
   - Ensure the API key is valid for the selected environment

3. **Callback not working**
   - Make sure you're implementing all required methods in `OnboardingCallback`
   - Verify the activity is a `FragmentActivity`

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🤝 Support

For support and questions:
- Create an issue in this repository
- Contact the Wedge team
- Check the [CHANGELOG](CHANGELOG.md) for updates

## 🔄 Migration Guide

### From Previous Versions

If you're upgrading from a previous version:

1. **No code changes required** - the SDK is fully backward compatible
2. **Optional**: Add the `type` parameter to customize the flow
3. **Optional**: Update your UI to reflect the selected flow type

### Example Migration

**Before (still works):**
```kotlin
OnboardingSDK.startOnboarding(
    activity = this,
    apiKey = apiKey,
    environment = environment,
    callback = callback
)
```

**After (with new features):**
```kotlin
OnboardingSDK.startOnboarding(
    activity = this,
    apiKey = apiKey,
    environment = environment,
    type = "funding", // New parameter
    callback = callback
)
```
