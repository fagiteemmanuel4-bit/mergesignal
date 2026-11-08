# Prompt Notes — Android + Live Web App

Prompt Notes is packaged as an Android WebView app with an offline HTML fallback and a hosted live interface.

## Android
- Minimum Android: 7.0 (API 24)
- Application ID: `com.promptnotes.app`
- Java + Android WebView, JDK 17, Android Gradle Plugin 8.6.1
- Local notes remain in the app's existing WebView localStorage origin.
- When online, the WebView transparently serves the latest hosted interface at `https://fagiteemmanuel4-bit.github.io/mergesignal/` while keeping the local app origin, so existing local notes are preserved.
- If offline, it uses the last cached live page or the bundled fallback.
- The app checks for new interface versions every five minutes. If a note is open, it waits until the editor is closed before refreshing.

## Live web app
- Hosted by GitHub Pages from `web/`.
- Every push to the `main` branch publishes the live interface with GitHub Actions before the APK build.
- Bootstrap Icons are used for the interface icon system.
- PWA manifest and service worker are included for browser installation and update caching.

## Important
Updates to HTML/CSS/JavaScript can go live to all installed apps without reinstalling. Changes to native Android code, permissions, app signing, or Android platform integration still require a new APK. AI calls in the original HTML still depend on the Claude artifact runtime; they need a secure standalone AI backend before they can work independently.
