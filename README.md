# Prompt Notes — Native Android

**Prompt Notes is a product of Cryona.** Version 2.0.0 is a native Android rebuild. The application UI and core features are implemented with Kotlin and Jetpack Compose; it does not use a WebView, HTML, CSS, or JavaScript runtime.

## Native technology
- Kotlin 2.0.21 and Android Gradle Plugin 8.6.1
- Jetpack Compose + Material 3
- AndroidX Activity, Lifecycle and SplashScreen
- Room database for on-device notes
- WorkManager for scheduled note reminders
- Android notification and vibration APIs
- Android Sharesheet for exporting notes
- JDK 17, compile/target SDK 35, minimum SDK 24

## Current app capabilities
- Native splash and first-run onboarding
- Notes home, search, create/edit, pin, duplicate and delete
- Local Room persistence
- Light and dark appearance
- Native notification permission flow
- Scheduled reminders using WorkManager
- Native text export/share and Markdown backup import (file or clipboard)
- AI Studio interface with an explicit not-connected state
- Cryona product branding

## AI connection
The AI screen is intentionally not a fake/demo responder. It displays an explicit pending state until a secure backend is configured. The OpenRouter key must be stored as a server-side secret in a Vercel Function or Supabase Edge Function; it must never be embedded in the APK or committed to this repository.

## Build and release
The GitHub Actions workflow runs unit tests and builds/verifies a debug APK. A signed production APK is built and published only when all four repository Actions secrets are configured:
- ANDROID_KEYSTORE_BASE64 — base64-encoded persistent Android keystore
- ANDROID_KEYSTORE_PASSWORD
- ANDROID_KEY_ALIAS
- ANDROID_KEY_PASSWORD

Use the same keystore for every future release so Android can install updates over existing installations. Never rotate or regenerate this key for routine updates.

## Existing app data and migration
The old 1.x app stored notes in WebView local storage. The native Room database is a separate storage system and cannot silently read that old browser store. Before replacing an older installation, export its notes from the old app and keep the backup. The native Settings screen can import the exact Markdown format produced by the old app, either from a file or directly from the clipboard. Do not uninstall the old app until the backup is safely stored.

## Production status
The native foundation and initial screens are being implemented on a dedicated branch. A green CI build is required before merge. Physical-device checks are still required for notifications, vibration, keyboard/IME behavior, Android 13+ permission prompts, and upgrade/data migration. The signed release remains gated on the persistent keystore secrets above.
