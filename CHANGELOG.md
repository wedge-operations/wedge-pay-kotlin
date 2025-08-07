# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2024-12-19

### Added
- Initial release of Wedge Onboarding SDK
- WebView-based onboarding integration
- Full-page experience (no bottom sheet)
- Support for sandbox and production environments
- Simple callback-based API
- API key-based authentication
- Comprehensive documentation

### Features
- `OnboardingSDK.startOnboarding()` - Main entry point for starting onboarding
- `OnboardingCallback` interface with success, exit, and error callbacks
- Automatic environment detection (sandbox/production)
- Secure API key handling
- Full-screen WebView integration 