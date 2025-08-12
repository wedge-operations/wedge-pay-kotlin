# Publishing Guide

This SDK is configured to be published automatically using JitPack.

## How It Works

1. **Push to GitHub**: When you push code to your GitHub repository, JitPack automatically detects it
2. **Automatic Build**: JitPack builds your SDK automatically
3. **Automatic Publishing**: The SDK becomes available for installation immediately

## Publishing Steps

### 1. Push to GitHub

```bash
git add .
git commit -m "Release version 1.0.0"
git push origin main
```

### 2. Create a Release (Optional but Recommended)

1. Go to your GitHub repository
2. Click "Releases" → "Create a new release"
3. Tag version: `v1.0.0`
4. Release title: `Version 1.0.0`
5. Description: Add release notes
6. Click "Publish release"

### 3. Installation for Users

Users can now install your SDK using:

```kotlin
// In settings.gradle.kts
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}

// In app/build.gradle.kts
dependencies {
    implementation("com.github.wedge-operations:wedge-sdk:1.0.0")
}
```

## Version Options

Users can install different versions:

- **Release tag**: `com.github.wedge-operations:wedge-sdk:1.0.0`
- **Commit hash**: `com.github.wedge-operations:wedge-sdk:abc1234`
- **Branch name**: `com.github.wedge-operations:wedge-sdk:main-SNAPSHOT`

## Benefits of JitPack

- ✅ **No setup required** - Works out of the box
- ✅ **Automatic builds** - No manual publishing needed
- ✅ **Multiple version formats** - Tags, commits, branches
- ✅ **Free** - No cost for public repositories
- ✅ **Simple** - Just push to GitHub

## Troubleshooting

If JitPack build fails:

1. Check the [JitPack build logs](https://jitpack.io/com/github/wedge-operations/wedge-sdk)
2. Ensure your `jitpack.yml` is correct
3. Make sure all dependencies are available
4. Check that the Gradle build works locally

## JitPack Status

You can check the build status at: `https://jitpack.io/com/github/wedge-operations/wedge-sdk`