# Onboarding SDK Demo App

This repository contains both the **Wedge Onboarding SDK for Android** and a comprehensive **demo application** showcasing its capabilities.

## 🚀 What's New - Type Parameter Support

The SDK now supports different onboarding flow types, allowing developers to specify the type of user experience:

- **"onboarding"** (default): Complete onboarding flow for new users
- **"funding"**: Streamlined flow for existing users making changes to linked funding accounts

### ✨ Key Features

- **Dynamic Flow Selection**: Choose between onboarding and funding flows
- **Enhanced User Experience**: Context-aware UI and messaging
- **100% Backward Compatible**: Existing code continues to work unchanged
- **Flexible Integration**: Easy to implement and customize

## 📱 SDK Features

- **Multiple Flow Types**: Support for onboarding and funding flows
- **Environment Support**: Sandbox, production, and integration environments
- **Callback System**: Comprehensive event handling
- **WebView Integration**: Built-in WebView with JavaScript bridge
- **Modern Android UI**: Edge-to-edge support and proper system bar handling

## 🏗️ Project Structure

```
OnboardingSDKDemoApp/
├── app/                          # Demo application
│   ├── src/main/java/...        # Demo app source code
│   └── build.gradle.kts         # Demo app build configuration
├── wedge-sdk/                   # Android SDK library
│   ├── src/main/java/...        # SDK source code
│   ├── build.gradle.kts         # SDK build configuration
│   └── README.md                # SDK documentation
└── README.md                    # This file
```

## 🔧 Quick Start

### 1. Clone the Repository

```bash
git clone https://github.com/wedge/onboarding-sdk-android.git
cd onboarding-sdk-android
```

### 2. Open in Android Studio

Open the project in Android Studio and sync Gradle dependencies.

### 3. Run the Demo App

1. Select your target device or emulator
2. Click the "Run" button
3. The demo app will launch with the new type selection feature

## 📚 Documentation

- **[SDK Documentation](wedge-sdk/README.md)**: Comprehensive SDK guide with examples
- **[Demo App Code](app/src/main/java/)**: Working examples of SDK integration
- **[API Reference](wedge-sdk/README.md#api-reference)**: Complete API documentation

## 🎯 Usage Examples

### Basic Onboarding Flow

```kotlin
OnboardingSDK.startOnboarding(
    activity = this,
    token = "your-token",
    env = "sandbox",
    type = "onboarding", // Default flow for new users
    callback = object : OnboardingCallback {
        override fun onSuccess(data: String) { }
        override fun onClose(reason: String) { }
        override fun onError(error: String) { }
        override fun onEvent(event: String) { }
        override fun onLoad(data: String) { }
    }
)
```

### Funding Flow for Existing Users

```kotlin
OnboardingSDK.startOnboarding(
    activity = this,
    token = "your-token",
    env = "sandbox",
    type = "funding", // Streamlined flow for existing users
    callback = callback
)
```

## 🔄 Backward Compatibility

The SDK is **100% backward compatible**. Existing implementations will continue to work unchanged:

- If no `type` parameter is provided, it defaults to `"onboarding"`
- All existing method signatures remain the same
- No breaking changes to current implementations

## 🌍 Environment Support

| Environment | URL | Use Case |
|-------------|-----|----------|
| `sandbox` | `https://onboarding-sandbox.wedge-can.com` | Development and testing |
| `production` | `https://onboarding-production.wedge-can.com` | Live production use |
| `integration` | `https://onboarding-integration.wedge-can.com` | Integration testing |

## 🛠️ Development

### Building the SDK

```bash
./gradlew :wedge-sdk:assembleRelease
```

### Building the Demo App

```bash
./gradlew :app:assembleDebug
```

### Running Tests

```bash
./gradlew test
```

## 📋 Requirements

- **Android Studio**: Arctic Fox or later
- **Minimum SDK**: API 21 (Android 5.0)
- **Target SDK**: API 34 (Android 14)
- **Kotlin**: 1.8+
- **Gradle**: 7.0+

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🆘 Support

- **Documentation**: Check the [SDK README](wedge-sdk/README.md)
- **Issues**: Create an issue in this repository
- **Questions**: Contact the Wedge team

## 🔄 Changelog

See [CHANGELOG.md](CHANGELOG.md) for a complete list of changes and updates.