# Prompt Notes for Android

A native Android application shell for the supplied Prompt Notes experience. The original single-file HTML interface is packaged locally in the app; no hosted website is required.

## Compatibility
- Minimum Android: 7.0 (API 24)
- Application ID: `com.promptnotes.app`
- Build stack: Java, Android WebView, Android Gradle Plugin 8.6.1, Gradle 8.7, JDK 17

## Included
- Local notes and prompt storage using WebView DOM storage
- Search, folders, tags, pins, trash, protected notes, and themes
- Native Android share sheet bridge
- Microphone permission handling for speech recognition when supported by the installed WebView

## AI integration
The supplied HTML calls `claude.use('sample')`, an API available only inside the Claude artifact runtime. A standalone APK does not inherit that runtime. Notes and local features work, but Axiom chat and AI rewrite/run/title features remain unavailable until a secure AI service endpoint and credentials are configured. No API key is embedded in this APK.

## Build
The GitHub Actions workflow builds the debug APK in the cloud and publishes it as a GitHub Release asset. The build uses JDK 17 and Android SDK Platform 35.
