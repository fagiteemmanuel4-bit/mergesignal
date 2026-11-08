# Prompt Notes — Android + Live Interface

Prompt Notes is packaged as an Android WebView app with a bundled offline fallback and a remotely refreshed interface.

## Android
- Minimum Android: 7.0 (API 24)
- Application ID: `com.promptnotes.app`
- Java + Android WebView, JDK 17, Android Gradle Plugin 8.6.1
- Existing note data stays in the same `file:///android_asset/index.html` WebView origin across app versions.
- When online, the Android shell fetches the current UI from the repository's raw GitHub source and serves it under the same local origin. This preserves localStorage and avoids a data migration.
- The live HTML is checked on app launch/resume and every five minutes. Updates apply immediately when idle, or after the current note editor is closed.
- When offline, the app uses the last cached live interface or the bundled fallback.

## Live interface and icons
- Edit `web/index.html` for UI changes.
- Bootstrap Icons 1.13.1 provides the icon set.
- Every push to `main` triggers a GitHub Actions build. The workflow copies `web/index.html` into the packaged offline fallback before compiling the APK.

## Automatic update limits
HTML, CSS, JavaScript, text, and UI changes can reach installed apps without reinstalling. Changes to Java/native Android code, permissions, app signing, or Android platform integration still require a new APK. Users need an internet connection for live UI updates; offline use remains on the last cached/bundled version.

## AI integration
The original HTML uses `claude.use('sample')`, which belongs to the Claude artifact runtime and is not available in a standalone Android app. AI chat, rewriting, and run features still require a secure standalone AI backend. No provider secret is embedded in the APK.
