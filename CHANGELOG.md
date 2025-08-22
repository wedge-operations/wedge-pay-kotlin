# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.1.0] - 2024-12-19

### 🚀 Added
- **Type Parameter Support**: New `type` parameter in `OnboardingSDK.startOnboarding()`
  - `"onboarding"` (default): Complete onboarding flow for new users
  - `"funding"`: Streamlined flow for existing users making changes to linked funding accounts
- **Dynamic URL Construction**: URLs now include `&type={type}` parameter
- **Enhanced Demo App**: Type selection UI with dynamic labels and button text
- **Comprehensive Documentation**: Updated READMEs with new functionality and examples

### ✨ Enhanced
- **User Experience**: Context-aware UI based on selected flow type
- **API Flexibility**: More granular control over onboarding flows
- **Developer Experience**: Better documentation and examples

### 🔄 Backward Compatibility
- **100% Compatible**: Existing code continues to work unchanged
- **Default Behavior**: `type` parameter defaults to `"onboarding"` if not specified
- **No Breaking Changes**: All existing method signatures remain the same

### 📱 Demo App Improvements
- Added type picker (onboarding vs funding)
- Dynamic token field labels based on selected type
- Updated button text and descriptions
- Enhanced user experience with context-aware UI

### 📚 Documentation Updates
- Updated main README with type parameter functionality
- Enhanced SDK README with comprehensive API reference
- Created migration guide for existing implementations
- Added troubleshooting and advanced usage sections

## [1.0.0] - 2024-12-01

### 🎉 Initial Release
- **Core SDK**: Basic onboarding functionality
- **WebView Integration**: Built-in WebView handling with JavaScript bridge
- **Environment Support**: Sandbox, production, and integration environments
- **Callback System**: Success, exit, and error handling
- **Demo App**: Working example of SDK integration
- **Modern Android UI**: Edge-to-edge support and proper system bar handling

### 🔧 Technical Features
- **Minimum SDK**: API 21 (Android 5.0)
- **Target SDK**: API 34 (Android 14)
- **Kotlin**: 1.8+ support
- **AndroidX**: Required dependencies
- **ProGuard Support**: Proper obfuscation rules

### 📋 Requirements
- Android API level 21+
- Kotlin 1.8+
- AndroidX
- Internet permission

---

## Migration Guide

### From v1.0.0 to v1.1.0

**No migration required!** The SDK is 100% backward compatible.

**Optional Enhancements:**
1. Add the `type` parameter to customize the flow:
   ```kotlin
   // Before (still works)
   OnboardingSDK.startOnboarding(
       activity = this,
       apiKey = apiKey,
       environment = environment,
       callback = callback
   )
   
   // After (with new features)
   OnboardingSDK.startOnboarding(
       activity = this,
       apiKey = apiKey,
       environment = environment,
       type = "funding", // New parameter
       callback = callback
   )
   ```

2. Update your UI to reflect the selected flow type (optional)

3. Take advantage of the new funding flow for existing users

## Support

For questions about migration or new features:
- Check the [SDK README](wedge-sdk/README.md)
- Create an issue in this repository
- Contact the Wedge team 