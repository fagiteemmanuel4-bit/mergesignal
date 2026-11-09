# Prompt Notes (Axiom 1.6) – standalone build

Note: this app keeps its own data on the phone. It does not share notes with the Claude artifact version; use Settings > Export (Markdown) to move text between them.

A single-file mobile notes app (`index.html`, no build step, no frameworks) that you can wrap as an Android APK/AAB.

## Quick start with GitHub (no Android Studio needed)
1. Put all files in this folder at the root of a GitHub repository (including the hidden `.github` folder).
2. Push to `main`. The "Build Android APK" workflow builds the app and publishes a Release containing `PromptNotes.apk`.
3. Download link (public repos): `https://github.com/OWNER/REPO/releases/latest/download/PromptNotes.apk`. Open it on an Android phone and allow installs from your browser.
4. To hand the project to ChatGPT, send it the zip and the text in `CHATGPT_PROMPT.md`.

## Files
| File | Purpose |
|---|---|
| `index.html` | The whole app: UI, logic, styles, AI adapter |
| `manifest.webmanifest`, `sw.js`, `icon*.png/svg` | PWA metadata + offline cache |
| `capacitor.config.json`, `package.json` | Capacitor project (Android wrapper) |

## Build the APK (Capacitor – recommended)
Needs Node 18+, Android Studio, JDK 17.
```bash
npm install
npm run android:add      # once: creates the android/ folder
npm run android:sync     # after every change to index.html
npm run android:open     # opens Android Studio
```
In Android Studio: Build > Build Bundle(s)/APK(s) > Build APK(s) to install on your phone, or Generate Signed Bundle (AAB) for Google Play.

### Quicker option (PWABuilder)
Host the folder over HTTPS (GitHub Pages, Netlify, Vercel), enter the URL at pwabuilder.com, choose Android, download the package.

## What works out of the box
Notes, folders, tags, colors, pin, trash (30 days), PIN lock, search, list/grid, swipe to pin/delete, select mode, settings (theme, accent, text size, haptics, system prompt), streak, offline banner, Markdown export, chat history per note. All data is stored in the WebView's localStorage on the device.

## What you must finish before publishing (hand this list to your developer or to ChatGPT)
1. **AI backend (required).** The standalone build calls an OpenAI-compatible endpoint directly from the device, using a key the user types in Settings > AI connection. For the Play Store, do NOT ship your own key in the app. Build a small backend (Cloud Run, Cloudflare Worker, Supabase function) that holds the key, does auth and rate-limits, and point `S.api` at it. The adapter is the `aiCall` function near the bottom of `index.html`; everything else calls `sample(messages, {signal,onText,modelTier})` and expects `{text}`.
2. **Voice input.** Android WebView does not support the browser SpeechRecognition API. Install `@capacitor-community/speech-recognition` and replace the `startRec/stopRec` functions (search for `SpeechRecognition` in `index.html`).
3. **Haptics, share, file save.** `navigator.vibrate`, `navigator.share` and Blob downloads are used with fallbacks. For reliable native behaviour swap in `@capacitor/haptics`, `@capacitor/share` and `@capacitor/filesystem`.
4. **PIN lock is not encryption.** Note lock only hides notes in the UI and the PIN is a SHA-256 hash in localStorage. For real protection use a native biometric plugin (for example `@capgo/capacitor-native-biometric`) and encrypt note bodies at rest.
5. **Reasoning depth setting** (Fast/Balanced/Deep) only affects Claude's `modelTier` and is ignored by the standalone adapter. Map it to your model's reasoning parameter.
6. **Web search** is not built. The assistant is told it cannot browse. Add it in the backend (search API tool-calling) if wanted.
7. **Play Store checklist:** unique `appId` in `capacitor.config.json`, app icon/adaptive icon (Android Studio > Image Asset), signed AAB, privacy policy URL (you send user text to an AI provider), Data safety form, target the current required Android API level.

## Architecture notes for contributors
- State: `notes[]`, `folders[]`, `trash[]`, settings object `S`; persisted with `sv()/ld()` to localStorage keys `pn_notes`, `pn_folders`, `pn_trash`, `pn_set`.
- Views are `<section class="view">` elements toggled with `show(id)`: `home`, `editor`, `settings`. Sheets are `.sheet` overlays.
- Note fields: `id, title, body, t(updated), f(folder), p(pinned), l(locked), c(color), g(tags[]), h(chat history[])`.
- Chat: `openChat/send/paint/md`. The assistant's reasoning is requested inside `<reasoning>` tags and rendered as a collapsible block.
- Haptics go through `buzz()`, so a Capacitor Haptics swap is one function.
