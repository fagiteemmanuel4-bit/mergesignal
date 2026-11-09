# Task for ChatGPT / developer

This repo is "Prompt Notes" (AI name shown in the app: Axiom 1.6): a single-file mobile notes app (`index.html`, vanilla JS, no framework) wrapped with Capacitor for Android. The GitHub Action in `.github/workflows/android.yml` already builds a debug APK on every push (download it from the run's Artifacts).

Please do the following, keeping the single-file UI unless a refactor is clearly better:

1. **AI backend.** The app currently calls an OpenAI-compatible endpoint using a key typed in Settings > AI connection. For release, remove the key field, add a small backend (Cloudflare Worker, Cloud Run or Supabase function) that holds the key, checks auth, rate-limits, and streams replies. Change the `aiCall` function in `index.html` to call it. Never put a secret key in the app.
2. **Voice input.** Android WebView has no SpeechRecognition. Add `@capacitor-community/speech-recognition` and replace `startRec/stopRec/finishRec`.
3. **Native bridges.** Swap `buzz()` to `@capacitor/haptics`, share to `@capacitor/share`, and Markdown export to `@capacitor/filesystem`.
4. **Security.** The note PIN only hides notes. Add biometric unlock and encrypt note bodies at rest.
5. **Reasoning depth** setting (Fast/Balanced/Deep) is ignored by the adapter; map it to the model's reasoning parameter.
6. **Optional:** web search through the backend (tool calling).
7. **Release.** Set a unique `appId` in `capacitor.config.json`, generate icons (`@capacitor/assets` from `icon.svg`), add a release signing config and build an AAB, write a privacy policy (user text goes to an AI provider), and fill in the Play Console Data safety form.

Data model and architecture notes are at the bottom of `README.md`.
